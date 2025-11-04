package ca.bc.gov.nrs.vdyp.batch.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import ca.bc.gov.nrs.vdyp.batch.exception.ResultAggregationException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.ResultAggregationService;
import ca.bc.gov.nrs.vdyp.batch.service.VdypProjectionService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

@Configuration
public class PartitionedBatchConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(PartitionedBatchConfiguration.class);

	private final JobRepository jobRepository;
	private final BatchMetricsCollector metricsCollector;
	private final BatchProperties batchProperties;
	private final ResultAggregationService resultAggregationService;

	public PartitionedBatchConfiguration(
			JobRepository jobRepository, BatchMetricsCollector metricsCollector, BatchProperties batchProperties,
			ResultAggregationService resultAggregationService
	) {
		this.jobRepository = jobRepository;
		this.metricsCollector = metricsCollector;
		this.batchProperties = batchProperties;
		this.resultAggregationService = resultAggregationService;
	}

	@Bean(name = "asyncJobLauncher")
	public JobLauncher asyncJobLauncher(TaskExecutor taskExecutor) throws Exception {
		TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.setTaskExecutor(taskExecutor);
		jobLauncher.afterPropertiesSet();
		logger.info("Asynchronous JobLauncher created successfully");
		return jobLauncher;
	}

	@Bean
	public BatchRetryPolicy retryPolicy() {
		int maxAttempts = batchProperties.getRetry().getMaxAttempts();
		int backoffPeriod = batchProperties.getRetry().getBackoffPeriod();
		logger.info("Creating BatchRetryPolicy with maxAttempts={}, backoffPeriod={}", maxAttempts, backoffPeriod);
		BatchRetryPolicy policy = new BatchRetryPolicy(maxAttempts, backoffPeriod);
		policy.setMetricsCollector(metricsCollector);
		return policy;
	}

	@Bean
	public BatchSkipPolicy skipPolicy() {
		int maxSkipCount = batchProperties.getSkip().getMaxCount();
		logger.info("Creating BatchSkipPolicy with maxSkipCount={}", maxSkipCount);
		return new BatchSkipPolicy(maxSkipCount, metricsCollector);
	}

	/**
	 * Task executor for parallel processing
	 */
	@Bean
	public TaskExecutor taskExecutor() {
		int corePoolSize = batchProperties.getThreadPool().getCorePoolSize();
		int maxPoolSizeMultiplier = batchProperties.getThreadPool().getMaxPoolSizeMultiplier();
		String threadNamePrefix = batchProperties.getThreadPool().getThreadNamePrefix();

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(corePoolSize * maxPoolSizeMultiplier);
		executor.setQueueCapacity(corePoolSize);
		executor.setThreadNamePrefix(threadNamePrefix);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}

	@Bean
	public DynamicPartitioner dynamicPartitioner() {
		return new DynamicPartitioner();
	}

	@Bean
	public DynamicPartitionHandler dynamicPartitionHandler(
			TaskExecutor taskExecutor, Step workerStep, DynamicPartitioner dynamicPartitioner,
			BatchProperties batchProperties
	) {
		return new DynamicPartitionHandler(taskExecutor, workerStep, dynamicPartitioner, batchProperties);
	}

	@Bean
	public Step masterStep(
			TaskExecutor taskExecutor, Step workerStep, DynamicPartitioner dynamicPartitioner,
			DynamicPartitionHandler dynamicPartitionHandler
	) {
		return new StepBuilder("masterStep", jobRepository).partitioner("workerStep", dynamicPartitioner)
				.partitionHandler(dynamicPartitionHandler).build();
	}

	/**
	 * Worker step with metrics collection
	 */
	@Bean
	public Step workerStep(
			BatchRetryPolicy retryPolicy, BatchSkipPolicy skipPolicy, PlatformTransactionManager transactionManager,
			BatchMetricsCollector metricsCollector, BatchProperties batchProperties,
			VdypProjectionService vdypProjectionService, ItemStreamReader<BatchRecord> partitionReader,
			VdypChunkProjectionWriter partitionWriter
	) {

		int chunkSize = Math.max(batchProperties.getReader().getDefaultChunkSize(), 1);
		logger.info("Worker step configured with chunk size: {}", chunkSize);

		return new StepBuilder("workerStep", jobRepository)
				.<BatchRecord, BatchRecord>chunk(chunkSize, transactionManager).reader(partitionReader)
				.processor(vdypProjectionProcessor(metricsCollector)).writer(partitionWriter).listener(partitionWriter)
				.listener(retryPolicy).listener(skipPolicy).faultTolerant().retryPolicy(retryPolicy)
				.skipPolicy(skipPolicy).listener(new StepExecutionListener() {
					@Override
					public void beforeStep(@NonNull StepExecution stepExecution) {
						String partitionName = stepExecution.getExecutionContext()
								.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN);

						Long jobExecutionId = stepExecution.getJobExecutionId();

						metricsCollector.initializePartitionMetrics(jobExecutionId, partitionName);

						logger.info("[{}] VDYP Worker step starting", partitionName);
					}

					@Override
					public ExitStatus afterStep(@NonNull StepExecution stepExecution) {
						String partitionName = stepExecution.getExecutionContext()
								.getString(BatchConstants.Partition.NAME, BatchConstants.Common.UNKNOWN);
						Long jobExecutionId = stepExecution.getJobExecutionId();

						// Complete partition metrics
						metricsCollector.completePartitionMetrics(
								jobExecutionId, partitionName, stepExecution.getWriteCount(),
								stepExecution.getExitStatus().getExitCode()
						);

						logger.info(
								"[{}] VDYP Worker step completed. Read: {}, Written: {}, Skipped: {}", partitionName,
								stepExecution.getReadCount(), stepExecution.getWriteCount(),
								stepExecution.getSkipCount()
						);

						return stepExecution.getExitStatus();
					}
				}).build();
	}

	/**
	 * VDYP Batch Job with metrics initialization Only created when explicitly enabled via property
	 */
	@Bean
	@ConditionalOnProperty(name = "batch.job.auto-create", havingValue = "true", matchIfMissing = false)
	public Job partitionedJob(
			PartitionedJobExecutionListener jobExecutionListener, Step masterStep, Step postProcessingStep,
			PlatformTransactionManager transactionManager
	) {
		return new JobBuilder("VdypPartitionedJob", jobRepository).incrementer(new RunIdIncrementer()).start(masterStep)
				.next(postProcessingStep).listener(new JobExecutionListener() {
					@Override
					public void beforeJob(@NonNull JobExecution jobExecution) {
						// Initialize job metrics
						metricsCollector.initializeMetrics(jobExecution.getId());
						jobExecutionListener.beforeJob(jobExecution);
						logger.info("=== VDYP Batch Job Starting ===");
					}

					@Override
					public void afterJob(@NonNull JobExecution jobExecution) {
						// Finalize job metrics - only count worker steps (partitioned steps)
						long totalRead = jobExecution.getStepExecutions().stream()
								.filter(stepExecution -> stepExecution.getStepName().startsWith("workerStep:"))
								.mapToLong(StepExecution::getReadCount).sum();
						long totalWritten = jobExecution.getStepExecutions().stream()
								.filter(stepExecution -> stepExecution.getStepName().startsWith("workerStep:"))
								.mapToLong(StepExecution::getWriteCount).sum();

						logger.debug(
								"[VDYP Metrics Debug] Job {} - All steps: [{}]", jobExecution.getId(),
								jobExecution.getStepExecutions().stream().map(StepExecution::getStepName)
										.collect(Collectors.joining(", "))
						);

						metricsCollector.finalizeJobMetrics(
								jobExecution.getId(), jobExecution.getStatus().toString(), totalRead, totalWritten
						);

						jobExecutionListener.afterJob(jobExecution);

						if (jobExecution.getStatus() == BatchStatus.STOPPED
								&& batchProperties.getPartition().getInterimDirsCleanupEnabled()) {
							try {
								String jobBaseDir = jobExecution.getJobParameters()
										.getString(BatchConstants.Job.BASE_DIR);
								if (jobBaseDir != null) {
									Path jobBasePath = Paths.get(jobBaseDir);
									resultAggregationService.cleanupPartitionDirectories(jobBasePath);
									logger.info(
											"Job {} was stopped. Interim partition directories cleanup completed",
											jobExecution.getId()
									);
								}
							} catch (Exception e) {
								logger.warn(
										"Failed to cleanup interim directories for stopped job {}: {}",
										jobExecution.getId(), e.getMessage()
								);
							}
						}

						metricsCollector.cleanupOldMetrics(20);

						logger.info("=== VDYP Batch Job Completed ===");
					}
				}).build();
	}

	@Bean
	@StepScope
	public ItemStreamReader<BatchRecord> partitionReader(
			BatchMetricsCollector metricsCollector,
			@Value("#{stepExecutionContext['partitionName']}") String partitionName,
			@Value("#{stepExecution.jobExecutionId}") Long jobExecutionId, BatchProperties batchProperties
	) {

		logger.info(
				"[{}] Using ChunkBasedPolygonItemReader with chunk size: {}", partitionName,
				batchProperties.getReader().getDefaultChunkSize()
		);
		return new ChunkBasedPolygonItemReader(
				partitionName, metricsCollector, jobExecutionId, batchProperties.getReader().getDefaultChunkSize()
		);
	}

	@Bean
	@StepScope
	public VdypProjectionProcessor vdypProjectionProcessor(BatchMetricsCollector metricsCollector) {
		return new VdypProjectionProcessor(metricsCollector);
	}

	@Bean
	@StepScope
	public VdypChunkProjectionWriter
			partitionWriter(VdypProjectionService vdypProjectionService, BatchMetricsCollector metricsCollector) {
		return new VdypChunkProjectionWriter(vdypProjectionService, metricsCollector);
	}

	/**
	 * Post-processing step that aggregates results from all partitions into a single consolidated ZIP file. This step
	 * runs after all worker partitions have completed successfully.
	 */
	@Bean
	public Step postProcessingStep(PlatformTransactionManager transactionManager) {
		return new StepBuilder("postProcessingStep", jobRepository)
				.tasklet(resultAggregationTasklet(), transactionManager).build();
	}

	/**
	 * Tasklet that performs result aggregation by collecting all partition results and creating a single consolidated
	 * output ZIP file.
	 */
	@Bean
	@StepScope
	public Tasklet resultAggregationTasklet() {
		return (contribution, chunkContext) -> {
			Long jobExecutionId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();
			logger.info("Starting result aggregation for job execution: {}", jobExecutionId);
			try {
				JobExecution jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();

				String jobTimestamp = jobExecution.getJobParameters().getString(BatchConstants.Job.TIMESTAMP);
				String jobBaseDir = jobExecution.getJobParameters().getString(BatchConstants.Job.BASE_DIR);

				Path consolidatedZip = resultAggregationService
						.aggregateResultsFromJobDir(jobExecutionId, jobBaseDir, jobTimestamp);

				chunkContext.getStepContext().getStepExecution().getExecutionContext()
						.putString("consolidatedOutputPath", consolidatedZip.toString());

				logger.info("Result aggregation completed successfully. Consolidated output: {}", consolidatedZip);

				// Clean up interim partition directories after successful zip creation and validation
				if (batchProperties.getPartition().getInterimDirsCleanupEnabled()) {
					if (resultAggregationService.validateConsolidatedZip(consolidatedZip)) {

						Path jobBasePath = Paths.get(jobBaseDir);
						resultAggregationService.cleanupPartitionDirectories(jobBasePath);

						logger.info("Interim partition directories cleanup completed for job: {}", jobExecutionId);
					} else {
						logger.warn(
								"Consolidated ZIP file validation failed. Skipping cleanup to preserve interim files for debugging."
						);
					}
				} else {
					logger.info(
							"Cleanup is disabled. Skipping cleanup of interim partition directories for job: {}",
							jobExecutionId
					);
				}

				return RepeatStatus.FINISHED;

			} catch (IOException ioException) {
				throw handleResultAggregationFailure(
						jobExecutionId, ioException, "I/O operation failed during result aggregation"
				);
			} catch (Exception generalException) {
				throw handleResultAggregationFailure(
						jobExecutionId, generalException, "Unexpected error during result aggregation"
				);
			}
		};
	}

	private ResultAggregationException
			handleResultAggregationFailure(Long jobExecutionId, Exception cause, String errorDescription) {
		String contextualMessage = String.format(
				"%s for job execution: %d, Exception type: %s, Root cause: %s", errorDescription, jobExecutionId,
				cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : BatchConstants.ErrorMessage.NO_ERROR_MESSAGE
		);

		logger.error(contextualMessage, cause);

		return new ResultAggregationException(contextualMessage, cause);
	}
}
