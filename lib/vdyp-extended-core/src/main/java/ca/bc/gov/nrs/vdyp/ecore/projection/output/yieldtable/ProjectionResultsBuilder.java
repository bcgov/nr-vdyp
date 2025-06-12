package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Sets;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionStageCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class ProjectionResultsBuilder {

	public static Map<Integer, VdypPolygon> read(
			Polygon polygon, PolygonProjectionState state, ProjectionTypeCode projectionType,
			ProjectionResultsReader forwardReader, ProjectionResultsReader backwardsReader
	) throws YieldTableGenerationException {

		var resultsByYear = new HashMap<Integer, VdypPolygon>();

		resultsByYear.putAll(
				readComponentResults(polygon, projectionType, ProjectionStageCode.Forward, forwardReader, state)
		);

		var backResultsByYear = readComponentResults(
				polygon, projectionType, ProjectionStageCode.Back, backwardsReader, state
		);

		var yearsInBothSets = Sets.intersection(backResultsByYear.keySet(), resultsByYear.keySet());
		if (!yearsInBothSets.isEmpty()) {
			throw new IllegalStateException(
					MessageFormat.format(
							"{0}: contains both FORWARD and BACK results for the years {1}", polygon, yearsInBothSets
					)
			);
		}

		resultsByYear.putAll(backResultsByYear);

		return resultsByYear;
	}

	private static Map<Integer, VdypPolygon> readComponentResults(
			Polygon polygon, ProjectionTypeCode projectionType, ProjectionStageCode stageCode,
			ProjectionResultsReader reader, PolygonProjectionState state
	) throws YieldTableGenerationException {

		if (state.didRunProjectionStage(stageCode, projectionType)) {
			return reader.read(polygon);
		} else {
			return new HashMap<>();
		}
	}
}
