package ca.bc.gov.nrs.vdyp.backend.projection;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

public class ComponentRunner implements IComponentRunner {

	@Override
	public void runFipStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {
		
		state.setInitialProcessingResults(projectionTypeCode, 0, new ProcessingResultsCode(-99));
	}

	@Override
	public void runVriStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		state.setInitialProcessingResults(projectionTypeCode, 0, new ProcessingResultsCode(-99));
	}

	@Override
	public void runAdjust(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void runForward(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void runBack(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		// TODO Auto-generated method stub
		
	}
}
