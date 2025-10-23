package ca.bc.gov.nrs.vdyp.backend.data.models;

import lombok.Data;

@Data
public class FileMappingModel {
	private String fileMappingGUID;
	private ProjectionFileSetModel projectionFileSetModel;
	private String comsObjectGUID;
}
