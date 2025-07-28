package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;

class FullReportYieldTableWriter extends YieldTableWriter<TextYieldTableRowValuesBean> {

	public static final String YIELD_TABLE_FILE_NAME = "YieldReport.txt";

	private final ProjectionContext context;

	private OutputStream outputStream;
	private TextFileTable textFileTable;

	private class TextFileTable {
		private record Column(String[] headerWords, int width, String format, boolean included, String superHeader) {
		}

		int longestHeaderLength = 0;
		List<Column> columns = new ArrayList();
		List<Object> currentRow;

		public void addColumn(String header, int width) {
			addColumn(header, width, "%" + width + "s");
		}

		public void addColumn(String header, int width, String format) {
			addColumn(header, width, format, true);
		}

		public void addColumn(String header, int width, String format, boolean included) {
			addColumn(header, width, format, included, null);
		}

		public void addColumn(String header, int width, String format, boolean included, String superHeader) {
			Column column = new Column(
					header.split(" "), width, format.equals("|") ? format : format + " ", included, superHeader
			);
			if (column.headerWords.length > longestHeaderLength)
				longestHeaderLength = column.headerWords.length;

			columns.add(column);
		}

		public void startNewRow() {
			currentRow = new ArrayList<>();
		}

		public void addCell(Object value) {
			if (currentRow == null) {
				throw new IllegalStateException("Cannot add cell data before starting a new row");
			}
			int colIndex = currentRow.size();
			if (colIndex >= columns.size()) {
				throw new IllegalStateException(
						"Cannot add cell data for column index " + colIndex
								+ " as it exceeds the number of defined columns: " + columns.size()
				);
			}
			Column column = columns.get(colIndex);
			if (!column.included() || column.headerWords()[0].equals("|")) {
				currentRow.add(null);
				if (column.headerWords()[0].equals("|"))
					addCell(value); // if this is a break column, we add the value to the next column
			} else {
				currentRow.add(value);
			}
		}

		public void printHeader() throws YieldTableGenerationException {
			StringBuilder sb;
			int lineLength = 0;
			String currentSuperHeader = null;
			int numColumns = columns.size();
			for (int endOffset = 1; endOffset <= longestHeaderLength; endOffset++) {
				sb = new StringBuilder();
				for (int colIndex = 0; colIndex < numColumns; colIndex++) {
					Column column = columns.get(colIndex);
					if (column.included()) {
						String[] headerWords = column.headerWords();
						int width = column.width();
						boolean isBreak = headerWords[0].equals("|");
						int useIndex = isBreak ? 0 : headerWords.length - endOffset;
						String headerRowWord;
						// Super header logic NOTE assumes that super headers occur at the same depth for all super
						// header columns
						if (useIndex == -1 && column.superHeader() != null) {
							if (column.superHeader().equals(currentSuperHeader)) {
								// we have accounted for this super header already skip this column at this depth
								continue;
							} else {
								currentSuperHeader = column.superHeader();
								// read ahead for the total width
								for (int ahead = colIndex + 1; ahead < numColumns; ahead++) {
									if (!currentSuperHeader.equals(columns.get(ahead).superHeader())) {
										break;
									}
									width += columns.get(ahead).width() + 1; // +1 for the space
								}
							}
							headerRowWord = column.superHeader();
						} else {
							headerRowWord = useIndex >= 0 ? headerWords[useIndex] : " ".repeat(width);

						}
						int padChars = width - headerRowWord.length();
						if (padChars > 0) {
							int leftPadChars = padChars / 2;
							int rightPadChars = padChars - leftPadChars;
							headerRowWord = " ".repeat(leftPadChars) + headerRowWord + " ".repeat(rightPadChars);
						}
						sb.append(headerRowWord.replaceAll("_", " "));
						if (!isBreak) {
							sb.append(" ");
						}
					}
				}
				lineLength = sb.length();
				doWrite(sb + "\n");
			}
			doWrite("-".repeat(lineLength) + "\n");

		}

		public void printRow() throws YieldTableGenerationException {
			int numColumns = columns.size();
			for (int j = 0; j < numColumns; j++) {
				Column column = columns.get(j);
				if (column.included()) {
					Object cellData = currentRow.get(j);
					doWrite(column.format(), cellData);
				}
			}
			doWrite("\n");
		}
	}

	private FullReportYieldTableWriter(ProjectionContext context, OutputStream outputStream, Path yieldTableFilePath) {
		super(TextYieldTableRowValuesBean.class, yieldTableFilePath);

		this.context = context;
		this.outputStream = outputStream;
		this.textFileTable = new TextFileTable();
	}

