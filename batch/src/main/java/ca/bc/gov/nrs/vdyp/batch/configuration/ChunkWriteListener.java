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

		ExecutionContext stepContext = stepExecution.getExecutionContext();

		int polygonsProcessed = stepContext.getInt("polygonsProcessed", 0);

		polygonsProcessed += meta.getPolygonRecordCount();

		stepContext.putInt("polygonsProcessed", polygonsProcessed);

		int projectionErrors = stepContext.getInt("projectionErrors", 0);

		projectionErrors += meta.getErrorCount();

		stepContext.putInt("projectionErrors", projectionErrors);

	}

}
