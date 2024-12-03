package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionRequestValidator;
import jakarta.ws.rs.WebApplicationException;

public class ParameterValidationTest {

	@Test
	public void testParametersProvider() throws WebApplicationException, IOException {
		Parameters op = new Parameters();
		
		ProjectionRequestValidator r;
	}
}
