package ca.bc.gov.nrs.vdyp.backend.projection;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTable;

public interface IComponentRunner {

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

	void generateYieldTableForPolygonLayer(
			YieldTable yieldTable, Polygon polygon, PolygonProjectionState state, LayerReportingInfo layerReportingInfo,
			boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException;

	void generateYieldTableForPolygon(
			YieldTable yieldTable, Polygon polygon, PolygonProjectionState state, boolean doGenerateDetailedTableHeader
	) throws YieldTableGenerationException;
}