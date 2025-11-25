package ca.bc.gov.nrs.vdyp.backend.data.models;

import lombok.Data;

@Data
public class ProjectionParameterPresetModel {
	private String projectionParameterPresetGUID;
	private VDYPUserModel ownerUser;
	private String presetName;
	private String presetParameters;
}
