package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

public class TextYieldTableWriter implements YieldTableWriter {

	public static final String YIELD_TABLE_FILE_NAME = "Output_YldTbl.txt";

	private final OutputStream outputStream;
	private final ProjectionContext context;

	private TextYieldTableWriter(ProjectionContext context, OutputStream outputStream) {
		this.context = context;
		this.outputStream = outputStream;
	}

	public static TextYieldTableWriter of(ProjectionContext context) throws YieldTableGenerationException {

		Path outputFilePath = Path.of(context.getExecutionFolder().toString(), YIELD_TABLE_FILE_NAME);

		TextYieldTableWriter writer;
		try {
			outputFilePath = Files.createFile(outputFilePath);
			OutputStream os = Files.newOutputStream(outputFilePath);
			writer = new TextYieldTableWriter(context, os);
		} catch (IOException e) {
			throw new YieldTableGenerationException(e);
		}

		return writer;
	}

	@Override
	public void close() throws IOException {
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				throw new IllegalStateException("Unable to close FileYieldTableOutputWriter outputStream");
			}
		}
	}

	@Override
	public void writeHeader(
			Polygon polygon, LayerReportingInfo layerReportingInfo, boolean doGenerateDetailedTableHeader,
			Integer yieldTableCount
	) throws YieldTableGenerationException {

		var params = context.getValidatedParams();

		write("vvvvvvvvvv Table Number: %-10d", yieldTableCount);

		if (doGenerateDetailedTableHeader) {
			String layerId;
			String processingMode;

			if (layerReportingInfo == null) {
				layerId = "N/A";
				processingMode = "Summary";
			} else {
				layerId = layerReportingInfo.getLayer().getLayerId();
				processingMode = layerReportingInfo.getProcessedAsVDYP7Layer().name;
			}

			var reportingInfo = polygon.getReportingInfo();

			write(
					" District: %-3s  Map Name: %-7s Polygon: %-9d Layer: %s - %s", reportingInfo.getDistrict(),
					reportingInfo.getMapSheet(), reportingInfo.getPolygonNumber(), layerId, processingMode
			);

			if (params.containsOption(ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE)) {

				write("   (Rcrd ID: %d)", reportingInfo.getFeatureId());
			}

			write("\n");

			if (params.containsOption(ExecutionOption.DO_INCLUDE_COLUMN_HEADERS_IN_YIELD_TABLE)) {
				write(
						"Year  Age                      Stand Composition                      %% Stk   SI   D Hgt  %sL Hgt   Dia    TPH       BA      Vws    Vcu    Vd     Vdw   Vdwb  %s\n",
						(params.containsOption(
								ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
						) ? "S Hgt  " : ""),
						(params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE) ? "Mode" : "")
				);

				write(
						"---- ---- ----------------------------------------------------------- ----- ------ ------ %s------ ----- -------- -------- ------ ------ ------ ------ ------ %s\n",
						(params.containsOption(
								ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE
						) ? "------ " : ""),
						(params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE) ? "----" : "")
				);
			}
		}
	}

	private void write(String message, Object... args) throws YieldTableGenerationException {

		try {
			outputStream.write(String.format(message, args).getBytes());
		} catch (IOException e) {
			throw new YieldTableGenerationException(e);
		}
	}

	@Override
	public void writeTrailer(Integer yieldTableCount) throws YieldTableGenerationException {
		write("^^^^^^^^^^ Table Number: %-10d", yieldTableCount);
	}

	@Override
	public void writeCalendarYearAndLayerAge(YieldTableData row) throws YieldTableGenerationException {
		write("%4d %4d ", Integer.toString(row.getCurrentTableYear()), Integer.toString(row.getCurrentTableAge()));
	}

	@Override
	public void startNewRecord() throws YieldTableGenerationException {
		write("\\n");
	}

	@Override
	public void writeSpeciesComposition(YieldTableData row) throws YieldTableGenerationException {
		var isPolygonTable = row.getLayerReportingInfo() != null;

		if (isPolygonTable) {
			write("%*s%3s%*s", (60 - 3) / 2, "", "N/A", 60 - ( ( (60 - 3) / 2) + 3), "");
		} else {
			for (var details : row.getSortedSpeciesArray()) {
				write("%-3s %5.1f ", details, details);
			}

			for (int i = row.getSortedSpeciesArray().size(); i < 6; i++) {
				write("%-3s %5.1f ", "", 0.0);
			}
		}
	}
}
