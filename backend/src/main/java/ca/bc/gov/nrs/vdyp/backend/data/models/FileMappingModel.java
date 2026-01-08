package ca.bc.gov.nrs.vdyp.backend.data.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class FileMappingModel {
	private String fileMappingGUID;
	private ProjectionFileSetModel projectionFileSet;
	private String comsObjectGUID;
	private String downloadURL;

	public String getFileMappingGUID() {
		return fileMappingGUID;
	}

	public ProjectionFileSetModel getProjectionFileSet() {
		return projectionFileSet;
	}

	public String getComsObjectGUID() {
		return comsObjectGUID;
	}

	public void setFileMappingGUID(String fileMappingGUID) {
		this.fileMappingGUID = fileMappingGUID;
	}

	public void setProjectionFileSet(ProjectionFileSetModel projectionFileSet) {
		this.projectionFileSet = projectionFileSet;
	}

	public void setComsObjectGUID(String comsObjectGUID) {
		this.comsObjectGUID = comsObjectGUID;
	}

	public String getDownloadURL() {
		return this.downloadURL;
	}

	public void setDownloadURL(String downloadURL) {
		this.downloadURL = downloadURL;
	}
}
