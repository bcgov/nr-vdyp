package ca.bc.gov.nrs.vdyp.backend.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "vdyp.coms.s3")
public interface COMSS3Config {
	String accessId();

	String secretAccessKey();

	String bucket();

	String endpoint();
}
