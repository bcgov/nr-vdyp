package ca.bc.gov.nrs.vdyp.backend.io.write;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import ca.bc.gov.nrs.vdyp.backend.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Species;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Write files to be input into FIP Start
 */
public class FipStartOutputWriter extends AbstractOutputWriter implements Closeable {

	protected final OutputStream polygonFile;
	protected final OutputStream layersFile;
	protected final OutputStream speciesFile;

	// FORMAT(A25, 1x, A1,1x, A4,1x, F4.0,1x, I2,1x, A5, F5.2)
	static final String POLY_FORMAT = POLY_IDENTIFIER_FORMAT + " %1s %4s %4.0f %2d %5s%5s\n";

	// FORMAT(A25, 1x, A1, f4.0, f5.2, f5.2, f5.1, 3x, A2, A3, f5.2, A1, 2x, I4, 1x, F6.1, I3)
	static final String LAYER_FORMAT = POLY_IDENTIFIER_FORMAT + " %1s%4s%5s%5s%5s   %2s%3s%5s%1s  %4d %6s%3s\n";
	static final String END_LAYER_FORMAT = POLY_IDENTIFIER_FORMAT + " Z   0  0.0  0.0  0.0         0.0\n";

	// FORMAT(A25, 1x, A1, 1x, A2, F6.1, A32)
	static final String SPECIES_FORMAT = POLY_IDENTIFIER_FORMAT + " %1s %2s%6s%8s%8s%8s%8s\n";
	static final String END_SPECIES_FORMAT = POLY_IDENTIFIER_FORMAT + " Z\n";

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

	/**
	 * V7W_FIP - Write the given polygon record to the polygon file
	 *
	 * @param polygon
	 * @throws IOException
	 */
	public void writePolygon(Polygon polygon, ProjectionTypeCode projectionType, PolygonProjectionState state)
			throws IOException {

		writeFormat(
				polygonFile, //
				POLY_FORMAT, //

				polygon.buildPolygonDescriptor(), //
				polygon.getForestInventoryZone() == null ? "" : polygon.getForestInventoryZone(), //
				polygon.getBecZone() == null ? "" : polygon.getBecZone(), //
				polygon.determineStockabilityByProjectionType(projectionType), //
				state.getProcessingModeUsedByProjectionType(projectionType).value, //
				polygon.getNonProductiveDescriptor() == null ? "" : polygon.getNonProductiveDescriptor(), //
				format(polygon.getYieldFactor(), 5, 2) //
		);
	}

	/**
	 * V7W_FIL1 - Write the given layer record to the layers file, and recursively write the layer's species to the
	 * species file.
	 *
	 * @param layers
	 * @throws IOException
	 */
	public void writePolygonLayer(Layer layer) throws IOException {

		boolean speciesWritten = false;

		Stand leadingStand = layer.determineLeadingSp0(0);
		Species leadingSpecies = leadingStand.getSpeciesByPercent().get(0);

		writeFormat(
				layersFile, //
				LAYER_FORMAT, //

				layer.getPolygon().buildPolygonDescriptor(), //
				LayerType.PRIMARY.getAlias(), // vdypintperform.c lines 2634 - 2651 - always write "P" for the layer
												// code.
				format(leadingSpecies.getTotalAge(), 4, 0), //
				format(leadingSpecies.getDominantHeight(), 5, 2), //
				format(leadingSpecies.getSiteIndex(), 5, 2), //
				format(layer.getCrownClosure(), 5), //
				leadingStand.getSp0Code(), //
				leadingSpecies.getSpeciesCode(), //
				format(leadingSpecies.getYearsToBreastHeight(), 5, 2), //
				' ', // stocking class - unknown
				Vdyp7Constants.EMPTY_INT, // inventory type group - unknown
				format(leadingSpecies.getAgeAtBreastHeight(), 6, 1), //
				leadingSpecies.getSiteCurve() != null ? format(leadingSpecies.getSiteCurve().n(), 3)
						: " " + Vdyp7Constants.EMPTY_INT
		);

		for (Stand s : layer.getSp0sAsSupplied()) {
			writeLayerSp0(s);
			speciesWritten = true;
		}

		writeLayersEndRecord(layer.getPolygon());

		if (speciesWritten) {
			writeSpeciesEndRecord(layer.getPolygon());
		}
	}

	@Override
	protected void writeLayersEndRecord(Polygon polygon) throws IOException {
		writeEndRecord(layersFile, polygon, END_LAYER_FORMAT);
	}

	@Override
	protected void writeSpeciesEndRecord(Polygon polygon) throws IOException {
		writeEndRecord(speciesFile, polygon, END_SPECIES_FORMAT);
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
			if (i < stand.getSpeciesByPercent().size()) {
				Species s = stand.getSpeciesByPercent().get(i);
				speciesDistributionTexts[i] = String.format("%3s%5.1f", s.getSpeciesCode(), s.getSpeciesPercent());
			} else {
				speciesDistributionTexts[i] = "     0.0";
			}
		}

		writeFormat(
				speciesFile, //
				SPECIES_FORMAT, //

				stand.getLayer().getPolygon().buildPolygonDescriptor(), //
				LayerType.PRIMARY.getAlias(), // vdypintperform.c lines 2634 - 2651 - always write "P" for the layer
												// code.
				stand.getSp0Code(), //
				format(stand.getSpeciesGroup().getSpeciesPercent(), 6, 1), speciesDistributionTexts[0],
				speciesDistributionTexts[1], speciesDistributionTexts[2], speciesDistributionTexts[3]
		);
	}

	@Override
	public void close() throws IOException {
		polygonFile.close();
		speciesFile.close();
		layersFile.close();
	}
}
