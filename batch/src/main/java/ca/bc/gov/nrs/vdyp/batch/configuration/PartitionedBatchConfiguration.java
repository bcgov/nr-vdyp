package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
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
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * VDYP Batch Configuration with partitioning, error handling, and detailed metrics collection.
 */
@Configuration
public class PartitionedBatchConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(PartitionedBatchConfiguration.class);

	private final JobRepository jobRepository;
	private final BatchMetricsCollector metricsCollector;
	private final BatchProperties batchProperties;

	public PartitionedBatchConfiguration(
			JobRepository jobRepository, BatchMetricsCollector metricsCollector, BatchProperties batchProperties
	) {
		this.jobRepository = jobRepository;
		this.metricsCollector = metricsCollector;
		this.batchProperties = batchProperties;
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
			BatchMetricsCollector metricsCollector, BatchProperties batchProperties
	) {
		int chunkSize = batchProperties.getPartitioning().getChunkSize();
		if (chunkSize <= 0) {
			throw new IllegalStateException(
					"batch.partitioning.chunk-size must be configured with a positive value in application.properties"
			);
		}

		return new StepBuilder("workerStep", jobRepository)
				.<BatchRecord, BatchRecord>chunk(chunkSize, transactionManager).reader(partitionReader(metricsCollector, batchProperties))
				.processor(vdypProjectionProcessor(retryPolicy, metricsCollector)).writer(partitionWriter(null, null)).faultTolerant()
				.retryPolicy(retryPolicy).skipPolicy(skipPolicy).listener(new StepExecutionListener() {
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
	public Job partitionedJob(PartitionedJobExecutionListener jobExecutionListener, Step masterStep) {
		return new JobBuilder("VdypPartitionedJob", jobRepository).incrementer(new RunIdIncrementer()).start(masterStep)
				.listener(new JobExecutionListener() {
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

	@Bean
	@StepScope
	public RangeAwareItemReader partitionReader(BatchMetricsCollector metricsCollector, BatchProperties batchProperties) {
		return new RangeAwareItemReader(null, metricsCollector, batchProperties);
	}

	@Bean
	@StepScope
	public FlatFileItemWriter<BatchRecord> partitionWriter(
			@Value("#{stepExecutionContext['partitionName']}") String partitionName,
			@Value("#{jobParameters['outputFilePath']}") String outputFilePath
	) {

		String actualPartitionName = partitionName != null ? partitionName : UNKNOWN;

		String actualOutputDirectory = outputFilePath;
		if (actualOutputDirectory == null) {
			actualOutputDirectory = batchProperties.getOutput().getDirectory().getDefaultPath();
		}
		if (actualOutputDirectory == null) {
			actualOutputDirectory = System.getProperty("java.io.tmpdir");
			logger.warn("No output directory specified, using system temp directory: {}", actualOutputDirectory);
		}

		String filePrefix = batchProperties.getOutput().getFilePrefix();
		if (filePrefix == null) {
			throw new IllegalStateException("batch.output.file-prefix must be configured in application.properties");
		}

		String csvHeader = batchProperties.getOutput().getCsvHeader();
		if (csvHeader == null || csvHeader.trim().isEmpty()) {
			throw new IllegalStateException("batch.output.csv-header must be configured in application.properties");
		}

		String partitionOutputPath = actualOutputDirectory + File.separator + filePrefix + "_" + actualPartitionName + ".csv";

		try {
			Files.createDirectories(Paths.get(actualOutputDirectory));
		} catch (Exception e) {
			logger.error("Failed to create output directory: {}", e.getMessage());
		}

		FlatFileItemWriter<BatchRecord> writer = new FlatFileItemWriter<>();
		writer.setResource(new org.springframework.core.io.FileSystemResource(partitionOutputPath));
		writer.setName("VdypItemWriter_" + actualPartitionName);
		writer.setHeaderCallback(w -> {
			logger.info("[{}] VDYP Writer: Writing header to file {}", actualPartitionName, partitionOutputPath);
			w.write(csvHeader);
		});
		writer.setLineAggregator(item -> item.getId() + ","
				+ (item.getData() != null ? "\"" + item.getData().replace("\"", "\"\"") + "\"" : "") + ","
				+ (item.getPolygonId() != null ? item.getPolygonId() : "") + ","
				+ (item.getLayerId() != null ? item.getLayerId() : "") + "," + "PROCESSED");

		logger.info("[{}] VDYP Writer configured for output path: {}", actualPartitionName, partitionOutputPath);

		return writer;
	}

	@Bean
	@StepScope
	public VdypProjectionProcessor vdypProjectionProcessor(BatchRetryPolicy retryPolicy, BatchMetricsCollector metricsCollector) {
		return new VdypProjectionProcessor(retryPolicy, metricsCollector);
	}
}