	public static FullReportYieldTableWriter of(ProjectionContext context) throws YieldTableGenerationException {

		Path yieldTableFilePath = Path.of(context.getExecutionFolder().toString(), YIELD_TABLE_FILE_NAME);

		FullReportYieldTableWriter writer;
		try {
			yieldTableFilePath = Files.createFile(yieldTableFilePath);
			OutputStream os = Files.newOutputStream(yieldTableFilePath);
			writer = new FullReportYieldTableWriter(context, os, yieldTableFilePath);
		} catch (IOException e) {
			throw new YieldTableGenerationException(e);
		}

		return writer;
	}

	@Override
	public void startNewRecord() {
		super.startNewRecord();
		textFileTable.startNewRow();
	}

	@Override
	protected void writeRecord(YieldTableRowContext rowContext) throws YieldTableGenerationException {

		textFileTable.addCell(Integer.parseInt(currentRecord.getTotalAge())); // TOT AGE
		textFileTable.addCell(Double.parseDouble(currentRecord.getDominantHeight())); // Height
		textFileTable.addCell(Double.parseDouble(currentRecord.getSecondaryHeight())); // Secondary Height
		textFileTable.addCell(Double.parseDouble(currentRecord.getLoreyHeight())); /// lorey Height
		textFileTable.addCell(Double.parseDouble(currentRecord.getDiameter()));// "Quad Stnd DIA (cm)";
		textFileTable.addCell(Double.parseDouble(currentRecord.getBasalArea()));// "BA (m**2/ha)");//Basal Area
		textFileTable.addCell(Integer.parseInt(currentRecord.getTreesPerHectare()));// TPH

		textFileTable.addCell(Double.parseDouble(currentRecord.getWholeStemVolume())); // whole stem volume
		textFileTable.addCell(Double.parseDouble(currentRecord.getWholeStemVolume())); // FIX ME whole Stem Volume MAI

		textFileTable.addCell(Double.parseDouble(currentRecord.getCloseUtilizationVolume())); // close util volume
		textFileTable.addCell(Double.parseDouble(currentRecord.getCloseUtilizationVolume())); // FIX ME close util MAI

		textFileTable.addCell(Double.parseDouble(currentRecord.getCuVolumeLessDecay())); // Net decay valume
		textFileTable.addCell(Double.parseDouble(currentRecord.getCuVolumeLessDecay())); // FIX ME Net decay valume MAI

		textFileTable.addCell(Double.parseDouble(currentRecord.getCuVolumeLessDecayWastage())); // Net decay wastage
																								// volume
		textFileTable.addCell(Double.parseDouble(currentRecord.getCuVolumeLessDecayWastage())); // FIX ME Net decay
																								// wastage MAI

		textFileTable.addCell(Double.parseDouble(currentRecord.getCuVolumeLessDecayWastageBreakage())); // close util
																										// less decay
																										// wastage and
																										// breakage
																										// volume
		textFileTable.addCell(Double.parseDouble(currentRecord.getCuVolumeLessDecayWastageBreakage())); // FIX ME close
																										// util less
																										// decay wastage
																										// and breakage
																										// MAI

		textFileTable.addCell(Double.parseDouble(currentRecord.getCfsBiomassStem())); // CFS Stem Biomass
		textFileTable.addCell(Double.parseDouble(currentRecord.getCfsBiomassBark())); // CFS Bark Biomass
		textFileTable.addCell(Double.parseDouble(currentRecord.getCfsBiomassBranch())); // CFS Branch Biomass
		textFileTable.addCell(Double.parseDouble(currentRecord.getCfsBiomassFoliage())); // CFS Foliage Biomass

		/*
		 * boolean includeSpeciesComposition = false; for(int specIndex = 0; specIndex < 7; specIndex++) {
		 * textFileTable.addColumn("Sp" + (specIndex + 1) + " Code", 3, includeSpeciesComposition); }
		 */

		// Print out all the relevant information
		textFileTable.printRow();
	}

