package ca.bc.gov.nrs.vdyp.ecore.projection;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;

public interface ComponentRunner {

	void runFipStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException;

	void runVriStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException;

	void runAdjust(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException;

	void runForward(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException;

	void runBack(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException;

	void generateYieldTables(ProjectionContext context, Polygon polygon, PolygonProjectionState state)
			throws YieldTableGenerationException;
}
