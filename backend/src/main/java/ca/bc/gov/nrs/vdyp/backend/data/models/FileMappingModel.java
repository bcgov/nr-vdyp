package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.net.URL;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class FileMappingModel {
	private String fileMappingGUID;
	private ProjectionFileSetModel projectionFileSet;
	private String comsObjectGUID;
	private URL downloadURL;

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

	public URL getDownloadURL() {
		return this.downloadURL;
	}

	public void setDownloadURL(URL downloadURL) {
		this.downloadURL = downloadURL;
	}
}
