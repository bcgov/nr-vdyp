package ca.bc.gov.nrs.vdyp.backend.v1;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@OpenAPIDefinition(
		info = @Info(
				version = "8.0", title = "Variable Density Yield Projection API", description = "Create, configure, run, and retrieve Variable Density Yield Projection (VDYP) projections.", contact = @Contact(
						name = "Government of British Columbia, Natural Resource Information and Digital Services"
				), license = @License(name = "Apache License 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
		), security = @SecurityRequirement(name = VdypBackendApplication.BEARER_AUTH)
)
@SecurityScheme(
		securitySchemeName = VdypBackendApplication.BEARER_AUTH, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "OIDC access token. Use a non-production test identity for automated scans."
)
@ApplicationPath(VdypBackendApplication.APPLICATION_PATH)
public class VdypBackendApplication extends Application {

	public static final String APPLICATION_PATH = "/";
	public static final String BEARER_AUTH = "bearerAuth";

}
