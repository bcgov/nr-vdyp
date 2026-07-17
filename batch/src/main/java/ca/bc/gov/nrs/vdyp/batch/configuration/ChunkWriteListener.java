package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

public class ChunkWriteListener implements ItemWriteListener<BatchChunkMetadata>, StepExecutionListener,
		SkipListener<BatchChunkMetadata, BatchChunkMetadata> {
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

		ExecutionContext stepCtx = stepExecution.getExecutionContext();

		// A chunk's record count can include polygons extended-core itself skipped internally
		// (meta.getSkippedPolygonCount()); those are reported separately below, so only the
		// remainder is genuinely "processed" - otherwise they'd be double-counted.
		int polygonsProcessed = stepCtx.getInt(BatchConstants.Job.POLYGONS_PROCESSED, 0);
		polygonsProcessed += meta.getPolygonRecordCount() - meta.getSkippedPolygonCount();
		stepCtx.putInt(BatchConstants.Job.POLYGONS_PROCESSED, polygonsProcessed);

		int projectionErrors = stepCtx.getInt(BatchConstants.Job.PROJECTION_ERRORS, 0);
		projectionErrors += meta.getErrorCount();
		stepCtx.putInt(BatchConstants.Job.PROJECTION_ERRORS, projectionErrors);

		int polygonsSkipped = stepCtx.getInt(BatchConstants.Job.POLYGONS_SKIPPED, 0);
		polygonsSkipped += meta.getSkippedPolygonCount();
		stepCtx.putInt(BatchConstants.Job.POLYGONS_SKIPPED, polygonsSkipped);

	}

	/**
	 * Called when an entire chunk's write fails and the skip policy accepts the failure. Because the write never
	 * completed, afterWrite never ran for this item, so its polygons would otherwise be dropped from every progress
	 * counter. Count them as skipped so processed + skipped reconciles with the total.
	 */
	@Override
	public void onSkipInWrite(BatchChunkMetadata item, Throwable t) {
		ExecutionContext stepCtx = stepExecution.getExecutionContext();

		int polygonsSkipped = stepCtx.getInt(BatchConstants.Job.POLYGONS_SKIPPED, 0);
		polygonsSkipped += item.getPolygonRecordCount();
		stepCtx.putInt(BatchConstants.Job.POLYGONS_SKIPPED, polygonsSkipped);
	}

}
