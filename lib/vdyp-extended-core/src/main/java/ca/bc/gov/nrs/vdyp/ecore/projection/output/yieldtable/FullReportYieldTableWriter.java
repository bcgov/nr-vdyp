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
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Species;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.ecore.utils.Utils;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP0Name;

/**
 * Write a yield table in the format required by Input Model Parameters Report. This class makes some assumptions that
 * it will only be run for a single polygon and layer as that is how the Inpt Model Parameters Report is run
 */
class FullReportYieldTableWriter extends YieldTableWriter<TextYieldTableRowValuesBean> {

	public static final String YIELD_TABLE_FILE_NAME = "YieldReport.txt";

	private final ProjectionContext context;

	private OutputStream outputStream;
	private TextFileTable textFileTable;

	private boolean writeTopHeader = true;

	private class TextFileTable {
		private record Column(String[] headerWords, int width, String format, boolean included, String superHeader) {
		}

		int longestHeaderLength = 0;
		List<Column> columns = new ArrayList();
		List<Object> currentRow;

		public void addColumn(String header, int width) {
			addColumn(header, width, "%" + width + "s");
		}

		public void addBreakColumn(boolean included) {
			addColumn("|", 1, "|", included);
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

		public void addCellAsDouble(Object value) {
			if (value == null) {
				addCell(null);
			} else {
				addCell(Double.parseDouble(value.toString()));
			}
		}

		public void addCellAsInteger(Object value) {
			if (value == null) {
				addCell(null);
			} else {
				addCell(Integer.parseInt(value.toString()));
			}
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

		public void printTableHeader() throws YieldTableGenerationException {
			StringBuilder sb;
			int lineLength = 0;
			String currentSuperHeader = null;
			int numColumns = columns.size();
			for (int endOffset = longestHeaderLength; endOffset >= 1; endOffset--) {
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
			String lastFormat = "";
			int index = 0;
			Object data = null;
			try {
				int numColumns = columns.size();
				for (int j = 0; j < numColumns; j++) {
					Column column = columns.get(j);
					if (column.included() && currentRow.size() > j) {
						Object cellData = currentRow.get(j);
						if (cellData == null && !column.format().equals("|")) {
							doWrite(" ".repeat(column.width() + 1));
						} else {
							doWrite(column.format(), cellData);
						}
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
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

	protected Double getMAI(String value, int currentAge) {
		if (value == null || currentAge <= 0) {
			return null;
		}
		return Double.parseDouble(value) / currentAge;
	}

	@Override
	protected void writeRecord(YieldTableRowContext rowContext) throws YieldTableGenerationException {

		try {
			int currentAge = rowContext.getCurrentTableAge();
			textFileTable.addCellAsInteger(currentRecord.getTotalAge()); // TOT AGE
			textFileTable.addCellAsDouble(currentRecord.getDominantHeight()); // Height
			textFileTable.addCellAsDouble(currentRecord.getSecondaryHeight()); // Secondary Height
			textFileTable.addCellAsDouble(currentRecord.getLoreyHeight()); /// lorey Height
			textFileTable.addCellAsDouble(currentRecord.getDiameter());// "Quad Stnd DIA (cm)";
			textFileTable.addCellAsDouble(currentRecord.getBasalArea());// "BA (m**2/ha)");//Basal Area
			textFileTable.addCellAsDouble(currentRecord.getTreesPerHectare());// TPH

			textFileTable.addCellAsDouble(currentRecord.getWholeStemVolume()); // whole stem volume
			textFileTable.addCell(getMAI(currentRecord.getWholeStemVolume(), currentAge)); // whole Stem Volume MAI

			textFileTable.addCellAsDouble(currentRecord.getCloseUtilizationVolume()); // close util volume
			textFileTable.addCell(getMAI(currentRecord.getCloseUtilizationVolume(), currentAge)); // close util MAI

			textFileTable.addCellAsDouble(currentRecord.getCuVolumeLessDecay()); // Net decay valume
			textFileTable.addCell(getMAI(currentRecord.getCuVolumeLessDecay(), currentAge)); // Net decay valume MAI

			textFileTable.addCellAsDouble(currentRecord.getCuVolumeLessDecayWastage()); // Net decay wastage
			// volume
			textFileTable.addCell(getMAI(currentRecord.getCuVolumeLessDecayWastage(), currentAge)); // Net decay
			// wastage MAI

			textFileTable.addCellAsDouble(currentRecord.getCuVolumeLessDecayWastageBreakage()); // close util
			// less decay
			// wastage and
			// breakage
			// volume
			textFileTable.addCell(getMAI(currentRecord.getCuVolumeLessDecayWastageBreakage(), currentAge)); // FIX ME
																											// close
			// util less
			// decay wastage
			// and breakage
			// MAI

			textFileTable.addCellAsDouble(currentRecord.getCfsBiomassStem()); // CFS Stem Biomass
			textFileTable.addCellAsDouble(currentRecord.getCfsBiomassBark()); // CFS Bark Biomass
			textFileTable.addCellAsDouble(currentRecord.getCfsBiomassBranch()); // CFS Branch Biomass
			textFileTable.addCellAsDouble(currentRecord.getCfsBiomassFoliage()); // CFS Foliage Biomass

			/*
			 * boolean includeSpeciesComposition = false; for(int specIndex = 0; specIndex < 7; specIndex++) {
			 * textFileTable.addColumn("Sp" + (specIndex + 1) + " Code", 3, includeSpeciesComposition); }
			 */
		} catch (Exception e) {
			throw new YieldTableGenerationException(
					"Error writing yield table record for polygon: " + rowContext.getPolygon().getPolygonNumber(), e
			);
		}
		// Print out all the relevant information
		textFileTable.printRow();
	}

	private String centerString(String ogString, int numCharacters) {
		int padding = numCharacters - ogString.length();
		return " ".repeat(padding / 2) + ogString + " ".repeat(padding - (padding / 2));
	}

	@Override
	public void writePolygonTableHeader(
			Polygon polygon, Optional<LayerReportingInfo> layerReportingInfo, boolean doGenerateDetailedTableHeader,
			Integer yieldTableCount
	) throws YieldTableGenerationException {

		if (writeTopHeader) {
			writeTopHeader = false;
			int lineChars = 80;
			StringBuilder titleLine = new StringBuilder();
			String title = context.getParams().getReportTitle();
			if (lineChars < title.length()) {
				doWrite(centerString(title, lineChars));
			} else {
				String[] titleWords = title.split(" ");
				for (String word : titleWords) {
					if (titleLine.length() + word.length() + 1 > lineChars) {
						doWrite(centerString(titleLine.toString(), lineChars));
						titleLine = new StringBuilder();
					}
					titleLine.append(word).append(" ");
				}
				if (!titleLine.isEmpty()) {
					doWrite(centerString(titleLine.toString(), lineChars));
				}
			}

			doWrite(centerString("VDYP Yield Table Report", lineChars));
			Layer layer = polygon.getPrimaryLayer();
			titleLine = new StringBuilder();
			for (Species species : layer.getSp64sAsSupplied()) {
				String append = SiteTool.getSpeciesFullName(species.getSpeciesCode()) + " ("
						+ species.getSpeciesPercent() + "%) ";

			}
		}

		textFileTable = new TextFileTable();
		var params = context.getParams();
		textFileTable.addColumn("TOT AGE", 3, "%3d");
		textFileTable.addColumn("Site HT (m)", 4, "%4.1f");
		textFileTable.addColumn(
				"Scnd HT (m)", 4, "%4.1f",
				params.containsOption(ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE)
		);
		textFileTable.addColumn("Lorey HT (m)", 5, "%5.1f");
		textFileTable.addColumn("Quad Stnd DIA (cm)", 4, "%4.1f");
		textFileTable.addColumn("BA (m**2/ha)", 9, "%9.1f");
		textFileTable.addColumn("TPH (trees/ha)", 10, "%10.0f");

		boolean showBioMassColumns = isCurrentlyWritingCategory(YieldTable.Category.CFSBIOMASS);
		boolean showMofVolumeColumns = isCurrentlyWritingCategory(YieldTable.Category.LAYER_MOFVOLUMES);
		boolean includeMAI = params.containsOption(ExecutionOption.REPORT_INCLUDE_VOLUME_MAI);
		boolean showStemVolume = showMofVolumeColumns
				&& params.containsOption(ExecutionOption.REPORT_INCLUDE_WHOLE_STEM_VOLUME);
		textFileTable.addBreakColumn(showMofVolumeColumns);
		textFileTable.addColumn(
				(includeMAI ? "" : "Whole Stem ") + "VOLUME (m**3/ha)", 11, "%11.1f", showStemVolume,
				includeMAI ? "Whole Stem" : null
		);
		textFileTable.addColumn("MAI (m**3/ha)", 11, "%11.1f", showStemVolume && includeMAI, "Whole Stem");

		boolean showCloseUtilization = showBioMassColumns || (showMofVolumeColumns
				&& params.containsOption(ExecutionOption.REPORT_INCLUDE_CLOSE_UTILIZATION_VOLUME));
		textFileTable.addBreakColumn(showCloseUtilization);
		textFileTable.addColumn(
				(includeMAI ? "" : "Close Utilization ") + "VOLUME (m**3/ha)", 11, "%11.1f", showCloseUtilization,
				includeMAI ? "Close Utilization" : null
		);
		textFileTable.addColumn("MAI (m**3/ha)", 11, "%11.1f", showCloseUtilization && includeMAI, "Close Utilization");

		boolean showNetDecayVolume = showMofVolumeColumns
				&& params.containsOption(ExecutionOption.REPORT_INCLUDE_NET_DECAY_VOLUME);
		textFileTable.addBreakColumn(showNetDecayVolume);
		textFileTable.addColumn(
				(includeMAI ? "" : "Net Decay ") + "VOLUME (m**3/ha)", 11, "%11.1f", showNetDecayVolume,
				includeMAI ? "Net Decay" : null
		);
		textFileTable.addColumn("MAI (m**3/ha)", 11, "%11.1f", showNetDecayVolume && includeMAI, "Net Decay");

		boolean showNetDecayWastageVolume = showMofVolumeColumns
				&& params.containsOption(ExecutionOption.REPORT_INCLUDE_ND_WASTE_VOLUME);
		textFileTable.addBreakColumn(showNetDecayWastageVolume);
		textFileTable.addColumn(
				(includeMAI ? "" : "Net_Decay and_Waste ") + "VOLUME (m**3/ha)", 11, "%11.1f",
				showNetDecayWastageVolume, includeMAI ? "Net Decay and Waste" : null
		);
		textFileTable.addColumn(
				"MAI (m**3/ha)", 11, "%11.1f", showNetDecayWastageVolume && includeMAI, "Net Decay and Waste"
		);

		boolean showMofVolumeBreakageColumns = showMofVolumeColumns
				&& params.containsOption(ExecutionOption.REPORT_INCLUDE_ND_WAST_BRKG_VOLUME);
		textFileTable.addBreakColumn(showMofVolumeBreakageColumns);
		textFileTable.addColumn(
				(includeMAI ? "" : "Net_Decay Waste,_Brkg ") + "VOLUME (m**3/ha)", 11, "%11.1f",
				showMofVolumeBreakageColumns, includeMAI ? "Net Decay Waste, Brkg" : null
		);
		textFileTable.addColumn(
				"MAI (m**3/ha)", 11, "%11.1f", showMofVolumeBreakageColumns && includeMAI, "Net Decay Waste, Brkg"
		);

		textFileTable.addBreakColumn(showBioMassColumns);
		textFileTable.addColumn("Stem (tons/ha)", 12, "%12.1f", showBioMassColumns, "CFS Biomass");
		textFileTable.addColumn("Bark (tons/ha)", 12, "%12.1f", showBioMassColumns, "CFS Biomass");
		textFileTable.addColumn("Branch (tons/ha)", 12, "%12.1f", showBioMassColumns, "CFS Biomass");
		textFileTable.addColumn("Foliage (tons/ha)", 12, "%12.1f", showBioMassColumns, "CFS Biomass");

		/*
		 * TODO species Composition boolean includeSpeciesComposition = false; textFileTable.addColumn("|", 1, "|",
		 * includeSpeciesComposition); for(int specIndex = 0; specIndex < 7; specIndex++) {
		 * textFileTable.addColumn((specIndex + 1), 3, "%2.1f", includeSpeciesComposition, "Spec Composition"); }
		 */

		// Print out all the relevant information
		textFileTable.printTableHeader();

	}

	Polygon lastPolygonForTrailer;

	@Override
	public void writePolygonTableTrailer(Integer yieldTableCount)
			throws YieldTableGenerationException {
		// No data written here
		this.lastPolygonForTrailer = polygon;
	}

	@Override
	public void writeTrailer() throws YieldTableGenerationException {
		// Write all the required META DATA
		writeNotes();
		writeTableProperties();
		writeSpeciesParameters();
		writeSiteIndexCurvesUsed();
		writeAdditionalStandAttributes();
	}

	private void writeNotes() {
		// WHERE ARE THE NOTES STORED? DO WE STORE THEM?
	}

	private void writeTableProperties() throws YieldTableGenerationException {
		List<String> entries = new ArrayList<>();
		entries.add("VDYP UI Version Number... 8.0");
		entries.add("VDYP Version Number... 8.0");
		entries.add("VDYP SI Version Number... 8.0");
		entries.add("SIMDEX Version Number... 8.0");
		Layer layer = lastPolygonForTrailer.getPrimaryLayer();
		int speciesNum = 1;
		for (Species species : layer.getSp64sAsSupplied()) {
			entries.add(
					"Species " + speciesNum++ + "............. " + species.getSpeciesCode() + " ("
							+ species.getSpeciesPercent() + "%)"
			);
		}
		if (lastPolygonForTrailer.getInventoryStandard() == InventoryStandard.FIP) {
			entries.add("FIP Calc Mode.......... 1");
		} else {
			entries.add("VRI Calc Mode.......... 1");
		}
		entries.add("BEC Zone................. " + lastPolygonForTrailer.getBecZone());
		entries.add(
				"Incl Second Species Ht... " + (context.getParams()
						.containsOption(ExecutionOption.DO_INCLUDE_SECONDARY_SPECIES_DOMINANT_HEIGHT_IN_YIELD_TABLE)
								? "1" : "N/A")
		);
		entries.add("% Crown Closure Supplied. " + layer.getCrownClosure());
		entries.add("% Stockable Area Supplied " + layer.getPercentStockable());
		entries.add("CFS Eco Zone............. " + lastPolygonForTrailer.getCfsEcoZone());
		entries.add(
				"Trees Per Hectare........ " + layer.getTreesPerHectare() != null
						? layer.getTreesPerHectare().toString() : "<Not Used>"
		);
		entries.add(
				"Measured Basal Area........ " + layer.getBasalArea() != null ? layer.getBasalArea().toString()
						: "<Not Used>"
		);
		entries.add("Starting Total Age....... " + context.getParams().getAgeStart());
		entries.add("Finishing Total Age...... " + context.getParams().getAgeEnd());
		entries.add("Age Increment............ " + context.getParams().getAgeIncrement());
		entries.add("");
		for (Stand stand : layer.getSp0sAsSupplied()) {
			SP0Name speciesGroup = SP0Name.forText(stand.getSpeciesGroup().getSpeciesCode());
			entries.add(
					"Min DBH Limit: " + speciesGroup.getText() + "........ "
							+ context.getParams().getUtils().get(speciesGroup) + "cm"
			);
		}
		doWrite("TABLE PROPERTIES...\n\n");
		for (int i = 0; i < (entries.size()) / 2; i++) {
			int j = i + (entries.size()) / 2;
			if (j < entries.size()) {
				doWrite("%-40s %s\n", entries.get(i), entries.get(j));
			} else {
				doWrite("%-40s\n", entries.get(i));
			}
		}
		doWrite("\n");
	}

	private void writeSpeciesParameters() throws YieldTableGenerationException {
		doWrite("Species Parameters...\n");
		doWrite("Species |  %% Comp | Tot Age |  BH Age |  Height |    SI   |  YTBH   \n");
		doWrite("--------+---------+---------+---------+---------+---------+---------\n");
		Layer layer = lastPolygonForTrailer.getPrimaryLayer();
		for (Species species : layer.getSp64sAsSupplied()) {
			String speciesCode = species.getSpeciesCode();
			Double percentComposition = species.getSpeciesPercent();
			Double totalAge = species.getTotalAge();
			Double bhAge = species.getAgeAtBreastHeight();
			Double height = species.getDominantHeight();
			Double siteIndex = species.getSiteIndex();
			Double ytbH = species.getYearsToBreastHeight();

			doWrite(
					"%-6s  |  %5.1f  |  %5.0f  |  %5.0f  |  %5.2f  |  %5.2f  |  %5.2f  \n", //
					speciesCode, percentComposition, totalAge, bhAge, height, siteIndex, ytbH
			);
		}
		doWrite("\n");
	}

	private void writeSiteIndexCurvesUsed() throws YieldTableGenerationException {
		doWrite("Site Index Curves Used...\n");

		doWrite("\n");
	}

	private void writeAdditionalStandAttributes() throws YieldTableGenerationException {
		// This is where adjust details are written, but none are currently applied
		doWrite("Additional Stand Attributes:\n");
		doWrite("----------------------------\n\n");
		doWrite("        None Applied.");
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
