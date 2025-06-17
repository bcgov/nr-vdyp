package ca.bc.gov.nrs.vdyp.ecore.projection;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;

public class StubComponentRunner implements ComponentRunner {

	@Override
	public void runFipStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, Optional.empty());
	}

	@Override
	public void runVriStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		state.setProcessingResults(ProjectionStageCode.Initial, projectionTypeCode, Optional.empty());
	}

	@Override
	public void runAdjust(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		state.setProcessingResults(ProjectionStageCode.Adjust, projectionTypeCode, Optional.empty());
	}

	@Override
	public void runForward(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		state.setProcessingResults(ProjectionStageCode.Forward, projectionTypeCode, Optional.empty());
	}

	@Override
	public void runBack(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		state.setProcessingResults(ProjectionStageCode.Back, projectionTypeCode, Optional.empty());
	}

	@Override
	public void generateYieldTables(ProjectionContext context, Polygon polygon, PolygonProjectionState state)
			throws YieldTableGenerationException {
	}
}
