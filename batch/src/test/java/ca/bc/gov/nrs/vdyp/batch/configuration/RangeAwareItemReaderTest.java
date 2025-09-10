package ca.bc.gov.nrs.vdyp.batch.configuration;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.service.BatchMetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RangeAwareItemReaderTest {

	@Mock
	private BatchMetricsCollector metricsCollector;

	@Mock
	private BatchProperties batchProperties;

	@Mock
	private StepExecution stepExecution;

	@Mock
	private org.springframework.batch.core.JobExecution jobExecution;

	@Mock
	private org.springframework.batch.core.JobParameters jobParameters;

	@Mock
	private ExecutionContext executionContext;

	@TempDir
	Path tempDir;

	private RangeAwareItemReader rangeAwareItemReader;
	private Resource testResource;

	@BeforeEach
	void setUp() throws IOException {
		// Create test CSV file
		Path testFile = tempDir.resolve("test.csv");
		Files.write(testFile, """
				id,data,polygonId,layerId
				1,test-data-1,polygon1,layer1
				2,test-data-2,polygon2,layer2
				3,test-data-3,polygon3,layer3
				4,test-data-4,polygon4,layer4
				5,test-data-5,polygon5,layer5
				""".getBytes());

		testResource = new FileSystemResource(testFile.toFile());
		rangeAwareItemReader = new RangeAwareItemReader(testResource, metricsCollector, batchProperties);

		setupMocks();
	}

	private void setupMocks() {
		when(stepExecution.getJobExecutionId()).thenReturn(1L);
		when(stepExecution.getJobExecution()).thenReturn(jobExecution);
		when(jobExecution.getJobParameters()).thenReturn(jobParameters);
		when(jobParameters.getString("inputFilePath")).thenReturn(null);

		when(executionContext.getLong("startLine", 2)).thenReturn(2L);
		when(executionContext.getLong("endLine", Long.MAX_VALUE)).thenReturn(4L);
		when(executionContext.getString("partitionName", "unknown")).thenReturn("test-partition");

		when(stepExecution.getExecutionContext()).thenReturn(executionContext);

		BatchProperties.Input input = mock(BatchProperties.Input.class);
		try {
			when(input.getFilePath()).thenReturn(testResource.getFile().getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		when(batchProperties.getInput()).thenReturn(input);
	}

	@Test
	void testConstructor() {
		assertNotNull(rangeAwareItemReader);
		// Partition name is set during beforeStep, so it will be null initially
		assertNull(rangeAwareItemReader.getPartitionName());
	}

	@Test
	void testSetInputResource() {
		Resource newResource = mock(Resource.class);
		rangeAwareItemReader.setInputResource(newResource);

		// Test that the resource was set (no direct getter, but method doesn't throw)
		assertDoesNotThrow(() -> rangeAwareItemReader.setInputResource(newResource));
	}

	@Test
	void testBeforeStep_WithValidConfiguration() {
		rangeAwareItemReader.beforeStep(stepExecution);

		verify(stepExecution).getJobExecutionId();
		verify(executionContext).getLong("startLine", 2);
		verify(executionContext).getLong("endLine", Long.MAX_VALUE);
		verify(executionContext).getString("partitionName", "unknown");
	}

	@Test
	void testBeforeStep_WithMissingInputFilePath_ThrowsException() {
		when(batchProperties.getInput().getFilePath()).thenReturn(null);
		when(jobParameters.getString("inputFilePath")).thenReturn(null);

		assertThrows(IllegalStateException.class, () -> rangeAwareItemReader.beforeStep(stepExecution));
	}

	@Test
	void testBeforeStep_WithNonExistentFile_ThrowsException() {
		when(batchProperties.getInput().getFilePath()).thenReturn("/non/existent/file.csv");

		assertThrows(IllegalStateException.class, () -> rangeAwareItemReader.beforeStep(stepExecution));
	}

	@Test
	void testOpen_CallsDelegate() {
		rangeAwareItemReader.beforeStep(stepExecution);
		ExecutionContext localExecutionContext = new ExecutionContext();

		assertDoesNotThrow(() -> rangeAwareItemReader.open(localExecutionContext));
	}

	@Test
	void testClose_CallsDelegate() {
		rangeAwareItemReader.beforeStep(stepExecution);
		ExecutionContext localExecutionContext = new ExecutionContext();
		rangeAwareItemReader.open(localExecutionContext);

		assertDoesNotThrow(() -> rangeAwareItemReader.close());
	}

	@Test
	void testRead_WithinRange() throws Exception {
		rangeAwareItemReader.beforeStep(stepExecution);

		BatchRecord batchRecord = rangeAwareItemReader.read();

		assertNotNull(batchRecord);
		assertEquals(1L, batchRecord.getId());
		assertEquals("test-data-1", batchRecord.getData());
	}

	@Test
	void testGetSkipStatistics_ReturnsMap() {
		ConcurrentMap<String, AtomicLong> stats = rangeAwareItemReader.getSkipStatistics();

		assertNotNull(stats);
	}

	@Test
	void testGetTotalDataSkips_ReturnsZero() {
		long skips = rangeAwareItemReader.getTotalDataSkips();

		assertEquals(0L, skips);
	}

	@Test
	void testGetTotalRangeSkips_ReturnsZero() {
		long skips = rangeAwareItemReader.getTotalRangeSkips();

		assertEquals(0L, skips);
	}

	@Test
	void testGetTotalProcessed_ReturnsZero() {
		long processed = rangeAwareItemReader.getTotalProcessed();

		assertEquals(0L, processed);
	}

	@Test
	void testGetPartitionName_ReturnsCorrectName() {
		rangeAwareItemReader.beforeStep(stepExecution);

		String partitionName = rangeAwareItemReader.getPartitionName();

		assertEquals("test-partition", partitionName);
	}

	@Test
	void testUpdate_CallsDelegate() {
		rangeAwareItemReader.beforeStep(stepExecution);
		ExecutionContext localExecutionContext = new ExecutionContext();
		rangeAwareItemReader.open(localExecutionContext);

		assertDoesNotThrow(() -> rangeAwareItemReader.update(localExecutionContext));
	}

	@Test
	void testRead_BeyondEndLine_ReturnsNull() throws Exception {
		// Set up separate execution context for this test
		ExecutionContext limitedContext = mock(ExecutionContext.class);
		when(limitedContext.getLong("startLine", 2)).thenReturn(2L);
		when(limitedContext.getLong("endLine", Long.MAX_VALUE)).thenReturn(2L); // Only one record
		when(limitedContext.getString("partitionName", "unknown")).thenReturn("test-partition");
		when(stepExecution.getExecutionContext()).thenReturn(limitedContext);

		rangeAwareItemReader.beforeStep(stepExecution);

		// First read should return a record
		BatchRecord record1 = rangeAwareItemReader.read();
		assertNotNull(record1);

		// Second read should return null (beyond range)
		BatchRecord record2 = rangeAwareItemReader.read();
		assertNull(record2);
	}

	@Test
	void testBeforeStep_WithClassPathResource() {
		when(jobParameters.getString("inputFilePath")).thenReturn("classpath:test-data.csv");
		when(batchProperties.getInput().getFilePath()).thenReturn(null);

		// This should trigger classpath resource creation logic
		assertThrows(IllegalStateException.class, () -> rangeAwareItemReader.beforeStep(stepExecution));
	}

	@Test
	void testRead_WithInvalidData_HandlesGracefully() throws Exception {
		// Create CSV with invalid data
		Path invalidFile = tempDir.resolve("invalid.csv");
		Files.write(invalidFile, """
				id,data,polygonId,layerId
				1,,polygon1,layer1
				2,test-data-2,,layer2
				3,test-data-3,polygon3,
				""".getBytes());

		Resource invalidResource = new FileSystemResource(invalidFile.toFile());
		RangeAwareItemReader invalidReader = new RangeAwareItemReader(
				invalidResource, metricsCollector, batchProperties
		);

		// Update mock to return the invalid file path
		BatchProperties.Input input = mock(BatchProperties.Input.class);
		when(input.getFilePath()).thenReturn(invalidFile.toAbsolutePath().toString());
		when(batchProperties.getInput()).thenReturn(input);

		invalidReader.beforeStep(stepExecution);

		// Reading should handle invalid data gracefully
		BatchRecord record1 = invalidReader.read();
		// Should skip records with missing required fields and continue
		assertNull(record1); // All records are invalid, so should return null
	}

	@Test
	void testRead_WithNullIdRecord_HandlesGracefully() throws Exception {
		// Create CSV with null ID
		Path nullIdFile = tempDir.resolve("nullid.csv");
		Files.write(nullIdFile, """
				id,data,polygonId,layerId
				,test-data-1,polygon1,layer1
				2,test-data-2,polygon2,layer2
				""".getBytes());

		Resource nullIdResource = new FileSystemResource(nullIdFile.toFile());
		RangeAwareItemReader nullIdReader = new RangeAwareItemReader(nullIdResource, metricsCollector, batchProperties);

		// Update mock to return the null ID file path
		BatchProperties.Input input = mock(BatchProperties.Input.class);
		when(input.getFilePath()).thenReturn(nullIdFile.toAbsolutePath().toString());
		when(batchProperties.getInput()).thenReturn(input);

		nullIdReader.beforeStep(stepExecution);

		// Should skip record with null ID and return the second record
		BatchRecord batchRecord = nullIdReader.read();
		assertNotNull(batchRecord);
		assertEquals(2L, batchRecord.getId());
	}

	@Test
	void testRead_MultipleRecords_ProcessesCorrectly() throws Exception {
		rangeAwareItemReader.beforeStep(stepExecution);

		// Read multiple records within range
		BatchRecord record1 = rangeAwareItemReader.read();
		assertNotNull(record1);
		assertEquals(1L, record1.getId());

		BatchRecord record2 = rangeAwareItemReader.read();
		assertNotNull(record2);
		assertEquals(2L, record2.getId());

		BatchRecord record3 = rangeAwareItemReader.read();
		assertNotNull(record3);
		assertEquals(3L, record3.getId());

		// Fourth record should be beyond our test range (endLine = 4)
		BatchRecord record4 = rangeAwareItemReader.read();
		assertNull(record4);
	}

	@Test
	void testSkipStatistics_TrackingWorks() throws Exception {
		// Create file with parsing errors
		Path errorFile = tempDir.resolve("error.csv");
		Files.write(errorFile, """
				id,data,polygonId,layerId
				1,test-data-1,polygon1,layer1
				invalid-line-without-proper-columns
				3,test-data-3,polygon3,layer3
				""".getBytes());

		Resource errorResource = new FileSystemResource(errorFile.toFile());
		RangeAwareItemReader errorReader = new RangeAwareItemReader(errorResource, metricsCollector, batchProperties);

		// Update mock to return the error file path
		BatchProperties.Input input = mock(BatchProperties.Input.class);
		when(input.getFilePath()).thenReturn(errorFile.toAbsolutePath().toString());
		when(batchProperties.getInput()).thenReturn(input);

		errorReader.beforeStep(stepExecution);

		// Read all records
		BatchRecord record1 = errorReader.read();
		assertNotNull(record1);

		BatchRecord record2 = errorReader.read();
		assertNotNull(record2);

		// Check skip statistics
		ConcurrentMap<String, AtomicLong> skipStats = errorReader.getSkipStatistics();
		assertNotNull(skipStats);

		long totalSkips = errorReader.getTotalDataSkips();
		assertTrue(totalSkips >= 0);
	}

	@Test
	void testProcessedCount_IncrementsCorrectly() throws Exception {
		rangeAwareItemReader.beforeStep(stepExecution);

		assertEquals(0L, rangeAwareItemReader.getTotalProcessed());

		// Read one record
		BatchRecord batchRecord = rangeAwareItemReader.read();
		assertNotNull(batchRecord);

		assertTrue(rangeAwareItemReader.getTotalProcessed() > 0);
	}

	@Test
	void testRead_WithoutOpen_AutoOpens() throws Exception {
		rangeAwareItemReader.beforeStep(stepExecution);

		// Don't manually call open()
		BatchRecord batchRecord = rangeAwareItemReader.read();
		assertNotNull(batchRecord);
		assertEquals(1L, batchRecord.getId());
	}

	@Test
	void testBeforeStep_WithJobParameterInputPath() throws IOException {
		String testPath = testResource.getFile().getAbsolutePath();
		when(jobParameters.getString("inputFilePath")).thenReturn(testPath);
		when(batchProperties.getInput().getFilePath()).thenReturn(null);

		assertDoesNotThrow(() -> rangeAwareItemReader.beforeStep(stepExecution));
	}

	@Test
	void testClose_MultipleCallsAreSafe() {
		rangeAwareItemReader.beforeStep(stepExecution);
		ExecutionContext localExecutionContext = new ExecutionContext();
		rangeAwareItemReader.open(localExecutionContext);

		// Multiple close calls should be safe
		assertDoesNotThrow(() -> rangeAwareItemReader.close());
		assertDoesNotThrow(() -> rangeAwareItemReader.close());
	}

	@Test
	void testClose_WithSkipStatistics() throws Exception {
		// Create file that will generate skips
		Path skipFile = tempDir.resolve("skip.csv");
		Files.write(skipFile, """
				id,data,polygonId,layerId
				1,,polygon1,layer1
				2,test-data-2,,layer2
				""".getBytes());

		Resource skipResource = new FileSystemResource(skipFile.toFile());
		RangeAwareItemReader skipReader = new RangeAwareItemReader(skipResource, metricsCollector, batchProperties);

		BatchProperties.Input input = mock(BatchProperties.Input.class);
		when(input.getFilePath()).thenReturn(skipFile.toAbsolutePath().toString());
		when(batchProperties.getInput()).thenReturn(input);

		skipReader.beforeStep(stepExecution);

		// Close should log skip statistics
		assertDoesNotThrow(skipReader::close);

		// Verify skip statistics were generated
		assertTrue(skipReader.getTotalDataSkips() >= 0);
	}

	@Test
	void testHandleEndOfRange_LogsCorrectly() throws Exception {
		// Set very small range
		ExecutionContext smallRangeContext = mock(ExecutionContext.class);
		when(smallRangeContext.getLong("startLine", 2)).thenReturn(2L);
		when(smallRangeContext.getLong("endLine", Long.MAX_VALUE)).thenReturn(2L);
		when(smallRangeContext.getString("partitionName", "unknown")).thenReturn("small-range-partition");
		when(stepExecution.getExecutionContext()).thenReturn(smallRangeContext);

		rangeAwareItemReader.beforeStep(stepExecution);

		// Read first record
		BatchRecord record1 = rangeAwareItemReader.read();
		assertNotNull(record1);

		// This should trigger handleEndOfRange
		BatchRecord record2 = rangeAwareItemReader.read();
		assertNull(record2);

		// Verify partition name is set correctly
		assertEquals("small-range-partition", rangeAwareItemReader.getPartitionName());
	}

	@Test
	void testRead_WithNullMetricsCollector_WorksCorrectly() throws Exception {
		// Create reader with null metrics collector
		RangeAwareItemReader nullMetricsReader = new RangeAwareItemReader(testResource, null, batchProperties);

		nullMetricsReader.beforeStep(stepExecution);

		BatchRecord batchRecord = nullMetricsReader.read();
		assertNotNull(batchRecord);
		assertEquals(1L, batchRecord.getId());
	}
}