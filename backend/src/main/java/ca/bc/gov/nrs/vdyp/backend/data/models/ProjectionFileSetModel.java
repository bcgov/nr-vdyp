package ca.bc.gov.nrs.vdyp.backend.data.models;

import lombok.Data;

@Data
public class ProjectionFileSetModel {
	private String projectionFileSetGUID;
	private FileSetTypeCodeModel fileSetTypeCode;
	private String fileSetName;
}
