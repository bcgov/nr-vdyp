package ca.bc.gov.nrs.vdyp.backend.projection;

import ca.bc.gov.nrs.vdyp.backend.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable.YieldTable;

public class StubComponentRunner implements IComponentRunner {

	@Override
	public void runFipStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		state.setProcessingResults(
				ProjectionStageCode.Initial, projectionTypeCode, ProcessingResult.RETURN_CODE_SUCCESS,
				ProcessingResult.RUN_CODE_SUCCESS
		);
	}

	@Override
	public void runVriStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		state.setProcessingResults(
				ProjectionStageCode.Initial, projectionTypeCode, ProcessingResult.RETURN_CODE_SUCCESS,
				ProcessingResult.RUN_CODE_SUCCESS
		);
	}

	@Override
	public void runAdjust(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		state.setProcessingResults(
				ProjectionStageCode.Adjust, projectionTypeCode, ProcessingResult.RETURN_CODE_SUCCESS,
				ProcessingResult.RUN_CODE_SUCCESS
		);
	}

	@Override
	public void runForward(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		state.setProcessingResults(
				ProjectionStageCode.Forward, projectionTypeCode, ProcessingResult.RETURN_CODE_SUCCESS,
				ProcessingResult.RUN_CODE_SUCCESS
		);
	}

	@Override
	public void runBack(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {
		state.setProcessingResults(
				ProjectionStageCode.Back, projectionTypeCode, ProcessingResult.RETURN_CODE_SUCCESS,
				ProcessingResult.RUN_CODE_SUCCESS
		);
	}

	@Override
	public void generateYieldTableForPolygonLayer(
			YieldTable yieldTable, Polygon polygon, PolygonProjectionState state, LayerReportingInfo layerReportingInfo,
			boolean doGenerateDetailedTableHeader
	) {
		// Do nothing
	}

	@Override
	public void generateYieldTableForPolygon(
			YieldTable yieldTable, Polygon polygon, PolygonProjectionState state, boolean doGenerateDetailedTableHeader
	) {
		// Do nothing
	}
}
