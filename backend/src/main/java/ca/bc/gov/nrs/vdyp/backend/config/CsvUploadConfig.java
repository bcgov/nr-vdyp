package ca.bc.gov.nrs.vdyp.backend.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "vdyp.upload.csv")
public interface CsvUploadConfig {
	@WithDefault("1000000000")
	long maxFileSizeBytes();

	@WithDefault("512")
	int maxColumns();

	@WithDefault("1048576")
	int maxFieldChars();

	@WithDefault("10485760")
	int maxRecordBytes();
}
