package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;

public class ChunkWriteListener implements ItemWriteListener<BatchChunkMetadata>, StepExecutionListener {
	private static final Logger logger = LoggerFactory.getLogger(ChunkWriteListener.class);
	private StepExecution stepExecution;

	@Override
	public void beforeStep(@NonNull StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		return stepExecution.getExitStatus();
	}

	@Override
	public void afterWrite(Chunk<? extends BatchChunkMetadata> chunk) {
		if (chunk.isEmpty()) {
			return;
		}
		var items = chunk.getItems();
		BatchChunkMetadata meta = items.get(0); // this is assuming the Chuck size of 1

		ExecutionContext jobCtx = stepExecution.getJobExecution().getExecutionContext();

		synchronized (jobCtx) {

			int polygonsProcessed = jobCtx.getInt("polygonsProcessed", 0);
			polygonsProcessed += meta.getPolygonRecordCount();
			jobCtx.putInt("polygonsProcessed", polygonsProcessed);

			int projectionErrors = jobCtx.getInt("projectionErrors", 0);
			projectionErrors += meta.getErrorCount();
			jobCtx.putInt("projectionErrors", projectionErrors);

			int skippedPolygons = jobCtx.getInt("skippedPolygons", 0);
			skippedPolygons += meta.getSkippedPolygonCount();
			jobCtx.putInt("skippedPolygons", skippedPolygons);

			int progressVersion = jobCtx.getInt("progressVersion", 0);
			progressVersion += 1;
			jobCtx.putInt("progressVersion", progressVersion);

		}
	}

}
