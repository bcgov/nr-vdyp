package ca.bc.gov.nrs.vdyp.backend.io.write;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Species;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

/**
 * Write files to be input into FIP Start
 */
public class VriStartOutputWriter implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(VriStartOutputWriter.class);

	protected final OutputStream polygonFile;
	protected final OutputStream layersFile;
	protected final OutputStream speciesFile;
	protected final OutputStream siteIndexFile;

	private Optional<Integer> currentYear = Optional.empty();

	static final String POLY_IDENTIFIER_FORMAT = "%-25s";

	static final float EMPTY_FLOAT = -9f;
	static final int EMPTY_INT = -9;

	// FORMAT( A25, 3x, A4, 1x, F4.0, 1x, I2, 1x, A5, F5.2 )
	static final String POLY_FORMAT = POLY_IDENTIFIER_FORMAT + "   %4s %4.0f %2d %5s%5.2f\n";

	// FORMAT( A25, 1x, A1, f6.0, f9.5, 1x, f8.2, 1x, f4.1 )
	static final String LAYER_FORMAT = POLY_IDENTIFIER_FORMAT + " %1s%6d%9.5f %8.2f %4.1f\n";

	// FORMAT(A25, 1x, A1, 1x, A2, F6.1, A32)
	static final String SPECIES_FORMAT = POLY_IDENTIFIER_FORMAT + " %1s %2s%6.1f%8s%8s%8s%8s\n";

	// FORMAT(A25,1x,A1,f4.0,f5.2,f5.2,8x,A2,A3,f5.2,8x,f6.1,I3)
	static final String SITE_INDEX_FORMAT = POLY_IDENTIFIER_FORMAT
			+ " %1s%4s%5.2f%5.2f        %2s%3s%5.2f        %6.1f%3d\n";

	static final String END_RECORD_FORMAT = POLY_IDENTIFIER_FORMAT + " Z\n";

	/**
	 * Create a writer for VriStart input files using provided OutputStreams. The Streams will be closed when the writer
	 * is closed.
	 *
	 * @param polygonFile
	 * @param layersFile
	 * @param speciesFile
	 * @param siteIndexFile
	 */
	public VriStartOutputWriter(
			OutputStream polygonFile, OutputStream layersFile, OutputStream speciesFile, OutputStream siteIndexFile
	) {
		this.polygonFile = polygonFile;
		this.layersFile = layersFile;
		this.speciesFile = speciesFile;
		this.siteIndexFile = siteIndexFile;
	}

	public void setPolygonYear(int currentYear) {
		this.currentYear = Optional.of(currentYear);
	}

	/**
	 * V7W_RIP - Write the given polygon record to the polygon file
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
				polygon.getBecZone() == null ? "" : polygon.getBecZone(), //
				polygon.determineStockabilityByProjectionType(projectionType), //
				state.getProcessingModeUsedByProjectionType().get(projectionType).value, //
				polygon.getNonProductiveDescriptor() == null ? "" : polygon.getNonProductiveDescriptor(), //
				polygon.getYieldFactor() == null ? -9.0 : polygon.getYieldFactor()//
		);
	}

	/**
	 * V7W_RIL1 - Write the given layer records to the layers file, and recursively write each layer's species to the
	 * species file.
	 * 
	 * @param layers
	 * @throws IOException
	 */
	public void writePolygonLayers(Layer... layers) throws IOException {

		boolean layerWritten = false;

		for (Layer layer : layers) {

			layerWritten = true;

			writeFormat(
					layersFile, //
					LAYER_FORMAT, //

					layer.getPolygon().buildPolygonDescriptor(), //
					layer.getLayerId(), //
					layer.getCrownClosure() == null ? -9 : layer.getCrownClosure(), //
					layer.getBasalArea() == null ? -9.0 : layer.getBasalArea(), //
					layer.getTreesPerHectare() == null ? -9.0 : layer.getTreesPerHectare(), //
					layer.getMeasuredUtilizationLevel() == null ? -9.0 : layer.getMeasuredUtilizationLevel()
			);

			writeLayerSpeciesInfo(layer);
		}

		if (layerWritten) {
			writeLayersEndRecord(layers[0].getPolygon());
		}
	}

	/**
	 * V7W_RIS1 - Write the given species group (Sp0) record to the species file.
	 * 
	 * @param stand
	 * @throws IOException
	 */
	private void writeLayerSpeciesInfo(Layer layer) throws IOException {

		boolean leadingSiteDominantHeightExceeds6m = false;
		
		Stand leadingSite = layer.getSp0sByPercent().get(0);
		if (leadingSite != null) {
			if (leadingSite.getSpeciesGroup().getDominantHeight() > 6.0) {
				logger.debug(
						"{}: height {} of leading species is tall enough to suppress secondary species heights less than breast height",
						layer, leadingSite.getSpeciesGroup().getDominantHeight()
				);
				leadingSiteDominantHeightExceeds6m = true;
			}
		}
		
		boolean speciesWasWritten = false;

		for (Stand stand : layer.getSp0sAsSupplied()) {

			if (stand.getSpeciesGroup().getSpeciesPercent() == 0.0) {
				logger.debug("{}: not emitting since species percent is 0", stand);
				continue;
			}

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
					stand.getSpeciesGroup().getSpeciesPercent(), //
					speciesDistributionTexts[0], speciesDistributionTexts[1], speciesDistributionTexts[2],
					speciesDistributionTexts[3]
			);

			var standDominantHeight = stand.getSpeciesGroup().getDominantHeight();
			if (leadingSiteDominantHeightExceeds6m && standDominantHeight != null && standDominantHeight <= 1.3) {
				logger.warn(
						"Secondary species group {} w/height {} and age {} suppressed next to tall leading species group",
						stand, leadingSite.getSpeciesGroup().getDominantHeight(),
						leadingSite.getSpeciesGroup().getTotalAge()
				);
				standDominantHeight = -9.0;
			}

			var sp0 = stand.getSpeciesGroup();

			String totalAgeText;
			if (stand.getSpeciesGroup().getTotalAge() == null) {
				totalAgeText = "  -9";
			} else {
				totalAgeText = Long.toString(Math.round(stand.getSpeciesGroup().getTotalAge()));
			}

			writeFormat(
					siteIndexFile, //
					SITE_INDEX_FORMAT, //

					stand.getLayer().getPolygon().buildPolygonDescriptor(), //
					stand.getLayer().getLayerId(), //
					totalAgeText, //
					sp0.getDominantHeight() == null ? -9.0 : sp0.getDominantHeight(), //
					sp0.getSiteIndex() == null ? -9.0 : sp0.getSiteIndex(), //
					sp0.getSpeciesCode(), //
					stand.getSpecies().get(0).getSpeciesCode(), //
					sp0.getYearsToBreastHeight() == null ? -9.0 : sp0.getYearsToBreastHeight(), //
					sp0.getAgeAtBreastHeight() == null ? -9.0 : sp0.getAgeAtBreastHeight(), //
					sp0.getSiteCurve() == null ? -9 : sp0.getSiteCurve().n()
			);
			
			speciesWasWritten = true;
		}
		
		if (speciesWasWritten) {
			writeSpeciesEndRecord(layer.getPolygon());
		}
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
