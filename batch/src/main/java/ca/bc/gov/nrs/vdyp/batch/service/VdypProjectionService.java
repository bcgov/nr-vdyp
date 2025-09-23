package ca.bc.gov.nrs.vdyp.batch.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

import ca.bc.gov.nrs.vdyp.batch.model.BatchRecord;
import ca.bc.gov.nrs.vdyp.batch.model.Layer;
import ca.bc.gov.nrs.vdyp.batch.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvLayerRecordBean;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonRecordBean;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;

@Service
public class VdypProjectionService {

	private static final Logger logger = LoggerFactory.getLogger(VdypProjectionService.class);


	@Value("${batch.output.directory.default-path}")
	private String outputBasePath;

	static {
		PolygonProjectionRunner.initializeSiteIndexCurves();
	}

	public VdypProjectionService() {
		// Default constructor - no initialization required
	}

	/**
	 * Creates input streams from a complete BatchRecord containing polygon + all
	 * layers.
	 *
	 * This method converts the complete polygon data from BatchRecord into the HCSV
	 * format input streams required by the VDYP extended-core projection engine.
	 *
	 * @param batchRecord Complete BatchRecord with polygon + all layers
	 * @return Map of input streams for VDYP projection
	 * @throws IOException if stream creation fails
	 */
	private Map<String, InputStream> createInputStreamsFromBatchRecord(BatchRecord batchRecord) {
		Map<String, InputStream> inputStreams = new HashMap<>();

		// Create polygon CSV content
		StringBuilder polygonCsv = new StringBuilder();
		polygonCsv.append(getPolygonCsvHeader()).append("\n");
		polygonCsv.append(polygonDataToCsvLine(batchRecord.getPolygon())).append("\n");

		// Create layer CSV content
		StringBuilder layerCsv = new StringBuilder();
		layerCsv.append(getLayerCsvHeader()).append("\n");
		if (batchRecord.getLayers() != null) {
			for (var layerData : batchRecord.getLayers()) {
				layerCsv.append(layerDataToCsvLine(layerData)).append("\n");
			}
		}

		// Convert to input streams
		inputStreams.put(
				ParameterNames.HCSV_POLYGON_INPUT_DATA, new ByteArrayInputStream(polygonCsv.toString().getBytes()));
		inputStreams
				.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, new ByteArrayInputStream(layerCsv.toString().getBytes()));

		logger.debug(
				"Created input streams for FEATURE_ID {}: polygon (1 record), layers ({} records)",
				batchRecord.getFeatureId(), batchRecord.getLayers() != null ? batchRecord.getLayers().size() : 0);

