package ca.bc.gov.nrs.vdyp.backend.data.models;

public class ProjectionFileSetModel {
	private String projectionFileSetGUID;
	private FileSetTypeCodeModel fileSetTypeCode;
	private String fileSetName;

	public String getProjectionFileSetGUID() {
		return projectionFileSetGUID;
	}

	public FileSetTypeCodeModel getFileSetTypeCode() {
		return fileSetTypeCode;
	}

	public String getFileSetName() {
		return fileSetName;
	}

	public void setProjectionFileSetGUID(String projectionFileSetGUID) {
		this.projectionFileSetGUID = projectionFileSetGUID;
	}

	public void setFileSetTypeCode(FileSetTypeCodeModel fileSetTypeCode) {
		this.fileSetTypeCode = fileSetTypeCode;
	}

	public void setFileSetName(String fileSetName) {
		this.fileSetName = fileSetName;
	}
}
