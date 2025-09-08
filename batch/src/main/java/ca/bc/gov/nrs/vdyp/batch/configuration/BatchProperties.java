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
	private Input input = new Input();
	private Output output = new Output();
	private Partitioning partitioning = new Partitioning();
	private ThreadPool threadPool = new ThreadPool();
	private Validation validation = new Validation();
	private Error error = new Error();
	private Retry retry = new Retry();
	private Skip skip = new Skip();

	public static class Job {
		private boolean autoCreate = true;

		public boolean isAutoCreate() {
			return autoCreate;
		}

		public void setAutoCreate(boolean autoCreate) {
			this.autoCreate = autoCreate;
		}
	}

	public static class Input {
		private String filePath;

		public String getFilePath() {
			return filePath;
		}

		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}
	}

	public static class Output {
		private Directory directory = new Directory();
		private String filePrefix;
		private String csvHeader;

		public static class Directory {
			private String defaultPath;

			public String getDefaultPath() {
				return defaultPath;
			}

			public void setDefaultPath(String defaultPath) {
				this.defaultPath = defaultPath;
			}
		}

		public Directory getDirectory() {
			return directory;
		}

		public void setDirectory(Directory directory) {
			this.directory = directory;
		}

		public String getFilePrefix() {
			return filePrefix;
		}

		public void setFilePrefix(String filePrefix) {
			this.filePrefix = filePrefix;
		}

		public String getCsvHeader() {
			return csvHeader;
		}

		public void setCsvHeader(String csvHeader) {
			this.csvHeader = csvHeader;
		}
	}

	public static class Partitioning {
		private boolean enabled = true;
		private int gridSize;
		private int chunkSize;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getGridSize() {
			return gridSize;
		}

		public void setGridSize(int gridSize) {
			this.gridSize = gridSize;
		}

		public int getChunkSize() {
			return chunkSize;
		}

		public void setChunkSize(int chunkSize) {
			this.chunkSize = chunkSize;
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

	public static class Error {
		private String transientPatterns;
		private int maxConsecutiveFailures;

		public String getTransientPatterns() {
			return transientPatterns;
		}

		public void setTransientPatterns(String transientPatterns) {
			this.transientPatterns = transientPatterns;
		}

		public int getMaxConsecutiveFailures() {
			return maxConsecutiveFailures;
		}

		public void setMaxConsecutiveFailures(int maxConsecutiveFailures) {
			this.maxConsecutiveFailures = maxConsecutiveFailures;
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

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}

	public Output getOutput() {
		return output;
	}

	public void setOutput(Output output) {
		this.output = output;
	}

	public Partitioning getPartitioning() {
		return partitioning;
	}

	public void setPartitioning(Partitioning partitioning) {
		this.partitioning = partitioning;
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

	public Error getError() {
		return error;
	}

	public void setError(Error error) {
		this.error = error;
	}

	public Skip getSkip() {
		return skip;
	}

	public void setSkip(Skip skip) {
		this.skip = skip;
	}
}