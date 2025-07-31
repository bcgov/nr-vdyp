package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTableRowBean.MultiFieldPrefixes;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTableRowBean.MultiFieldSuffixes;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;

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
	protected void writeRecord(YieldTableRowContext rowContext) throws YieldTableGenerationException {

		writeCalendarYearAndLayerAge();
		writeSpeciesComposition(rowContext);
		if (isCurrentlyWritingCategory(YieldTable.Category.CFSBIOMASS)) {
			writeCFSBiomassInfo();
		} else {
			writeProjectionGrowthInfo();
		}
		writeRecordMode();
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

			var reportingInfo = polygon.getReportingInfo();

			doWrite(
					" District: %-3s  Map Name: %-7s Polygon: %-9d Layer: %s - %s", reportingInfo.getDistrict(),
					reportingInfo.getMapSheet(), reportingInfo.getPolygonNumber(), layerId, processingMode
			);

			if (isCurrentlyWritingCategory(YieldTable.Category.POLYGON_ID)) {

				doWrite("   (Rcrd ID: %d)", reportingInfo.getFeatureId());
			}

			doWrite("\n");
		}
		if (params.containsOption(ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE)) {

			String projectionMode = "";
			String projectionModeLine = "";
			if (isCurrentlyWritingCategory(YieldTable.Category.PROJECTION_MODE)) {
				projectionMode = "Mode";
				projectionModeLine = "----";
			}

			if (isCurrentlyWritingCategory(YieldTable.Category.CFSBIOMASS)) {
				doWrite(
						"Year  Age                      Stand Composition                       Vcu    Bstem   Bbark  Bbranch   Bfol  %s\n",
						projectionMode
				);

				doWrite(
						"---- ---- ----------------------------------------------------------- ------ ------- ------- ------- ------- %s\n",
						projectionModeLine
				);
			} else {
				doWrite(
						"Year  Age                      Stand Composition                      %% Stk   SI   D Hgt  %sL Hgt   Dia    TPH       BA      Vws    Vcu    Vd     Vdw   Vdwb  %s\n",
						(params.containsOption(
								ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
						) ? "S Hgt  " : ""), projectionMode
				);

				doWrite(
						"---- ---- ----------------------------------------------------------- ----- ------ ------ %s------ ----- -------- -------- ------ ------ ------ ------ ------ %s\n",
						(params.containsOption(
								ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
						) ? "------ " : ""), projectionModeLine
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

	private void writeSpeciesComposition(YieldTableRowContext rowContext) throws YieldTableGenerationException {

		if (rowContext.isPolygonTable()) {
			doWrite("%28s%3s%29s", "", "N/A", "");
		} else {
			int speciesIndex = 1;

			String code = currentRecord
					.getSpeciesFieldValue(MultiFieldPrefixes.Species, speciesIndex, MultiFieldSuffixes.Code);
			while (code != null) {
				String percentage = currentRecord
						.getSpeciesFieldValue(MultiFieldPrefixes.Species, speciesIndex, MultiFieldSuffixes.Percent);
				if (percentage != null) {
					doWrite("%-3s %5.1f ", code, Double.parseDouble(percentage));
				} else {
					doWrite("%-3s       ", code);
				}
				speciesIndex += 1;
				code = currentRecord
						.getSpeciesFieldValue(MultiFieldPrefixes.Species, speciesIndex, MultiFieldSuffixes.Code);
			}

			if (speciesIndex == 1) {
				doWrite("%28s%3s%29s", "", "N/A", "");
			} else {
				while (speciesIndex++ <= 6) {
					doWrite("%-3s %5.1f ", "", 0.0);
				}
			}
		}
	}

	protected void writeProjectionGrowthInfo() throws YieldTableGenerationException {
		if (currentRecord.getPercentStockable() != null)
			doWrite("%5.1f ", Double.parseDouble(currentRecord.getPercentStockable()));
		else
			doWrite("%5s ", " ");

		if (currentRecord.getSiteIndex() != null)
			doWrite("%6.2f ", Double.parseDouble(currentRecord.getSiteIndex()));
		else
			doWrite("%6s ", " ");

		if (currentRecord.getDominantHeight() != null)
			doWrite("%6.2f ", Double.parseDouble(currentRecord.getDominantHeight()));
		else
			doWrite("%6s ", " ");

		if (context.getParams()
				.containsOption(ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE)) {
			if (currentRecord.getSecondaryHeight() != null)
				doWrite("%6.2f ", Double.parseDouble(currentRecord.getSecondaryHeight()));
			else
				doWrite("%6s ", " ");
		}

		if (currentRecord.getLoreyHeight() != null)
			doWrite("%6.2f ", Double.parseDouble(currentRecord.getLoreyHeight()));
		else
			doWrite("%6s ", " ");

		if (currentRecord.getDiameter() != null)
			doWrite("%5.1f ", Double.parseDouble(currentRecord.getDiameter()));
		else
			doWrite("%5s ", " ");

		if (currentRecord.getTreesPerHectare() != null)
			doWrite("%8.2f ", Double.parseDouble(currentRecord.getTreesPerHectare()));
		else
			doWrite("%8s ", " ");

		if (currentRecord.getBasalArea() != null)
			doWrite("%8.4f ", Double.parseDouble(currentRecord.getBasalArea()));
		else
			doWrite("%8s ", " ");

		if (currentRecord.getWholeStemVolume() != null)
			doWrite("%6.1f ", Double.parseDouble(currentRecord.getWholeStemVolume()));
		else
			doWrite("%6s ", " ");

		if (currentRecord.getCloseUtilizationVolume() != null)
			doWrite("%6.1f ", Double.parseDouble(currentRecord.getCloseUtilizationVolume()));
		else
			doWrite("%6s ", " ");

		if (currentRecord.getCuVolumeLessDecay() != null)
			doWrite("%6.1f ", Double.parseDouble(currentRecord.getCuVolumeLessDecay()));
		else
			doWrite("%6s ", " ");

		if (currentRecord.getCuVolumeLessDecayWastage() != null)
			doWrite("%6.1f ", Double.parseDouble(currentRecord.getCuVolumeLessDecayWastage()));
		else
			doWrite("%6s ", " ");

		if (currentRecord.getCuVolumeLessDecayWastageBreakage() != null)
			doWrite("%6.1f ", Double.parseDouble(currentRecord.getCuVolumeLessDecayWastageBreakage()));
		else
			doWrite("%6s ", " ");

	}

	protected void writeCFSBiomassInfo() throws YieldTableGenerationException {
		if (currentRecord.getCloseUtilizationVolume() != null)
			doWrite("%6.1f ", Double.parseDouble(currentRecord.getCloseUtilizationVolume()));
		else
			doWrite("%6s ", " ");

		if (currentRecord.getCfsBiomassStem() != null)
			doWrite("%7.2f ", Double.parseDouble(currentRecord.getCfsBiomassStem()));
		else
			doWrite("%7s ", " ");

		if (currentRecord.getCfsBiomassBark() != null)
			doWrite("%7.2f ", Double.parseDouble(currentRecord.getCfsBiomassBark()));
		else
			doWrite("%7s ", " ");

		if (currentRecord.getCfsBiomassBranch() != null)
			doWrite("%7.2f ", Double.parseDouble(currentRecord.getCfsBiomassBranch()));
		else
			doWrite("%7s ", " ");

		if (currentRecord.getCfsBiomassFoliage() != null)
			doWrite("%7.2f ", Double.parseDouble(currentRecord.getCfsBiomassFoliage()));
		else
			doWrite("%7s ", " ");

	}

	protected void writeRecordMode() throws YieldTableGenerationException {
		if (isCurrentlyWritingCategory(YieldTable.Category.PROJECTION_MODE)) {
			if (currentRecord.getMode() != null)
				doWrite("%5s ", currentRecord.getMode());
			else
				doWrite("      ");
		}
	}

	@Override
	public void writePolygonTableTrailer(Integer yieldTableCount, Polygon polygon)
			throws YieldTableGenerationException {
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
