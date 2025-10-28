package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for VDYP batch processing. This class handles all custom batch.* properties to eliminate
 * unknown property warnings.
 */
@Component
@ConfigurationProperties(prefix = "batch")
public class BatchProperties {

	private Job job = new Job();
	private Partition partition = new Partition();
	private ThreadPool threadPool = new ThreadPool();
	private Validation validation = new Validation();
	private Retry retry = new Retry();
	private Skip skip = new Skip();
	private Reader reader = new Reader();
	private String rootDirectory;

	public static class Job {
		private boolean autoCreate = true;
		private String baseFolderPrefix;

		public boolean isAutoCreate() {
			return autoCreate;
		}

		public void setAutoCreate(boolean autoCreate) {
			this.autoCreate = autoCreate;
		}

		public String getBaseFolderPrefix() {
			return baseFolderPrefix;
		}

		public void setBaseFolderPrefix(String baseFolderPrefix) {
			this.baseFolderPrefix = baseFolderPrefix;
		}
	}

	public static class Partition {
		private Integer defaultPartitionSize;
		private String inputPolygonFileName;
		private String inputLayerFileName;
		private String inputFolderNamePrefix;
		private String outputFolderNamePrefix;
		private String namePrefix;

		public Integer getDefaultPartitionSize() {
			return defaultPartitionSize;
		}

		public void setDefaultPartitionSize(Integer defaultPartitionSize) {
			this.defaultPartitionSize = defaultPartitionSize;
		}

		public String getInputPolygonFileName() {
			return inputPolygonFileName;
		}

		public void setInputPolygonFileName(String inputPolygonFileName) {
			this.inputPolygonFileName = inputPolygonFileName;
		}

		public String getInputLayerFileName() {
			return inputLayerFileName;
		}

		public void setInputLayerFileName(String inputLayerFileName) {
			this.inputLayerFileName = inputLayerFileName;
		}

		public String getInputFolderNamePrefix() {
			return inputFolderNamePrefix;
		}

		public void setInputFolderNamePrefix(String inputFolderNamePrefix) {
			this.inputFolderNamePrefix = inputFolderNamePrefix;
		}

		public String getOutputFolderNamePrefix() {
			return outputFolderNamePrefix;
		}

		public void setOutputFolderNamePrefix(String outputFolderNamePrefix) {
			this.outputFolderNamePrefix = outputFolderNamePrefix;
		}

		public String getNamePrefix() {
			return namePrefix;
		}

		public void setNamePrefix(String namePrefix) {
			this.namePrefix = namePrefix;
		}
	}

	public static class Retry {
		private int maxAttempts;
		private int backoffPeriod;

		public int getMaxAttempts() {
			return maxAttempts;
		}

		public void setMaxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public int getBackoffPeriod() {
			return backoffPeriod;
		}

		public void setBackoffPeriod(int backoffPeriod) {
			this.backoffPeriod = backoffPeriod;
		}
	}

	public static class ThreadPool {
		private int corePoolSize;
		private int maxPoolSizeMultiplier;
		private String threadNamePrefix;

		public int getCorePoolSize() {
			return corePoolSize;
		}

		public void setCorePoolSize(int corePoolSize) {
			this.corePoolSize = corePoolSize;
		}

		public int getMaxPoolSizeMultiplier() {
			return maxPoolSizeMultiplier;
		}

		public void setMaxPoolSizeMultiplier(int maxPoolSizeMultiplier) {
			this.maxPoolSizeMultiplier = maxPoolSizeMultiplier;
		}

		public String getThreadNamePrefix() {
			return threadNamePrefix;
		}

		public void setThreadNamePrefix(String threadNamePrefix) {
			this.threadNamePrefix = threadNamePrefix;
		}
	}

	public static class Validation {
		private int maxDataLength;
		private int minPolygonIdLength;
		private int maxPolygonIdLength;

		public int getMaxDataLength() {
			return maxDataLength;
		}

		public void setMaxDataLength(int maxDataLength) {
			this.maxDataLength = maxDataLength;
		}

		public int getMinPolygonIdLength() {
			return minPolygonIdLength;
		}

		public void setMinPolygonIdLength(int minPolygonIdLength) {
			this.minPolygonIdLength = minPolygonIdLength;
		}

		public int getMaxPolygonIdLength() {
			return maxPolygonIdLength;
		}

		public void setMaxPolygonIdLength(int maxPolygonIdLength) {
			this.maxPolygonIdLength = maxPolygonIdLength;
		}
	}

	public static class Skip {
		private int maxCount;

		public int getMaxCount() {
			return maxCount;
		}

		public void setMaxCount(int maxCount) {
			this.maxCount = maxCount;
		}
	}

	public static class Reader {
		private Integer defaultChunkSize;

		public Integer getDefaultChunkSize() {
			return defaultChunkSize;
		}

		public void setDefaultChunkSize(Integer defaultChunkSize) {
			this.defaultChunkSize = defaultChunkSize;
		}
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public Partition getPartition() {
		return partition;
	}

	public void setPartition(Partition partition) {
		this.partition = partition;
	}

	public Retry getRetry() {
		return retry;
	}

	public void setRetry(Retry retry) {
		this.retry = retry;
	}

	public ThreadPool getThreadPool() {
		return threadPool;
	}

	public void setThreadPool(ThreadPool threadPool) {
		this.threadPool = threadPool;
	}

	public Validation getValidation() {
		return validation;
	}

	public void setValidation(Validation validation) {
		this.validation = validation;
	}

	public Skip getSkip() {
		return skip;
	}

	public void setSkip(Skip skip) {
		this.skip = skip;
	}

	public Reader getReader() {
		return reader;
	}

	public void setReader(Reader reader) {
		this.reader = reader;
	}

	public String getRootDirectory() {
		return rootDirectory;
	}

	public void setRootDirectory(String rootDirectory) {
		this.rootDirectory = rootDirectory;
	}
}
