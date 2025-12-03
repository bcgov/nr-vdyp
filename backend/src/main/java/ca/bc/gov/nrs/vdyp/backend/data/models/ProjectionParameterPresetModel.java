package ca.bc.gov.nrs.vdyp.backend.data.models;

public class ProjectionParameterPresetModel {
	private String projectionParameterPresetGUID;
	private VDYPUserModel ownerUser;
	private String presetName;
	private String presetParameters;

	public String getProjectionParameterPresetGUID() {
		return projectionParameterPresetGUID;
	}

	public VDYPUserModel getOwnerUser() {
		return ownerUser;
	}

	public String getPresetName() {
		return presetName;
	}

	public String getPresetParameters() {
		return presetParameters;
	}

	public void setProjectionParameterPresetGUID(String projectionParameterPresetGUID) {
		this.projectionParameterPresetGUID = projectionParameterPresetGUID;
	}

	public void setOwnerUser(VDYPUserModel ownerUser) {
		this.ownerUser = ownerUser;
	}

	public void setPresetName(String presetName) {
		this.presetName = presetName;
	}

	public void setPresetParameters(String presetParameters) {
		this.presetParameters = presetParameters;
	}
}
