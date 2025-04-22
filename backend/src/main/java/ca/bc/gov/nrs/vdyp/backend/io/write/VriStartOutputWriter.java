package ca.bc.gov.nrs.vdyp.backend.io.write;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Species;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.utils.Utils;
import ca.bc.gov.nrs.vdyp.model.LayerType;

/**
 * Write files to be input into FIP Start
 */
public class VriStartOutputWriter extends AbstractOutputWriter implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(VriStartOutputWriter.class);

	protected final OutputStream polygonFile;
	protected final OutputStream layersFile;
	protected final OutputStream speciesFile;
	protected final OutputStream siteIndexFile;

	// FORMAT( A25, 3x, A4, 1x, F4.0, 1x, I2, 1x, A5, F5.2 )
	static final String POLY_FORMAT = POLY_IDENTIFIER_FORMAT + "   %4s %4s %2d %5s%5s\n";

	// FORMAT( A25, 1x, A1, f6.0, f9.5, 1x, f8.2, 1x, f4.1 )
	static final String LAYER_FORMAT = POLY_IDENTIFIER_FORMAT + " %1s%6s%9s %8s %4s\n";
	static final String END_LAYER_FORMAT = POLY_IDENTIFIER_FORMAT + " Z\n";

	// FORMAT( A25, 1x, A1, 1x, A2, F6.1, A32)
	static final String SPECIES_FORMAT = POLY_IDENTIFIER_FORMAT + " %1s %2s%6s%8s%8s%8s%8s\n";
	static final String END_SPECIES_FORMAT = POLY_IDENTIFIER_FORMAT + " Z      0.0     0.0     0.0     0.0     0.0\n";

	// FORMAT( A25, 1x, A1, f4.0, f5.2, f5.2, 8x, A2, A3, f5.2, 8x, f6.1, I3)
	static final String SITE_INDEX_FORMAT = POLY_IDENTIFIER_FORMAT + " %1s%4s%5s%5s        %2s%3s%5s        %6s%3s\n";
	static final String END_SITE_INDEX_FORMAT = POLY_IDENTIFIER_FORMAT + " Z   0  0.0  0\n";

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
				format(polygon.determineStockabilityByProjectionType(projectionType), 4, 0), //
				state.getProcessingMode(projectionType).value, //
				polygon.getNonProductiveDescriptor() == null ? "" : polygon.getNonProductiveDescriptor(), //
				format(polygon.getYieldFactor(), 5, 2) //
		);
	}

	/**
	 * V7W_RIL1 - Write the given layer record to the layers file, and recursively write the layer's species to the
	 * species file.
	 *
	 * @param layers
	 * @throws IOException
	 */
	public void writePolygonLayer(Layer layer) throws IOException {

		writeFormat(
				layersFile, //
				LAYER_FORMAT, //

				layer.getPolygon().buildPolygonDescriptor(), //
				// vdypintperform.c lines 3686 - 3703 - always write "P" for the layer code.
				LayerType.PRIMARY.getAlias(), //
				format(layer.getCrownClosure(), 6), //
				format(layer.getBasalArea(), 9, 5), //
				format(layer.getTreesPerHectare(), 8, 2), //
				format(layer.getMeasuredUtilizationLevel(), 4, 1)
		);

		writeLayerSpeciesInfo(layer);

		writeLayersEndRecord(layer.getPolygon());
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
			if (Utils.safeGet(leadingSite.getSpeciesGroup().getDominantHeight()) > 6.0) {
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
				if (i < stand.getSpeciesByPercent().size()) {
					Species s = stand.getSpeciesByPercent().get(i);
					var percentOfStand = 100.0 * s.getSpeciesPercent() / stand.getSpeciesGroup().getSpeciesPercent();
					speciesDistributionTexts[i] = String.format("%3s%5.1f", s.getSpeciesCode(), percentOfStand);
				} else {
					speciesDistributionTexts[i] = "     0.0";
				}
			}

			writeFormat(
					speciesFile, //
					SPECIES_FORMAT, //

					stand.getLayer().getPolygon().buildPolygonDescriptor(), //
					LayerType.PRIMARY.getAlias(), // vdypintperform.c lines 3934 - 3950 - always write "P" for the layer
													// code.
					stand.getSp0Code(), //
					format(stand.getSpeciesGroup().getSpeciesPercent(), 6, 1), //
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
				standDominantHeight = Vdyp7Constants.EMPTY_DECIMAL;
			}

			var sp0 = stand.getSpeciesGroup();

			String totalAgeText;
			if (stand.getSpeciesGroup().getTotalAge() == null) {
				totalAgeText = "  " + Vdyp7Constants.EMPTY_INT;
			} else {
				totalAgeText = Long.toString(Math.round(stand.getSpeciesGroup().getTotalAge()));
			}

			writeFormat(
					siteIndexFile, //
					SITE_INDEX_FORMAT, //

					stand.getLayer().getPolygon().buildPolygonDescriptor(), //
					LayerType.PRIMARY.getAlias(), // vdypintperform.c lines 3934 - 3950 - always write "P" for the layer
													// code.
					totalAgeText, //
					format(sp0.getDominantHeight(), 5, 2), //
					format(sp0.getSiteIndex(), 5, 2), //
					sp0.getSpeciesCode(), //
					stand.getSpeciesByPercent().get(0).getSpeciesCode(), //
					format(sp0.getYearsToBreastHeight(), 5, 2), //
					format(sp0.getAgeAtBreastHeight(), 6, 1), //
					sp0.getSiteCurve() == null ? " " + Vdyp7Constants.EMPTY_INT : format(sp0.getSiteCurve().n(), 3)
			);

			speciesWasWritten = true;
		}

		if (speciesWasWritten) {
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
		writeEndRecord(siteIndexFile, polygon, END_SITE_INDEX_FORMAT);
	}

	@Override
	public void close() {
		Utils.close(polygonFile, "VriStartOutputWriter.polygonFile");
		Utils.close(speciesFile, "VriStartOutputWriter.speciesFile");
		Utils.close(layersFile, "VriStartOutputWriter.layersFile");
	}
}
