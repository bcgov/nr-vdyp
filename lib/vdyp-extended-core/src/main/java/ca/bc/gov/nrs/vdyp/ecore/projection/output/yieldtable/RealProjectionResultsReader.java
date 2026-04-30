package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.ForwardDataStreamReader;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypPolygonParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypSpeciesParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypUtilizationParser;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class RealProjectionResultsReader implements ProjectionResultsReader {

	private static Logger logger = LoggerFactory.getLogger(RealProjectionResultsReader.class);

	private final Map<String, Object> controlMap;

	public RealProjectionResultsReader(Map<String, Object> controlMap) {
		this.controlMap = controlMap;
	}

	@Override
	public Map<Integer, VdypPolygon> read(Polygon polygon) throws YieldTableGenerationException {

		var projectionResultsByYear = new HashMap<Integer, VdypPolygon>();

		var expectedPolygonIdentifier = new PolygonIdentifier(
				polygon.getMapSheet(), polygon.getPolygonNumber(), polygon.getDistrict(), 0 /* expect any year */
		);

		Object polygonFileLocation = controlMap.get(ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name());
		Object speciesFileLocation = controlMap.get(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES.name());
		Object utilizationsFileLocation = controlMap.get(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name());

		var readerControlMap = new HashMap<String, Object>();

		var absolutePathFileResolver = new FileSystemFileResolver();
		readerControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_POLY.name(),
				new VdypPolygonParser().map(polygonFileLocation.toString(), absolutePathFileResolver, readerControlMap)
		);
		readerControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(),
				new VdypSpeciesParser().reportSIHeight()
						.map(speciesFileLocation.toString(), absolutePathFileResolver, readerControlMap)
		);
		readerControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(),
				new VdypUtilizationParser()
						.map(utilizationsFileLocation.toString(), absolutePathFileResolver, readerControlMap)
		);
		readerControlMap.put(ControlKey.BEC_DEF.name(), controlMap.get(ControlKey.BEC_DEF.name()));
		readerControlMap.put(ControlKey.SP0_DEF.name(), controlMap.get(ControlKey.SP0_DEF.name()));

		try (ForwardDataStreamReader reader = new ForwardDataStreamReader(readerControlMap);) {

			var vdypPolygon = reader.readNextPolygon(false /* do not run post-create adjustments */);
			while (vdypPolygon.isPresent()
					&& expectedPolygonIdentifier.getBase().equals(vdypPolygon.get().getPolygonIdentifier().getBase())) {

				doLoadProjectionResultsProcessing(vdypPolygon.get());

				projectionResultsByYear.put(vdypPolygon.get().getPolygonIdentifier().getYear(), vdypPolygon.get());

				vdypPolygon = reader.readNextPolygon(false /* do not run post-create adjustments */);
			}

			if (projectionResultsByYear.size() == 0) {
				recordPolygonProjectionInformationMissing(polygon);
			}

			if (vdypPolygon.isPresent()) {
				throw new YieldTableGenerationException(
						MessageFormat.format(
								"Expected exactly one polygon {0} in the projection output, but saw {1} as well",
								expectedPolygonIdentifier, vdypPolygon.get().getPolygonIdentifier()
						)
				);
			}
		} catch (ProcessingException e) {
			throw new YieldTableGenerationException(e);
		}

		return projectionResultsByYear;
	}

	/**
	 * Perform load data results transforms on data coming from forward or back Sum the species utilization vectors at
	 * each utilization class for the layer utlization class instead of trusting the layer utilization values provided
	 * from forward (or back)
	 *
	 * vdyp7loaddataresults.for Potentially a side effect but from lines 1927 - 2362 the utilization for each util class
	 * is first summed on a per species per year so that small represents all including small, and 7.5 index represents
	 * all and 12.5 represents 12.5+ etc
	 *
	 * This is apparently added to the layer utilization amounts for each utilization class for the layer. There are
	 * cases where the output from forward appears to have some rounding errors in it and doing this prevents those from
	 * being summed together. This code does not sum the util classes because that action is performed on read where
	 * needed in VDYP8 instad
	 *
	 * @param polygon
	 */
	private static void doLoadProjectionResultsProcessing(VdypPolygon polygon) {
		for (var entry : polygon.getLayers().entrySet()) {
			var layer = entry.getValue();
			for (UtilizationClass uc : UtilizationClass.values()) {
				float ucTPH = 0.0f;
				float ucVolWS = 0.0f;
				float ucVolCU = 0.0f;
				float ucVolD = 0.0f;
				float ucVolDW = 0.0f;
				float ucVolDWB = 0.0f;
				float ucBA = 0.0f;
				for (var specEntry : layer.getSpecies().entrySet()) {
					var species = specEntry.getValue();
					ucTPH += species.getTreesPerHectareByUtilization().get(uc);
					ucVolWS += species.getWholeStemVolumeByUtilization().get(uc);
					ucVolCU += species.getCloseUtilizationVolumeByUtilization().get(uc);
					ucVolD += species.getCloseUtilizationVolumeNetOfDecayByUtilization().get(uc);
					ucVolDW += species.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().get(uc);
					ucVolDWB += species.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization().get(uc);
					ucBA += species.getBaseAreaByUtilization().get(uc);
				}
				layer.getTreesPerHectareByUtilization().set(uc, ucTPH);
				layer.getWholeStemVolumeByUtilization().set(uc, ucVolWS);
				layer.getCloseUtilizationVolumeByUtilization().set(uc, ucVolCU);
				layer.getCloseUtilizationVolumeNetOfDecayByUtilization().set(uc, ucVolD);
				layer.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().set(uc, ucVolDW);
				layer.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization().set(uc, ucVolDWB);
				layer.getBaseAreaByUtilization().set(uc, ucBA);
			}
		}
	}

	private static void recordPolygonProjectionInformationMissing(Polygon polygon) {
		String message = MessageFormat.format(
				"Expected, but did not find, a polygon {0,number,#} in the output of Forward",
				polygon.getPolygonNumber()
		);
		logger.warn("{}: {}", polygon, message);
	}
}
