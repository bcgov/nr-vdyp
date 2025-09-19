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
	private Output output = new Output();
	private Partitioning partitioning = new Partitioning();
	private ThreadPool threadPool = new ThreadPool();
	private Validation validation = new Validation();
	private Retry retry = new Retry();
	private Skip skip = new Skip();
	private Vdyp vdyp = new Vdyp();

	public static class Job {
		private boolean autoCreate = true;

		public boolean isAutoCreate() {
			return autoCreate;
		}

		public void setAutoCreate(boolean autoCreate) {
			this.autoCreate = autoCreate;
		}
	}

	public static class Output {
		private Directory directory = new Directory();

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

	}

	public static class Partitioning {
		private int gridSize;

		public int getGridSize() {
			return gridSize;
		}

		public void setGridSize(int gridSize) {
			this.gridSize = gridSize;
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

	public static class Vdyp {
		private Projection projection = new Projection();

		public static class Projection {
			private String polygonFile;
			private String layerFile;
			private String parametersFile;

			public String getPolygonFile() {
				return polygonFile;
			}

			public void setPolygonFile(String polygonFile) {
				this.polygonFile = polygonFile;
			}

			public String getLayerFile() {
				return layerFile;
			}

			public void setLayerFile(String layerFile) {
				this.layerFile = layerFile;
			}

			public String getParametersFile() {
				return parametersFile;
			}

			public void setParametersFile(String parametersFile) {
				this.parametersFile = parametersFile;
			}
		}

		public Projection getProjection() {
			return projection;
		}

		public void setProjection(Projection projection) {
			this.projection = projection;
		}
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
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

	public Skip getSkip() {
		return skip;
	}

	public void setSkip(Skip skip) {
		this.skip = skip;
	}

	public Vdyp getVdyp() {
		return vdyp;
	}

	public void setVdyp(Vdyp vdyp) {
		this.vdyp = vdyp;
	}
}