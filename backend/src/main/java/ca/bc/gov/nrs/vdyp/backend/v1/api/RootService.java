package ca.bc.gov.nrs.vdyp.backend.v1.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.v1.api.impl.exceptions.NotFoundException;
import ca.bc.gov.nrs.vdyp.backend.v1.gen.responses.RootResource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class RootService {

	private static final Logger logger = LoggerFactory.getLogger(RootService.class);

	public RootResource rootGet(UriInfo uriInfo, SecurityContext securityContext) throws NotFoundException {

		logger.info("<rootGet");
		logger.info(">rootGet");

		return RootResource.of(uriInfo);
	}
}