	@Override
	public void writePolygonTableHeader(
			Polygon polygon, Optional<LayerReportingInfo> layerReportingInfo, boolean doGenerateDetailedTableHeader,
			Integer yieldTableCount
	) throws YieldTableGenerationException {

		var params = context.getParams();
		textFileTable.addColumn("TOT AGE", 3, "%3d");
		textFileTable.addColumn("Site HT (m)", 4, "%3.1f");
		textFileTable.addColumn(
				"Scnd HT (m)", 4, "%3.1f",
				params.containsOption(ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE)
		);
		textFileTable.addColumn("Lorey HT (m)", 5, "%4.1f");
		textFileTable.addColumn("Quad Stnd DIA (cm)", 4, "%3.1f");
		textFileTable.addColumn("BA (m**2/ha)", 9, "%8.1f");
		textFileTable.addColumn("TPH (trees/ha)", 10, "%10d");

		boolean showBioMassColumns = isCurrentlyWritingCategory(YieldTable.Category.CFSBIOMASS);
		boolean showMofVolumeColumns = isCurrentlyWritingCategory(YieldTable.Category.LAYER_MOFVOLUMES);
		boolean includeMAI = false;
		textFileTable.addColumn("|", 1, "|", showMofVolumeColumns);
		textFileTable.addColumn(
				(includeMAI ? "" : "Whole Stem ") + "VOLUME (m**3/ha)", 11, "%10.1f", showMofVolumeColumns,
				includeMAI ? "Whole Stem" : null
		);
		textFileTable.addColumn("MAI (m**3/ha)", 11, "%10.1f", showMofVolumeColumns && includeMAI, "Whole Stem");

		boolean showCloseUtilization = showBioMassColumns || showMofVolumeColumns;
		textFileTable.addColumn("|", 1, "|", showCloseUtilization);
		textFileTable.addColumn(
				(includeMAI ? "" : "Close Utilization ") + "VOLUME (m**3/ha)", 11, "%10.1f", showMofVolumeColumns,
				includeMAI ? "Close Utilization" : null
		);
		textFileTable.addColumn("MAI (m**3/ha)", 11, "%10.1f", showMofVolumeColumns && includeMAI, "Close Utilization");

		textFileTable.addColumn("|", 1, "|", showMofVolumeColumns);
		textFileTable.addColumn(
				(includeMAI ? "" : "Net Decay ") + "VOLUME (m**3/ha)", 11, "%10.1f", showMofVolumeColumns,
				includeMAI ? "Net Decay" : null
		);
		textFileTable.addColumn("MAI (m**3/ha)", 11, "%10.1f", showMofVolumeColumns && includeMAI, "Net Decay");

		textFileTable.addColumn("|", 1, "|", showMofVolumeColumns);
		textFileTable.addColumn(
				(includeMAI ? "" : "Net_Decay and_Waste ") + "VOLUME (m**3/ha)", 11, "%10.1f", showMofVolumeColumns,
				includeMAI ? "Net Decay and Waste" : null
		);
		textFileTable
				.addColumn("MAI (m**3/ha)", 11, "%10.1f", showMofVolumeColumns && includeMAI, "Net Decay and Waste");

		textFileTable.addColumn("|", 1, "|", showMofVolumeColumns);
		textFileTable.addColumn(
				(includeMAI ? "" : "Net_Decay Waste,_Brkg ") + "VOLUME (m**3/ha)", 11, "%10.1f", showMofVolumeColumns,
				includeMAI ? "Net Decay Waste, Brkg" : null
		);
		textFileTable
				.addColumn("MAI (m**3/ha)", 11, "%10.1f", showMofVolumeColumns && includeMAI, "Net Decay Waste, Brkg");

		textFileTable.addColumn("|", 1, "|", showBioMassColumns);
		textFileTable.addColumn("Stem (tons/ha)", 12, "%11.1f", showBioMassColumns, "CFS Biomass");
		textFileTable.addColumn("Bark (tons/ha)", 12, "%11.1f", showBioMassColumns, "CFS Biomass");
		textFileTable.addColumn("Branch (tons/ha)", 12, "%11.1f", showBioMassColumns, "CFS Biomass");
		textFileTable.addColumn("Foliage (tons/ha)", 12, "%11.1f", showBioMassColumns, "CFS Biomass");

		/*
		 * TODO species Composition boolean includeSpeciesComposition = false; textFileTable.addColumn("|", 1, "|",
		 * includeSpeciesComposition); for(int specIndex = 0; specIndex < 7; specIndex++) {
		 * textFileTable.addColumn((specIndex + 1), 3, "%2.1f", includeSpeciesComposition, "Spec Composition"); }
		 */

		// Print out all the relevant information
		textFileTable.printHeader();

	}

	@Override
	public void writePolygonTableTrailer(Integer yieldTableCount) throws YieldTableGenerationException {
		// No data written here
	}

	@Override
	public void writeTrailer() throws YieldTableGenerationException {
		// Write all the required META DATA
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
