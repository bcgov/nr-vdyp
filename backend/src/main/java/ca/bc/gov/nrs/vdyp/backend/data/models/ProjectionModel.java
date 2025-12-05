package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.time.OffsetDateTime;

public class ProjectionModel {
	private String projectionGUID;
	private VDYPUserModel ownerUser;
	private ProjectionFileSetModel polygonFileSet;
	private ProjectionFileSetModel layerFileSet;
	private ProjectionFileSetModel resultFileSet;
	private String projectionParameters;
	private OffsetDateTime startDate;
	private OffsetDateTime endDate;
	private CalculationEngineCodeModel calculationEngineCode;
	private ProjectionStatusCodeModel projectionStatusCode;

	public String getProjectionGUID() {
		return projectionGUID;
	}

	public VDYPUserModel getOwnerUser() {
		return ownerUser;
	}

	public ProjectionFileSetModel getPolygonFileSet() {
		return polygonFileSet;
	}

	public ProjectionFileSetModel getLayerFileSet() {
		return layerFileSet;
	}

	public ProjectionFileSetModel getResultFileSet() {
		return resultFileSet;
	}

	public String getProjectionParameters() {
		return projectionParameters;
	}

	public OffsetDateTime getStartDate() {
		return startDate;
	}

	public OffsetDateTime getEndDate() {
		return endDate;
	}

	public CalculationEngineCodeModel getCalculationEngineCode() {
		return calculationEngineCode;
	}

	public ProjectionStatusCodeModel getProjectionStatusCode() {
		return projectionStatusCode;
	}

	public void setProjectionGUID(String projectionGUID) {
		this.projectionGUID = projectionGUID;
	}

	public void setOwnerUser(VDYPUserModel ownerUser) {
		this.ownerUser = ownerUser;
	}

	public void setPolygonFileSet(ProjectionFileSetModel polygonFileSet) {
		this.polygonFileSet = polygonFileSet;
	}

	public void setLayerFileSet(ProjectionFileSetModel layerFileSet) {
		this.layerFileSet = layerFileSet;
	}

	public void setResultFileSet(ProjectionFileSetModel resultFileSet) {
		this.resultFileSet = resultFileSet;
	}

	public void setProjectionParameters(String projectionParameters) {
		this.projectionParameters = projectionParameters;
	}

	public void setStartDate(OffsetDateTime startDate) {
		this.startDate = startDate;
	}

	public void setEndDate(OffsetDateTime endDate) {
		this.endDate = endDate;
	}

	public void setCalculationEngineCode(CalculationEngineCodeModel calculationEngineCode) {
		this.calculationEngineCode = calculationEngineCode;
	}

	public void setProjectionStatusCode(ProjectionStatusCodeModel projectionStatusCode) {
		this.projectionStatusCode = projectionStatusCode;
	}
}
