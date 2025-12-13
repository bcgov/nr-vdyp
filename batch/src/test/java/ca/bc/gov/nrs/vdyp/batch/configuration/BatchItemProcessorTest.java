package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import ca.bc.gov.nrs.vdyp.batch.exception.BatchConfigurationException;
import ca.bc.gov.nrs.vdyp.batch.exception.BatchException;
import ca.bc.gov.nrs.vdyp.batch.model.BatchChunkMetadata;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchItemProcessorTest {

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private StepExecution stepExecution;

	@Mock
	private ExecutionContext executionContext;

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobParameters jobParameters;

	private BatchItemProcessor processor;

	private static final String JOB_GUID = "7c26643a-50cb-497e-a539-afac6966ecea";

	@BeforeEach
	void setUp() {
		processor = new BatchItemProcessor(metricsCollector);

		setupStepExecution();
	}

	private void setupStepExecution() {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("jobGuid")).thenReturn(JOB_GUID);
		when(executionContext.getString("partitionName")).thenReturn("test-partition");
		when(executionContext.getLong("startLine", 0)).thenReturn(1L);
		when(executionContext.getLong("endLine", 0)).thenReturn(100L);
		when(stepExecution.getExecutionContext()).thenReturn(executionContext);
	}

	@Test
	void testBeforeStep_InitializesProcessor() throws BatchException {
		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(stepExecution).getJobExecution();
		verify(jobExecution).getJobParameters();
		verify(jobParameters).getString("jobGuid");
		verify(stepExecution).getExecutionContext();
		verify(executionContext).getString("partitionName");
		verify(metricsCollector).initializePartitionMetrics(1L, JOB_GUID, "test-partition");
	}

	@Test
	void testProcess_WithValidChunkMetadata_ReturnsChunkUnchanged() throws BatchException {
		processor.beforeStep(stepExecution);

		BatchChunkMetadata chunkMetadata = createChunkMetadata("test-partition", 0, 10);

		BatchChunkMetadata result = processor.process(chunkMetadata);

		assertNotNull(result);
		assertEquals(0, result.getStartIndex());
		assertEquals(10, result.getRecordCount());
		assertEquals(chunkMetadata, result);
	}

	@Test
	void testBeforeStep_WithNullMetricsCollector_DoesNotThrowException() throws BatchException {
		processor = new BatchItemProcessor(null);

		processor.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(executionContext).getString("partitionName");
		// No metrics collector calls should be made when metricsCollector is null
	}

	@Test
	void testBeforeStep_CalledTwice_ThrowsException() throws BatchException {
		processor.beforeStep(stepExecution);

		assertThrows(
				BatchConfigurationException.class, () -> processor.beforeStep(stepExecution),
				"BatchItemProcessor already initialized. beforeStep() should only be called once."
		);
	}

	private BatchChunkMetadata createChunkMetadata(String partitionName, int startIndex, int recordCount) {
		return new BatchChunkMetadata(partitionName, "/tmp/test-job", startIndex, recordCount);
	}
}
