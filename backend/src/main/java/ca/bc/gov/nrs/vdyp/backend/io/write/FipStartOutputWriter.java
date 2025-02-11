package ca.bc.gov.nrs.vdyp.backend.io.write;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.backend.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Species;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

/**
 * Write files to be input into FIP Start
 */
public class FipStartOutputWriter implements Closeable {

	protected final OutputStream polygonFile;
	protected final OutputStream layersFile;
	protected final OutputStream speciesFile;

	private Optional<Integer> currentYear = Optional.empty();

	static final String POLY_IDENTIFIER_FORMAT = "%-25s";

	static final float EMPTY_FLOAT = -9f;
	static final int EMPTY_INT = -9;

	// FORMAT(A25,1x, A1,1x, A4,1x, F4.0,1x, I2,1x, A5, F5.2)
	static final String POLY_FORMAT = POLY_IDENTIFIER_FORMAT + " %1s %4s %4.0f %2d %5s%5.2f\n";

	// FORMAT(A25,1x, A1, f4.0, f5.2, f5.2, f5.1, 3x, A2, A3, f5.2, A1, 2x, I4, 1x, F6.1, I3)
	static final String LAYER_FORMAT = POLY_IDENTIFIER_FORMAT
			+ " %1s%4.0f%5.2f%5.2f%5.1f   %2s%3s%5.2f%1s  %4d %6.1f%3d\n";

	// FORMAT(A25, 1x, A1, 1x, A2, F6.1, A32)
	static final String SPECIES_FORMAT = POLY_IDENTIFIER_FORMAT + " %1s %2s%6.1f%8s%8s%8s%8s\n";

	static final String END_RECORD_FORMAT = POLY_IDENTIFIER_FORMAT + " Z\n";

	/**
	 * Create a writer for FipStart input files using provided OutputStreams. The Streams will be closed when the writer
	 * is closed.
	 *
	 * @param polygonFile
	 * @param layersFile
	 * @param speciesFile
	 */
	public FipStartOutputWriter(OutputStream polygonFile, OutputStream layersFile, OutputStream speciesFile) {
		this.polygonFile = polygonFile;
		this.layersFile = layersFile;
		this.speciesFile = speciesFile;
	}

	public void setPolygonYear(int currentYear) {
		this.currentYear = Optional.of(currentYear);
	}

	/**
	 * V7W_FIP - Write the given polygon record to the polygon file
	 *
	 * @param polygon
	 * @throws IOException
	 */
	public void writePolygon(Polygon polygon, ProjectionTypeCode projectionType, PolygonProjectionState state) throws IOException {

		writeFormat(
				polygonFile, //
				POLY_FORMAT, //

				polygon.buildPolygonDescriptor(), //
				polygon.getForestInventoryZone() == null ? "" : polygon.getForestInventoryZone(), //
				polygon.getBecZone() == null ? "" : polygon.getBecZone(), //
				polygon.determineStockabilityByProjectionType(projectionType), //
				state.getProcessingModeUsedByProjectionType().get(projectionType).value, //
				polygon.getNonProductiveDescriptor() == null ? "" : polygon.getNonProductiveDescriptor(), //
				polygon.getYieldFactor() == null ? -9.0 : polygon.getYieldFactor()//
		);
	}

	/**
	 * V7W_FIL1 - Write the given layer records to the layers file, and recursively
	 * write each layer's species to the species file.
	 * 
	 * @param layers
	 * @throws IOException
	 */
	public void writePolygonLayers(Layer... layers) throws IOException {

		boolean layerWritten = false;
		boolean speciesWritten = false;
		
		for (Layer layer: layers) {
			
			layerWritten = true;
			
			Stand leadingStand = layer.determineLeadingSp0(0);
			Species leadingSpecies = leadingStand.getSpecies().get(0);
			
			writeFormat(
					layersFile, //
					LAYER_FORMAT, //
	
					layer.getPolygon().buildPolygonDescriptor(), //
					layer.getLayerId(), //
					leadingSpecies.getTotalAge(), //
					leadingSpecies.getDominantHeight(), //
					leadingSpecies.getSiteIndex(), //
					layer.getCrownClosure(), //
					leadingStand.getSp0Code(), //
					leadingSpecies.getSpeciesCode(), //
					leadingSpecies.getYearsToBreastHeight(), //
					' ', // stocking class - unknown
					-9, // inventory type group - unknown
					leadingSpecies.getAgeAtBreastHeight(), //
					leadingSpecies.getSiteCurve().n()
			);
	
			for (Stand s : layer.getSp0sAsSupplied()) {
				writeLayerSp0(s);
				speciesWritten = true;
			}
		}
		
		if (layerWritten) {
			writeLayersEndRecord(layers[0].getPolygon());
		}
		
		if (speciesWritten) {
			writeSpeciesEndRecord(layers[0].getPolygon());
		}
	}

	/** 
	 * V7W_FIS1 - Write the given species group (Sp0) record to the species file.
	 * 
	 * @param stand
	 * @throws IOException
	 */
	private void writeLayerSp0(Stand stand) throws IOException {
		
		String[] speciesDistributionTexts = new String[4];
		for (int i = 0; i < 4; i++) {
			if (i < stand.getSpecies().size()) {
				Species s = stand.getSpecies().get(i);
				speciesDistributionTexts[i] = String.format("%3s%5.1f", s.getSpeciesCode(), s.getSpeciesPercent());
			} else {
				speciesDistributionTexts[i] = "        ";
			}
		}
		
		writeFormat(
				speciesFile, //
				SPECIES_FORMAT, //

				stand.getLayer().getPolygon().buildPolygonDescriptor(), //
				stand.getLayer().getLayerId(), //
				stand.getSp0Code(), //
				stand.getSpeciesGroup().getSpeciesPercent(),
				speciesDistributionTexts[0],
				speciesDistributionTexts[1],
				speciesDistributionTexts[2],
				speciesDistributionTexts[3]
		);
	}

	private void writeEndRecord(OutputStream os, Polygon polygon) throws IOException {
		writeFormat(os, END_RECORD_FORMAT, polygon.buildPolygonDescriptor());
	}

	private void writeLayersEndRecord(Polygon polygon) throws IOException {
		writeEndRecord(layersFile, polygon);
	}

	private void writeSpeciesEndRecord(Polygon polygon) throws IOException {
		writeEndRecord(speciesFile, polygon);
	}

	void writeFormat(OutputStream os, String format, Object... params) throws IOException {
		os.write(String.format(format, params).getBytes());
	}

	@Override
	public void close() throws IOException {
		polygonFile.close();
		speciesFile.close();
		layersFile.close();
	}
}
