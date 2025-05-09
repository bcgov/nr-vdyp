package ca.bc.gov.nrs.vdyp.backend.v1;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition(
		info = @org.eclipse.microprofile.openapi.annotations.info.Info(
				version = "1.0.0", title = "Variable Density Yield Projection", description = "API for the Variable Density Yield Projection service"

		)
)
@ApplicationPath(VdypBackendApplication.APPLICATION_PATH)
public class VdypBackendApplication extends Application {

	public static final String APPLICATION_PATH = "/";

}
