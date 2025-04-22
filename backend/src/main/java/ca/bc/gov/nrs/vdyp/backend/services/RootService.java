package ca.bc.gov.nrs.vdyp.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.responses.v1.RootResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class RootService {

	private static final Logger logger = LoggerFactory.getLogger(RootService.class);

	public RootResource rootGet(UriInfo uriInfo, SecurityContext securityContext) {

		logger.info("<rootGet");
		logger.info(">rootGet");

		return RootResource.of(uriInfo);
	}
}
