package ca.bc.gov.nrs.vdyp.backend.data.models;

import lombok.Data;

@Data
public class FileMappingModel {
	private String fileMappingGUID;
	private ProjectionFileSetModel projectionFileSet;
	private String comsObjectGUID;
}
