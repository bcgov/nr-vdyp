package ca.bc.gov.nrs.vdyp.batch.configuration;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchMetricsException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.BatchProjectionService;
import ca.bc.gov.nrs.vdyp.batch.service.BatchResultAggregationService;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

/**
 * Central configuration class for the VDYP batch processing system.
 */
@Configuration
public class BatchConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);

	private final JobRepository jobRepository;
	private final BatchMetricsCollector metricsCollector;
	private final BatchProperties batchProperties;
	private final BatchResultAggregationService resultAggregationService;

	public BatchConfiguration(
			JobRepository jobRepository, BatchMetricsCollector metricsCollector, BatchProperties batchProperties,
			BatchResultAggregationService resultAggregationService
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
	@StepScope
	public BatchRetryPolicy retryPolicy() {
		int maxAttempts = batchProperties.getRetry().getMaxAttempts();
		int backoffPeriod = batchProperties.getRetry().getBackoffPeriod();
		logger.info("Creating BatchRetryPolicy with maxAttempts={}, backoffPeriod={}", maxAttempts, backoffPeriod);
		return new BatchRetryPolicy(maxAttempts, backoffPeriod, metricsCollector);
	}

	@Bean
	@StepScope
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
			BatchProjectionService batchProjectionService, ItemStreamReader<BatchChunkMetadata> partitionReader,
			BatchItemWriter partitionWriter, BatchItemProcessor batchItemProcessor
	) {

		// Spring Batch chunk size must be 1 because each BatchChunkMetadata represents a chunk of records
		int stepChunkSize = 1;
		logger.trace(
				"Worker step configured with Spring Batch chunk size: {} (each metadata item contains {} records)",
				stepChunkSize, batchProperties.getReader().getDefaultChunkSize()
		);

		return new StepBuilder("workerStep", jobRepository)
				.<BatchChunkMetadata, BatchChunkMetadata>chunk(stepChunkSize, transactionManager)
				.reader(partitionReader).processor(batchItemProcessor).writer(partitionWriter).listener(partitionWriter)
				.listener(retryPolicy).listener(skipPolicy).faultTolerant().retryPolicy(retryPolicy)
				.skipPolicy(skipPolicy).listener(new StepExecutionListener() {
					@Override
					@SuppressWarnings("java:S2637") // jobGuid, jobExecutionId, partitionName cannot be null in batch
													// context
					public void beforeStep(@NonNull StepExecution stepExecution) {
						String partitionName = stepExecution.getExecutionContext()
								.getString(BatchConstants.Partition.NAME);

						Long jobExecutionId = stepExecution.getJobExecutionId();
						String jobGuid = stepExecution.getJobExecution().getJobParameters()
								.getString(BatchConstants.Job.GUID);

						try {
							metricsCollector.initializePartitionMetrics(jobExecutionId, jobGuid, partitionName);
						} catch (BatchException e) {
							logger.error("Failed to initialize partition metrics: {}", e.getMessage());
						}

						logger.trace(
								"[{}] [GUID: {}] VDYP Worker step starting for job execution ID: {}", partitionName,
								jobGuid, jobExecutionId
						);
					}

					@Override
					@SuppressWarnings("java:S2637") // jobGuid, jobExecutionId, partitionName cannot be null in batch
													// context
					public ExitStatus afterStep(@NonNull StepExecution stepExecution) {
						String partitionName = stepExecution.getExecutionContext()
								.getString(BatchConstants.Partition.NAME);
						Long jobExecutionId = stepExecution.getJobExecutionId();
						String jobGuid = stepExecution.getJobExecution().getJobParameters()
								.getString(BatchConstants.Job.GUID);

						// Complete partition metrics
						try {
							metricsCollector.completePartitionMetrics(
									jobExecutionId, jobGuid, partitionName, stepExecution.getWriteCount(),
									stepExecution.getExitStatus().getExitCode()
							);
						} catch (BatchMetricsException e) {
							logger.error("Failed to complete partition metrics: {}", e.getMessage());
						}

						logger.trace(
								"[{}] [GUID: {}] VDYP Worker step completed for job execution ID: {}. Read: {}, Written: {}, Skipped: {}",
								partitionName, jobGuid, jobExecutionId, stepExecution.getReadCount(),
								stepExecution.getWriteCount(), stepExecution.getSkipCount()
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
			BatchJobExecutionListener jobExecutionListener, Step masterStep, Step postProcessingStep,
			PlatformTransactionManager transactionManager
	) {
		return new JobBuilder("VdypPartitionedJob", jobRepository) //
				.incrementer(new RunIdIncrementer()) //
				.start(masterStep) //
				.next(postProcessingStep) //
				.listener(new JobExecutionListener() {
					@Override
					@SuppressWarnings("java:S2637") // jobGuid, jobExecutionId, partitionName cannot be null in batch
													// context
					public void beforeJob(@NonNull JobExecution jobExecution) {
						// Initialize job metrics
						String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);
						try {
							metricsCollector.initializeMetrics(jobExecution.getId(), jobGuid);
						} catch (BatchMetricsException e) {
							logger.error("Failed to initialize job metrics: {}", e.getMessage());
						}
						logger.info(
								"[GUID: {}] === VDYP Batch Job Starting === Execution ID: {}", jobGuid,
								jobExecution.getId()
						);
						jobExecutionListener.beforeJob(jobExecution);
					}

					@Override
					@SuppressWarnings("java:S2637") // jobGuid, jobExecutionId, partitionName cannot be null in batch
													// context
					public void afterJob(@NonNull JobExecution jobExecution) {
						String jobGuid = jobExecution.getJobParameters().getString(BatchConstants.Job.GUID);

						// Finalize job metrics - only count worker steps (partitioned steps)
						long totalRead = jobExecution.getStepExecutions().stream()
								.filter(stepExecution -> stepExecution.getStepName().startsWith("workerStep:"))
								.mapToLong(StepExecution::getReadCount).sum();
						long totalWritten = jobExecution.getStepExecutions().stream()
								.filter(stepExecution -> stepExecution.getStepName().startsWith("workerStep:"))
								.mapToLong(StepExecution::getWriteCount).sum();

						logger.debug(
								"[GUID: {}] [VDYP Metrics Debug] Job execution ID: {} - All steps: [{}]", jobGuid,
								jobExecution.getId(),
								jobExecution.getStepExecutions().stream().map(StepExecution::getStepName)
										.collect(Collectors.joining(", "))
						);

						try {
							metricsCollector.finalizeJobMetrics(
									jobExecution.getId(), jobGuid, jobExecution.getStatus().toString(), totalRead,
									totalWritten
							);
						} catch (BatchMetricsException e) {
							logger.error("Failed to finalize job metrics: {}", e.getMessage());
						}

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
											"[GUID: {}] Job execution ID: {} was stopped. Interim partition directories cleanup completed",
											jobGuid, jobExecution.getId()
									);
								}
							} catch (Exception e) {
								logger.error(
										"[GUID: {}] Failed to cleanup interim directories for stopped job execution ID: {}: {}",
										jobGuid, jobExecution.getId(), e.getMessage()
								);
							}
						}

						try {
							metricsCollector.cleanupOldMetrics(20);
						} catch (BatchMetricsException e) {
							logger.error("Failed to cleanup old metrics: {}", e.getMessage());
						}

						logger.info(
								"[GUID: {}] === VDYP Batch Job Completed === Execution ID: {}", jobGuid,
								jobExecution.getId()
						);
					}
				}).build();
	}

	@Bean
	@StepScope
	public ItemStreamReader<BatchChunkMetadata> partitionReader(
			@Value("#{stepExecutionContext['partitionName']}") String partitionName,
			@Value("#{stepExecution.jobExecutionId}") Long jobExecutionId,
			@Value("#{jobParameters['" + BatchConstants.Job.GUID + "']}") String jobGuid,
			BatchProperties batchProperties
	) {
		logger.trace(
				"[GUID: {}, Execution ID: {}, Partition: {}] Using BatchItemReader with chunk size: {}", jobGuid,
				jobExecutionId, partitionName, batchProperties.getReader().getDefaultChunkSize()
		);
		return new BatchItemReader(
				partitionName, jobExecutionId, jobGuid, batchProperties.getReader().getDefaultChunkSize()
		);
	}

	@Bean
	@StepScope
	public BatchItemProcessor batchItemProcessor(BatchMetricsCollector metricsCollector) {
		return new BatchItemProcessor(metricsCollector);
	}

	@Bean
	@StepScope
	public BatchItemWriter partitionWriter(BatchProjectionService batchProjectionService, ObjectMapper objectMapper) {
		return new BatchItemWriter(batchProjectionService, objectMapper);
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
			var stepExecution = chunkContext.getStepContext().getStepExecution();
			Long jobExecutionId = stepExecution.getJobExecutionId();
			String jobGuid = stepExecution.getJobExecution().getJobParameters().getString(BatchConstants.Job.GUID);
			logger.info("[GUID: {}] Starting result aggregation for job execution: {}", jobGuid, jobExecutionId);

			// Get job parameters for aggregation
			JobExecution jobExecution = stepExecution.getJobExecution();
			String jobTimestamp = jobExecution.getJobParameters().getString(BatchConstants.Job.TIMESTAMP);
			String jobBaseDir = jobExecution.getJobParameters().getString(BatchConstants.Job.BASE_DIR);

			// Execute aggregation
			Path consolidatedZip = resultAggregationService
					.aggregateResultsFromJobDir(jobExecutionId, jobGuid, jobBaseDir, jobTimestamp);

			stepExecution.getExecutionContext().putString("consolidatedOutputPath", consolidatedZip.toString());

			logger.info(
					"[GUID: {}] Result aggregation completed successfully for job execution: {}. Consolidated output: {}",
					jobGuid, jobExecutionId, consolidatedZip
			);

			// Clean up interim partition directories after successful zip creation and validation
			if (batchProperties.getPartition().getInterimDirsCleanupEnabled()) {
				if (resultAggregationService.validateConsolidatedZip(consolidatedZip)) {

					Path jobBasePath = Paths.get(jobBaseDir);
					resultAggregationService.cleanupPartitionDirectories(jobBasePath);

					logger.info(
							"[GUID: {}] Interim partition directories cleanup completed for job: {}", jobGuid,
							jobExecutionId
					);
				} else {
					logger.warn(
							"[GUID: {}] Consolidated ZIP file validation failed for job: {}. Skipping cleanup to preserve interim files for debugging.",
							jobGuid, jobExecutionId
					);
				}
			} else {
				logger.info(
						"[GUID: {}] Cleanup is disabled. Skipping cleanup of interim partition directories for job: {}",
						jobGuid, jobExecutionId
				);
			}

			return RepeatStatus.FINISHED;
		};
	}
}
