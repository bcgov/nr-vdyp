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

	private static void recordPolygonProjectionInformationMissing(Polygon polygon) {
		String message = MessageFormat.format(
				"Expected, but did not find, a polygon {0,number,#} in the output of Forward",
				polygon.getPolygonNumber()
		);
		logger.warn("{}: {}", polygon, message);
	}
}