		return inputStreams;
	}

	/**
	 * Get polygon CSV header for HCSV format.
	 */
	private String getPolygonCsvHeader() {
		return extractCsvHeaderFromRecordBean(HcsvPolygonRecordBean.class);
	}

	/**
	 * Get layer CSV header for HCSV format.
	 */
	private String getLayerCsvHeader() {
		return extractCsvHeaderFromRecordBean(HcsvLayerRecordBean.class);
	}

	/**
	 * Extract CSV header from record bean class by reading @CsvBindByName
	 * annotations.
	 */
	private String extractCsvHeaderFromRecordBean(Class<?> recordBeanClass) {
		return Arrays.stream(recordBeanClass.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(CsvBindByName.class))
				.sorted((f1, f2) -> {
					var pos1 = f1.getAnnotation(CsvBindByPosition.class);
					var pos2 = f2.getAnnotation(CsvBindByPosition.class);
					if (pos1 != null && pos2 != null) {
						return Integer.compare(pos1.position(), pos2.position());
					}
					return f1.getName().compareTo(f2.getName());
				})
				.map(field -> field.getAnnotation(CsvBindByName.class).column())
				.collect(Collectors.joining(","));
	}

	/**
	 * Convert polygon data to CSV line.
	 */
	private String polygonDataToCsvLine(Polygon polygonData) {
		return String.join(
				",", nvl(polygonData.getFeatureId()), nvl(polygonData.getMapId()), nvl(polygonData.getPolygonNumber()),
				nvl(polygonData.getOrgUnit()), nvl(polygonData.getTsaName()), nvl(polygonData.getTflName()),
				nvl(polygonData.getInventoryStandardCode()), nvl(polygonData.getTsaNumber()),
				nvl(polygonData.getShrubHeight()), nvl(polygonData.getShrubCrownClosure()),
				nvl(polygonData.getShrubCoverPattern()), nvl(polygonData.getHerbCoverTypeCode()),
				nvl(polygonData.getHerbCoverPct()), nvl(polygonData.getHerbCoverPatternCode()),
				nvl(polygonData.getBryoidCoverPct()), nvl(polygonData.getBecZoneCode()),
				nvl(polygonData.getCfsEcozone()), nvl(polygonData.getPreDisturbanceStockability()),
				nvl(polygonData.getYieldFactor()), nvl(polygonData.getNonProductiveDescriptorCd()),
				nvl(polygonData.getBclcsLevel1Code()), nvl(polygonData.getBclcsLevel2Code()),
				nvl(polygonData.getBclcsLevel3Code()), nvl(polygonData.getBclcsLevel4Code()),
				nvl(polygonData.getBclcsLevel5Code()), nvl(polygonData.getPhotoEstimationBaseYear()),
				nvl(polygonData.getReferenceYear()), nvl(polygonData.getPctDead()),
				nvl(polygonData.getNonVegCoverType1()), nvl(polygonData.getNonVegCoverPct1()),
				nvl(polygonData.getNonVegCoverPattern1()), nvl(polygonData.getNonVegCoverType2()),
				nvl(polygonData.getNonVegCoverPct2()), nvl(polygonData.getNonVegCoverPattern2()),
				nvl(polygonData.getNonVegCoverType3()), nvl(polygonData.getNonVegCoverPct3()),
				nvl(polygonData.getNonVegCoverPattern3()), nvl(polygonData.getLandCoverClassCd1()),
				nvl(polygonData.getLandCoverPct1()), nvl(polygonData.getLandCoverClassCd2()),
				nvl(polygonData.getLandCoverPct2()), nvl(polygonData.getLandCoverClassCd3()),
				nvl(polygonData.getLandCoverPct3()));
	}

	/**
	 * Convert layer data to CSV line.
	 */
	private String layerDataToCsvLine(Layer layerData) {
		return String.join(
				",", nvl(layerData.getFeatureId()), nvl(layerData.getTreeCoverLayerEstimatedId()),
				nvl(layerData.getMapId()), nvl(layerData.getPolygonNumber()), nvl(layerData.getLayerLevelCode()),
				nvl(layerData.getVdyp7LayerCd()), nvl(layerData.getLayerStockability()),
				nvl(layerData.getForestCoverRankCode()), nvl(layerData.getNonForestDescriptorCode()),
				nvl(layerData.getEstSiteIndexSpeciesCd()), nvl(layerData.getEstimatedSiteIndex()),
				nvl(layerData.getCrownClosure()), nvl(layerData.getBasalArea75()), nvl(layerData.getStemsPerHa75()),
				nvl(layerData.getSpeciesCd1()), nvl(layerData.getSpeciesPct1()), nvl(layerData.getSpeciesCd2()),
				nvl(layerData.getSpeciesPct2()), nvl(layerData.getSpeciesCd3()), nvl(layerData.getSpeciesPct3()),
				nvl(layerData.getSpeciesCd4()), nvl(layerData.getSpeciesPct4()), nvl(layerData.getSpeciesCd5()),
				nvl(layerData.getSpeciesPct5()), nvl(layerData.getSpeciesCd6()), nvl(layerData.getSpeciesPct6()),
				nvl(layerData.getEstAgeSpp1()), nvl(layerData.getEstHeightSpp1()), nvl(layerData.getEstAgeSpp2()),
				nvl(layerData.getEstHeightSpp2()), nvl(layerData.getAdjInd()), nvl(layerData.getLoreyHeight75()),
				nvl(layerData.getBasalArea125()), nvl(layerData.getWsVolPerHa75()), nvl(layerData.getWsVolPerHa125()),
				nvl(layerData.getCuVolPerHa125()), nvl(layerData.getDVolPerHa125()), nvl(layerData.getDwVolPerHa125()));
	}

	/**
	 * Null-safe string conversion for CSV output.
	 */
	private String nvl(Object value) {
		return value != null ? value.toString() : "";
	}

	/**
	 * Generates a unique projection ID for a specific partition and FEATURE_ID
	 */
	private String buildProjectionId(String partitionName, String featureId) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSSS");
		return String.format(
				"batch-projection-%s-feature-%s-%s", partitionName, featureId, formatter.format(LocalDateTime.now()));
	}

	/**
	 * Performs VDYP projection for a complete BatchRecord containing one polygon +
	 * all its layers.
	 *
	 * This method implements the FEATURE_ID-based processing strategy where each
	 * BatchRecord contains a complete
	 * polygon entity (1 polygon + all associated layers).
	 *
	 * @param batchRecord   Complete BatchRecord with polygon + all layers for one
	 *                      FEATURE_ID
	 * @param partitionName Partition identifier for logging and output organization
	 * @param parameters    VDYP projection parameters
	 * @return Projection result summary
	 * @throws Exception if projection fails
	 */
	public String performProjectionForRecord(BatchRecord batchRecord, String partitionName, Parameters parameters)
			throws IOException {
		logger.debug(
				"Starting VDYP projection for complete polygon FEATURE_ID {} in partition {}",
				batchRecord.getFeatureId(),
				partitionName);

		try {
			// Create partition-specific output directory
			Path partitionOutputDir = createPartitionOutputDir(partitionName);

			// Create input streams from the complete BatchRecord data
			Map<String, InputStream> inputStreams = createInputStreamsFromBatchRecord(batchRecord);

			String projectionId = buildProjectionId(partitionName, batchRecord.getFeatureId());

			try (
					ProjectionRunner runner = new ProjectionRunner(
							ProjectionRequestKind.HCSV, projectionId, parameters, false)) {

				logger.debug(
						"Running HCSV projection {} for complete polygon FEATURE_ID {}", projectionId,
						batchRecord.getFeatureId());

				// Run the projection on the complete polygon data
				runner.run(inputStreams);

				// Store intermediate results (no ZIP generation)
				storeIntermediateResults(runner, partitionOutputDir, projectionId, batchRecord.getFeatureId());

				String result = String.format(
						"Projection completed for FEATURE_ID %s in partition %s. Polygon: 1, Layers: %d, Results stored",
						batchRecord.getFeatureId(), partitionName,
						batchRecord.getLayers() != null ? batchRecord.getLayers().size() : 0);

				logger.debug(
						"VDYP projection completed for FEATURE_ID {} in partition {}. Intermediate results stored",
						batchRecord.getFeatureId(), partitionName);

				return result;

			} finally {
				// Close input streams
				for (var entry : inputStreams.entrySet()) {
					Utils.close(entry.getValue(), entry.getKey());
				}
			}

		} catch (AbstractProjectionRequestException e) {
			throw handleProjectionFailure(batchRecord, partitionName, e);
		}
	}

	/**
	 * Handles VDYP projection failures by logging with context and creating
	 * IOException.
	 *
	 * @param batchRecord   The batch record being processed
	 * @param partitionName The partition name being processed
	 * @param cause         The original exception that caused the failure
	 * @return IOException with context for retry logic
	 */
	private IOException handleProjectionFailure(BatchRecord batchRecord, String partitionName, Exception cause) {
		String contextualMessage = String.format(
				"VDYP projection failed for FEATURE_ID %s in partition %s (Map: %s, Polygon: %s). Exception type: %s, Root cause: %s",
				batchRecord.getFeatureId(),
				partitionName,
				batchRecord.getPolygon() != null ? batchRecord.getPolygon().getMapId() : "N/A",
				batchRecord.getPolygon() != null ? batchRecord.getPolygon().getPolygonNumber() : "N/A",
				cause.getClass().getSimpleName(),
				cause.getMessage() != null ? cause.getMessage() : "No error message available");

		logger.error(contextualMessage, cause);

		return new IOException(contextualMessage, cause);
	}

	/**
	 * Creates a partition-specific output directory
	 */
	private Path createPartitionOutputDir(String partitionName) throws IOException {
		Path baseOutputDir = Paths.get(outputBasePath);
		// partitionName already contains "partition" prefix from DynamicPartitioner
		Path partitionDir = baseOutputDir.resolve(partitionName);
		Files.createDirectories(partitionDir);
		return partitionDir;
	}

	/**
	 * Stores intermediate projection results for later aggregation. This method
	 * saves all projection outputs (yield
	 * tables, logs) directly to partition directories for simple structure.
	 *
	 * @param runner             ProjectionRunner containing the results
	 * @param partitionOutputDir Directory for this partition's results
	 * @param projectionId       Unique identifier for this projection
	 * @param featureId          The FEATURE_ID being processed
	 * @throws IOException if storing results fails
	 */
	private void storeIntermediateResults(
			ProjectionRunner runner, Path partitionOutputDir, String projectionId, String featureId)
			throws IOException {

		logger.debug("Storing intermediate results for projection {} (FEATURE_ID: {})", projectionId, featureId);

		// Store files directly in partition directory
		// Store yield tables
		storeYieldTables(runner, partitionOutputDir, featureId);

		// Store logs if enabled
		storeLogs(runner, partitionOutputDir, featureId);

		logger.debug(
				"Successfully stored intermediate results for projection {} (FEATURE_ID: {}) in {}", projectionId,
				featureId, partitionOutputDir);
	}

	/**
	 * Stores yield tables from the projection runner directly in partition
	 * directory.
	 */
	private void storeYieldTables(ProjectionRunner runner, Path partitionDir, String featureId) throws IOException {
		for (YieldTable yieldTable : runner.getContext().getYieldTables()) {
			String yieldTableFileName = yieldTable.getOutputFormat().getYieldTableFileName();
			// Add FEATURE_ID prefix to maintain traceability
			String prefixedFileName = String.format("YieldTables_FEATURE_%s_%s", featureId, yieldTableFileName);
			Path yieldTablePath = partitionDir.resolve(prefixedFileName);

			try (InputStream yieldTableStream = yieldTable.getAsStream()) {
				Files.copy(yieldTableStream, yieldTablePath, StandardCopyOption.REPLACE_EXISTING);
				logger.trace("Stored yield table: {} for FEATURE_ID: {}", prefixedFileName, featureId);
			}
		}
	}

	/**
	 * Stores log files from the projection runner directly in partition directory
	 * if logging is enabled.
	 */
	private void storeLogs(ProjectionRunner runner, Path partitionDir, String featureId) throws IOException {
		// Store progress log if enabled
		if (runner.getContext().getParams().containsOption(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING)) {
			String progressLogFileName = String.format("YieldTables_FEATURE_%s_ProgressLog.txt", featureId);
			Path progressLogPath = partitionDir.resolve(progressLogFileName);

			try (InputStream progressStream = runner.getProgressStream()) {
				Files.copy(progressStream, progressLogPath, StandardCopyOption.REPLACE_EXISTING);
				logger.trace("Stored progress log: {} for FEATURE_ID: {}", progressLogFileName, featureId);
			}
		}

		// Store error log if enabled
		if (runner.getContext().getParams().containsOption(ExecutionOption.DO_ENABLE_ERROR_LOGGING)) {
			String errorLogFileName = String.format("YieldTables_FEATURE_%s_ErrorLog.txt", featureId);
			Path errorLogPath = partitionDir.resolve(errorLogFileName);

			try (InputStream errorStream = runner.getErrorStream()) {
				Files.copy(errorStream, errorLogPath, StandardCopyOption.REPLACE_EXISTING);
				logger.trace("Stored error log: {} for FEATURE_ID: {}", errorLogFileName, featureId);
			}
		}

		// Store debug log if enabled
		if (runner.getContext().getParams().containsOption(ExecutionOption.DO_ENABLE_DEBUG_LOGGING)) {
			String debugLogFileName = String.format("YieldTables_FEATURE_%s_DebugLog.txt", featureId);
			Path debugLogPath = partitionDir.resolve(debugLogFileName);

			// Debug log would typically be from file system; for now create empty
			// placeholder
			Files.write(debugLogPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			logger.trace("Created debug log placeholder: {} for FEATURE_ID: {}", debugLogFileName, featureId);
		}
	}
}
