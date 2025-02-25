package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.IOException;
import java.nio.file.Path;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.fip.FipStart;
import ca.bc.gov.nrs.vdyp.forward.VdypForwardApplication;
import ca.bc.gov.nrs.vdyp.vri.VriStart;

public class ComponentRunner implements IComponentRunner {

	@Override
	public void runFipStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		try {
			Path controlFilePath = Path
					.of(state.getExecutionFolder().toString(), projectionTypeCode.toString(), "FIPSTART.CTR");
			FipStart.main(controlFilePath.toAbsolutePath().toString());
		} catch (IOException e) {
			throw new PolygonExecutionException("Encountered exception while running FIPSTART", e);
		}
		state.setInitialProcessingResults(projectionTypeCode, 0, new ProcessingResultsCode(-99));
	}

	@Override
	public void runVriStart(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {
		try {
			Path controlFilePath = Path
					.of(state.getExecutionFolder().toString(), projectionTypeCode.toString(), "VRISTART.CTR");
			VriStart.main(controlFilePath.toAbsolutePath().toString());
		} catch (IOException e) {
			throw new PolygonExecutionException("Encountered exception while running VRISTART", e);
		}

		state.setInitialProcessingResults(projectionTypeCode, 0, new ProcessingResultsCode(-99));
	}

	@Override
	public void runAdjust(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state) {

		// ADJUST is not currently used - skipping
	}

	@Override
	public void runForward(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		try {
			Path controlFilePath = Path
					.of(state.getExecutionFolder().toString(), projectionTypeCode.toString(), "VDYP.CTR");

			VdypForwardApplication app = new VdypForwardApplication();
			app.doMain(controlFilePath.toAbsolutePath().toString());
		} catch (Exception e) {
			throw new PolygonExecutionException("Encountered exception while running ForwardApplication", e);
		}
	}

	@Override
	public void runBack(Polygon polygon, ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		try {
			// TODO: BACK is not supported yet.
			
			Path controlFilePath = Path
					.of(state.getExecutionFolder().toString(), projectionTypeCode.toString(), "VDYPBACK.CTR");

			// VdypBackApplication app = new VdypBackApplication();
			// app.doMain(controlFilePath.toAbsolutePath().toString());
		} catch (Exception e) {
			throw new PolygonExecutionException("Encountered exception while running BackApplication", e);
		}
	}
}
