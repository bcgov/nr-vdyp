package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowBean.MultiFieldPrefixes;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTableRowBean.MultiFieldSuffixes;
import ca.bc.gov.nrs.vdyp.backend.utils.Utils;

class TextYieldTableWriter extends YieldTableWriter<TextYieldTableRowValuesBean> {

	public static final String YIELD_TABLE_FILE_NAME = "Output_YldTbl.txt";

	private final ProjectionContext context;

	private OutputStream outputStream;

	private TextYieldTableWriter(ProjectionContext context, OutputStream outputStream, Path yieldTableFilePath) {
		super(TextYieldTableRowValuesBean.class, yieldTableFilePath);

		this.context = context;
		this.outputStream = outputStream;
	}

	public static TextYieldTableWriter of(ProjectionContext context) throws YieldTableGenerationException {

		Path yieldTableFilePath = Path.of(context.getExecutionFolder().toString(), YIELD_TABLE_FILE_NAME);

		TextYieldTableWriter writer;
		try {
			yieldTableFilePath = Files.createFile(yieldTableFilePath);
			OutputStream os = Files.newOutputStream(yieldTableFilePath);
			writer = new TextYieldTableWriter(context, os, yieldTableFilePath);
		} catch (IOException e) {
			throw new YieldTableGenerationException(e);
		}

		return writer;
	}

	@Override
	public void startNewRecord() {
		super.startNewRecord();
	}

	@Override
	protected void recordPolygonAndLayerDetails(int yieldTableNumber, YieldTableRowContext rowContext) {
		var polygon = rowContext.getPolygon();

		currentRecord.setDistrict(polygon.getDistrict());
		currentRecord.setFeatureId(polygon.getFeatureId());
		currentRecord.setMapId(polygon.getMapSheet());
		currentRecord.setPolygonId(polygon.getPolygonNumber());
		currentRecord.setTableNumber(yieldTableNumber);

		if (!rowContext.isPolygonTable()) {
			currentRecord.setLayerId(rowContext.getLayerReportingInfo().getLayerID());
		}
	}

	@Override
	public void recordCalendarYearAndLayerAge(YieldTableRowContext rowContext) {
		currentRecord.setProjectionYear(rowContext.getCurrentTableYear());
		currentRecord.setTotalAge(rowContext.getCurrentTableAge());
	}

	@Override
	public void recordSpeciesComposition(YieldTableRowContext rowContext) throws YieldTableGenerationException {

		if (rowContext.getLayerReportingInfo() != null) {
			int speciesIndex = 1;
			for (var details : rowContext.getSortedSpeciesArray()) {
				currentRecord.setSpeciesFieldValue(
						MultiFieldPrefixes.Species, speciesIndex, MultiFieldSuffixes.Code, details.speciesCode()
				);
				currentRecord.setSpeciesFieldValue(
						MultiFieldPrefixes.Species, speciesIndex, MultiFieldSuffixes.Percent, details.speciesPercent()
				);

				speciesIndex += 1;
			}
		}
	}

	@Override
	void recordSiteInformation(
			Double percentStockable, Double siteIndex, Double dominantHeight, Double secondaryHeight
	) {
		currentRecord.setPercentStockable(percentStockable);
		currentRecord.setSiteIndex(siteIndex);
		currentRecord.setDominantHeight(dominantHeight);
		currentRecord.setSecondaryHeight(secondaryHeight == null ? null : secondaryHeight);
	}

	@Override
	void recordGrowthDetails(EntityGrowthDetails growthDetails, EntityVolumeDetails entityVolumeDetails) {
	}

	@Override
	protected void writeRecord() throws YieldTableGenerationException {

		writeCalendarYearAndLayerAge();
		writeSpeciesComposition();
		writeProjectionGrowthInfo();

		doWrite("\n");
	}

