package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.exception.ResultAggregationException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import ca.bc.gov.nrs.vdyp.batch.service.ResultAggregationService;
import ca.bc.gov.nrs.vdyp.batch.service.VdypProjectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * VDYP Batch Configuration with FEATURE_ID-based partitioning, error handling, and detailed metrics collection.
 *
 * This configuration implements the FEATURE_ID-based partitioning strategy that ensures complete polygon data integrity
 * for VDYP projection processing.
 *
 * Key Components: - DynamicPartitioner: Partitions by unique FEATURE_IDs - PolygonAwareItemReader: Reads complete
 * polygon + all layers as atomic units - VdypProjectionProcessor: Processes complete Polygon objects through
 * extended-core
 */
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

	private static final String UNKNOWN = "unknown";

	@Bean
	@StepScope
	public BatchRetryPolicy retryPolicy(
			@Value("#{jobParameters['maxRetryAttempts']}") Long maxRetryAttemptsParam,
			@Value("#{jobParameters['retryBackoffPeriod']}") Long retryBackoffPeriodParam
	) {

		// Get max attempts
		int maxAttempts;
		if (maxRetryAttemptsParam != null && maxRetryAttemptsParam > 0) {
			maxAttempts = maxRetryAttemptsParam.intValue();
		} else if (batchProperties.getRetry().getMaxAttempts() > 0) {
			maxAttempts = batchProperties.getRetry().getMaxAttempts();
		} else {
			throw new IllegalStateException("No max retry attempts specified in job parameters or properties. ");
		}

		// Get backoff period
		int backoffPeriod;
		if (retryBackoffPeriodParam != null && retryBackoffPeriodParam > 0) {
			backoffPeriod = retryBackoffPeriodParam.intValue();
		} else if (batchProperties.getRetry().getBackoffPeriod() > 0) {
			backoffPeriod = batchProperties.getRetry().getBackoffPeriod();
		} else {
			throw new IllegalStateException("No retry backoff period specified in job parameters or properties. ");
		}

		BatchRetryPolicy policy = new BatchRetryPolicy(maxAttempts, backoffPeriod);
		policy.setMetricsCollector(metricsCollector);
		return policy;
	}

	/**
	 * Batch Skip policy with metrics - step scoped to access job parameters
	 */
	@Bean
	@StepScope
	public BatchSkipPolicy skipPolicy(@Value("#{jobParameters['maxSkipCount']}") Long maxSkipCountParam) {
		// Get max skip count
		int maxSkipCount;
		if (maxSkipCountParam != null && maxSkipCountParam > 0) {
			maxSkipCount = maxSkipCountParam.intValue();
		} else if (batchProperties.getSkip().getMaxCount() > 0) {
			maxSkipCount = batchProperties.getSkip().getMaxCount();
		} else {
			throw new IllegalStateException("No max skip count specified in job parameters or properties. ");
		}

		return new BatchSkipPolicy(maxSkipCount, metricsCollector);
	}

	/**
	 * Task executor for parallel processing
	 */
	@Bean
	public TaskExecutor taskExecutor() {
		int corePoolSize = batchProperties.getThreadPool().getCorePoolSize();
		if (corePoolSize <= 0) {
			throw new IllegalStateException(
					"batch.thread-pool.core-pool-size must be configured with a positive value in application.properties"
			);
		}

		int maxPoolSizeMultiplier = batchProperties.getThreadPool().getMaxPoolSizeMultiplier();
		if (maxPoolSizeMultiplier <= 0) {
			throw new IllegalStateException(
					"batch.thread-pool.max-pool-size-multiplier must be configured with a positive value in application.properties"
			);
		}

		String threadNamePrefix = batchProperties.getThreadPool().getThreadNamePrefix();
		if (threadNamePrefix == null || threadNamePrefix.trim().isEmpty()) {
			throw new IllegalStateException(
					"batch.thread-pool.thread-name-prefix must be configured in application.properties"
			);
		}

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(corePoolSize * maxPoolSizeMultiplier);
		executor.setQueueCapacity(corePoolSize);
		executor.setThreadNamePrefix(threadNamePrefix);
		executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
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
			VdypProjectionService vdypProjectionService
	) {
		// Use fixed chunk size for VDYP processing (one record per chunk for atomic
		// processing)
		int chunkSize = 1;

		return new StepBuilder("workerStep", jobRepository)
				.<BatchRecord, BatchRecord>chunk(chunkSize, transactionManager)
				.reader(partitionReader(metricsCollector, batchProperties, null))
				.processor(vdypProjectionProcessor(retryPolicy, metricsCollector, vdypProjectionService))
				.writer(partitionWriter(null)).faultTolerant().retryPolicy(retryPolicy).skipPolicy(skipPolicy)
				.listener(new StepExecutionListener() {
					@Override
					public void beforeStep(@NonNull StepExecution stepExecution) {
						String partitionName = stepExecution.getExecutionContext().getString("partitionName", UNKNOWN);
						long startLine = stepExecution.getExecutionContext().getLong("startLine", 0);
						long endLine = stepExecution.getExecutionContext().getLong("endLine", 0);
						Long jobExecutionId = stepExecution.getJobExecutionId();

						// Initialize partition metrics using line ranges as ID ranges
						metricsCollector.initializePartitionMetrics(jobExecutionId, partitionName, startLine, endLine);

						logger.info(
								"[{}] VDYP Worker step starting for range {}-{}", partitionName, startLine, endLine
						);
					}

					@Override
					public ExitStatus afterStep(@NonNull StepExecution stepExecution) {
						String partitionName = stepExecution.getExecutionContext().getString("partitionName", UNKNOWN);
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
						metricsCollector.initializeJobMetrics(jobExecution.getId());
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

						// Debug logging for metrics validation
						logger.debug(
								"[VDYP Metrics Debug] Job {} - All steps: [{}]", jobExecution.getId(),
								jobExecution.getStepExecutions().stream().map(StepExecution::getStepName)
										.collect(Collectors.joining(", "))
						);

						metricsCollector.finalizeJobMetrics(
								jobExecution.getId(), jobExecution.getStatus().toString(), totalRead, totalWritten
						);

						jobExecutionListener.afterJob(jobExecution);

						// Clean up old metrics
						metricsCollector.cleanupOldMetrics(20);

						logger.info("=== VDYP Batch Job Completed ===");
					}
				}).build();
	}

	/**
	 * FEATURE_ID-aware reader that assembles complete polygon objects.
	 *
	 * This reader loads both polygon and layer data, groups by FEATURE_ID, and creates complete BatchRecord objects
	 * containing polygon + all layers. Only processes FEATURE_IDs assigned to this partition.
	 */
	@Bean
	@StepScope
	public PolygonAwareItemReader partitionReader(
			BatchMetricsCollector metricsCollector, BatchProperties batchProperties,
			@Value("#{stepExecutionContext['assignedFeatureIds']}") String assignedFeatureIds
	) {

		// Get polygon and layer resources from configuration
		Resource polygonResource = new ClassPathResource(
				batchProperties.getVdyp().getProjection().getPolygonFile().replace("classpath:", "")
		);
		Resource layerResource = new ClassPathResource(
				batchProperties.getVdyp().getProjection().getLayerFile().replace("classpath:", "")
		);

		return new PolygonAwareItemReader(polygonResource, layerResource, metricsCollector);
	}

	@Bean
	@StepScope
	public ItemWriter<BatchRecord>
			partitionWriter(@Value("#{stepExecutionContext['partitionName']}") String partitionName) {

		String actualPartitionName = partitionName != null ? partitionName : UNKNOWN;

		logger.info(
				"[{}] VDYP No-Op Writer configured - results stored via VdypProjectionService", actualPartitionName
		);

		// Return a no-op writer that does nothing
		return chunk -> {
			// No-op: Results are already stored by VdypProjectionService in the processor
		};
	}

	@Bean
	@StepScope
	public VdypProjectionProcessor vdypProjectionProcessor(
			BatchRetryPolicy retryPolicy, BatchMetricsCollector metricsCollector,
			VdypProjectionService vdypProjectionService
	) {
		return new VdypProjectionProcessor(retryPolicy, metricsCollector, vdypProjectionService);
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
			String baseOutputPath = batchProperties.getOutput().getDirectory().getDefaultPath();

			if (baseOutputPath == null) {
				baseOutputPath = System.getProperty("java.io.tmpdir");
				logger.warn("No output directory configured, using system temp directory: {}", baseOutputPath);
			}

			logger.info(
					"Starting result aggregation for job execution: {} from path: {}", jobExecutionId, baseOutputPath
			);

			try {
				// Aggregate all partition results into consolidated ZIP
				Path consolidatedZip = resultAggregationService.aggregateResults(jobExecutionId, baseOutputPath);

				// Store the final ZIP path in the execution context for potential retrieval
				chunkContext.getStepContext().getStepExecution().getExecutionContext()
						.putString("consolidatedOutputPath", consolidatedZip.toString());

				logger.info("Result aggregation completed successfully. Consolidated output: {}", consolidatedZip);

				return RepeatStatus.FINISHED;

			} catch (IOException ioException) {
				// Handle I/O specific failures: perform cleanup and wrap with enhanced context
				throw handleResultAggregationFailure(
						jobExecutionId, baseOutputPath, ioException, "I/O operation failed during result aggregation"
				);
			} catch (Exception generalException) {
				// Handle all other failures: perform cleanup and wrap with enhanced context
				throw handleResultAggregationFailure(
						jobExecutionId, baseOutputPath, generalException, "Unexpected error during result aggregation"
				);
			}
		};
	}

	/**
	 * Handles result aggregation failures by performing cleanup, logging, and creating appropriate exception.
	 *
	 * @param jobExecutionId   The job execution ID for context
	 * @param baseOutputPath   The base output path where the failure occurred
	 * @param cause            The original exception that caused the failure
	 * @param errorDescription A description of the type of error that occurred
	 * @return ResultAggregationException with enhanced context
	 */
	private ResultAggregationException handleResultAggregationFailure(
			Long jobExecutionId, String baseOutputPath, Exception cause, String errorDescription
	) {
		// Perform cleanup of partial aggregation results
		performAggregationCleanup(jobExecutionId, baseOutputPath);

		// Create enhanced contextual message
		String contextualMessage = String.format(
				"%s for job execution: %d, Output path: %s, Exception type: %s, Root cause: %s", errorDescription,
				jobExecutionId, baseOutputPath, cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : "No error message available"
		);

		// Log the failure with full context
		logger.error(contextualMessage, cause);

		// Return dedicated exception with enhanced context
		return new ResultAggregationException(contextualMessage, cause);
	}

	/**
	 * Performs cleanup of partial aggregation results when aggregation fails. This method safely handles cleanup
	 * without throwing exceptions.
	 *
	 * @param jobExecutionId The job execution ID for context
	 * @param baseOutputPath The base output path where cleanup should occur
	 */
	private void performAggregationCleanup(Long jobExecutionId, String baseOutputPath) {
		try {
			// Attempt to clean up any partial files created during aggregation
			java.nio.file.Path outputDir = java.nio.file.Paths.get(baseOutputPath);
			if (Files.exists(outputDir)) {
				// Clean up temporary files related to this job execution
				String jobPrefix = "job_" + jobExecutionId;
				try (java.util.stream.Stream<java.nio.file.Path> pathStream = Files.list(outputDir)) {
					pathStream.filter(path -> path.getFileName().toString().startsWith(jobPrefix)).forEach(path -> {
						try {
							Files.deleteIfExists(path);
							logger.debug("Cleaned up partial aggregation file: {}", path);
						} catch (Exception cleanupException) {
							logger.warn("Failed to cleanup file: {}", path, cleanupException);
						}
					});
				}
			}
		} catch (Exception cleanupException) {
			// Log cleanup failure but don't throw exception to avoid masking original error
			logger.warn(
					"Failed to perform aggregation cleanup for job execution: {}", jobExecutionId, cleanupException
			);
		}
	}
}