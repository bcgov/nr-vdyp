package ca.bc.gov.nrs.vdyp.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(
	{ FileMetadata.JSON_PROPERTY_FILENAME, FileMetadata.JSON_PROPERTY_CONTENT_TYPE,
			FileMetadata.JSON_PROPERTY_SIZE_IN_BYTES }
)
public class FileMetadata {

	public static final String JSON_PROPERTY_FILENAME = "filename";
	@JsonProperty(JSON_PROPERTY_FILENAME)
	private String filename;

	public static final String JSON_PROPERTY_CONTENT_TYPE = "contentType";
	@JsonProperty(JSON_PROPERTY_CONTENT_TYPE)
	private String contentType;

	public static final String JSON_PROPERTY_SIZE_IN_BYTES = "sizeBytes";
	@JsonProperty(JSON_PROPERTY_SIZE_IN_BYTES)
	private Integer size;

	public String getFilename() {
		return filename;
	}

	public String getContentType() {
		return contentType;
	}

	public Integer getSize() {
		return size;
	}
}