	@Override
	public void writePolygonTableHeader(
			Polygon polygon, Optional<LayerReportingInfo> layerReportingInfo, boolean doGenerateDetailedTableHeader,
			Integer yieldTableCount
	) throws YieldTableGenerationException {

		var params = context.getParams();

		doWrite("vvvvvvvvvv Table Number: %-10d", yieldTableCount);

		if (doGenerateDetailedTableHeader) {
			String layerId;
			String processingMode;

			if (layerReportingInfo.isPresent()) {
				layerId = layerReportingInfo.get().getLayer().getLayerId();
				processingMode = layerReportingInfo.get().getProcessedAsVDYP7Layer().name;
			} else {
				layerId = "N/A";
				processingMode = "Summary";
			}
			;

			var reportingInfo = polygon.getReportingInfo();

			doWrite(
					" District: %-3s  Map Name: %-7s Polygon: %-9d Layer: %s - %s", reportingInfo.getDistrict(),
					reportingInfo.getMapSheet(), reportingInfo.getPolygonNumber(), layerId, processingMode
			);

			if (params.containsOption(ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE)) {

				doWrite("   (Rcrd ID: %d)", reportingInfo.getFeatureId());
			}

			doWrite("\n");

			if (params.containsOption(ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE)) {
				doWrite(
						"Year  Age                      Stand Composition                      %% Stk   SI   D Hgt  %sL Hgt   Dia    TPH       BA      Vws    Vcu    Vd     Vdw   Vdwb  %s\n",
						(params.containsOption(
								ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
						) ? "S Hgt  " : ""),
						(params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE) ? "Mode" : "")
				);

				doWrite(
						"---- ---- ----------------------------------------------------------- ----- ------ ------ %s------ ----- -------- -------- ------ ------ ------ ------ ------ %s\n",
						(params.containsOption(
								ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
						) ? "------ " : ""),
						(params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE) ? "----" : "")
				);
			}
		}
	}

	private void writeCalendarYearAndLayerAge() throws YieldTableGenerationException {
		if (currentRecord.getProjectionYear() != null) {
			doWrite("%4d ", Integer.parseInt(currentRecord.getProjectionYear()));
		} else {
			doWrite("     ");
		}

		if (currentRecord.getTotalAge() != null) {
			doWrite("%4d ", Integer.parseInt(currentRecord.getTotalAge()));
		} else {
			doWrite("     ");
		}
	}

	private void writeSpeciesComposition() throws YieldTableGenerationException {

		int speciesIndex = 1;

		String code = currentRecord
				.getSpeciesFieldValue(MultiFieldPrefixes.Species, speciesIndex, MultiFieldSuffixes.Code);
		while (code != null) {
			String percentage = currentRecord
					.getSpeciesFieldValue(MultiFieldPrefixes.Species, speciesIndex, MultiFieldSuffixes.Percent);
			doWrite("%-3s %5.1f ", code, Double.parseDouble(percentage));

			speciesIndex += 1;
			code = currentRecord
					.getSpeciesFieldValue(MultiFieldPrefixes.Species, speciesIndex, MultiFieldSuffixes.Code);
		}

		if (speciesIndex == 1) {
			doWrite("%*s%3s%*s", (60 - 3) / 2, "", "N/A", 60 - ( ( (60 - 3) / 2) + 3), "");
		} else {
			while (speciesIndex++ <= 6) {
				doWrite("%-3s %5.1f ", "", 0.0);
			}
		}
	}

	@Override
	void writeProjectionGrowthInfo() throws YieldTableGenerationException {
		if (currentRecord.getPercentStockable() != null)
			doWrite("%5.1f ", Double.parseDouble(currentRecord.getPercentStockable()));
		else
			doWrite("      ");

		if (currentRecord.getSiteIndex() != null)
			doWrite("%6.2f ", Double.parseDouble(currentRecord.getSiteIndex()));
		else
			doWrite("%6s ", " ");

		if (currentRecord.getDominantHeight() != null)
			doWrite("%6.2f ", Double.parseDouble(currentRecord.getDominantHeight()));
		else
			doWrite("       ");

		if (context.getParams()
				.containsOption(ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE)) {
			if (currentRecord.getSecondaryHeight() != null)
				doWrite("%6.2f ", Double.parseDouble(currentRecord.getSecondaryHeight()));
			else
				doWrite("       ");
		}
	}

	@Override
	public void writePolygonTableTrailer(Integer yieldTableCount) throws YieldTableGenerationException {
		doWrite("^^^^^^^^^^ Table Number: %-10d\n", yieldTableCount);
	}

	@Override
	public void writeTrailer() throws YieldTableGenerationException {
		doWrite("Run completed: %s\n", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
	}

	private void doWrite(String message, Object... args) throws YieldTableGenerationException {

		try {
			outputStream.write(String.format(message, args).getBytes());
		} catch (IOException e) {
			throw new YieldTableGenerationException(e);
		}
	}

	@Override
	public final void close() {
		Utils.close(outputStream, "TextYieldTableWriter.outputStream");
		outputStream = null;
	}
}
