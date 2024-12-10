package ca.bc.gov.nrs.vdyp.backend.v1.gen;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition(
		info = @org.eclipse.microprofile.openapi.annotations.info.Info(
				version = "1.0.0", title = "Variable Density Yield Projection", description = "API for the Variable Density Yield Projection service"

		)
)
@ApplicationPath(RestResourceRoot.APPLICATION_PATH)
public class RestApplication extends Application {

}
