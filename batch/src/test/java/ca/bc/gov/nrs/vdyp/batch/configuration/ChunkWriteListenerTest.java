package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.util.BatchConstants;

class ChunkWriteListenerTest {

	private ChunkWriteListener listener;
	private ExecutionContext executionContext;
	private StepExecution stepExecution;

	@BeforeEach
	void setUp() {
		listener = new ChunkWriteListener();
		executionContext = new ExecutionContext();

		stepExecution = mock(StepExecution.class);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
		when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

		listener.beforeStep(stepExecution);
	}

	@Test
	void afterStep_returnsStepExitStatus() {
		assertThat(listener.afterStep(stepExecution), is(ExitStatus.COMPLETED));
	}

	@Test
	void afterWrite_emptyChunk_doesNotUpdateCounters() {
		listener.afterWrite(new Chunk<>());

		assertThat(executionContext.getInt(BatchConstants.Job.POLYGONS_PROCESSED, -1), is(-1));
	}

	@Test
	void afterWrite_successfulChunk_accumulatesProcessedErrorAndSkippedCounts() {
		BatchChunkMetadata meta = new BatchChunkMetadata("partition0", "/base", 0, 150, 0, 150, 1);
		meta.setErrorCount(3);
		meta.setSkippedPolygonCount(2);

		listener.afterWrite(new Chunk<>(meta));

		assertThat(executionContext.getInt(BatchConstants.Job.POLYGONS_PROCESSED, 0), is(150));
		assertThat(executionContext.getInt(BatchConstants.Job.PROJECTION_ERRORS, 0), is(3));
		assertThat(executionContext.getInt(BatchConstants.Job.POLYGONS_SKIPPED, 0), is(2));
	}

	@Test
	void onSkipInWrite_addsWholeChunkRecordCountToPolygonsSkipped() {
		BatchChunkMetadata meta = new BatchChunkMetadata("partition0", "/base", 0, 150, 0, 150, 1);

		listener.onSkipInWrite(meta, new RuntimeException("chunk projection failed"));

		assertThat(executionContext.getInt(BatchConstants.Job.POLYGONS_SKIPPED, 0), is(150));
	}

	@Test
	void onSkipInWrite_accumulatesAcrossMultipleSkippedChunks() {
		BatchChunkMetadata first = new BatchChunkMetadata("partition0", "/base", 0, 150, 0, 150, 1);
		BatchChunkMetadata second = new BatchChunkMetadata("partition0", "/base", 150, 100, 150, 100, 2);

		listener.onSkipInWrite(first, new RuntimeException("chunk projection failed"));
		listener.onSkipInWrite(second, new RuntimeException("chunk storage failed"));

		assertThat(executionContext.getInt(BatchConstants.Job.POLYGONS_SKIPPED, 0), is(250));
	}
}
