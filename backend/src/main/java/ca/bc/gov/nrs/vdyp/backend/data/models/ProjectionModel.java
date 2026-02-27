package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.time.OffsetDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ProjectionModel {
	public static final int DAYS_UNTIL_EXPIRY = 30;

	private String projectionGUID;
	private VDYPUserModel ownerUser;
	private ProjectionFileSetModel polygonFileSet;
	private ProjectionFileSetModel layerFileSet;
	private ProjectionFileSetModel resultFileSet;
	private String projectionParameters;
	private String modelParameters;
	private OffsetDateTime startDate;
	private OffsetDateTime endDate;
	private CalculationEngineCodeModel calculationEngineCode;
	private ProjectionStatusCodeModel projectionStatusCode;
	private OffsetDateTime lastUpdatedDate;
	private OffsetDateTime expiryDate;
	private OffsetDateTime createDate;
	private String reportTitle;
	private String reportDescription;
	private ProjectionBatchMappingModel batchMapping;

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

	public String getModelParameters() {
		return modelParameters;
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

	public OffsetDateTime getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public OffsetDateTime getCreateDate() {
		return createDate;
	}

	public OffsetDateTime getExpiryDate() {
		return expiryDate;
	}

	public String getReportTitle() {
		return reportTitle;
	}

	public String getReportDescription() {
		return reportDescription;
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

	public void setModelParameters(String modelParameters) {
		this.modelParameters = modelParameters;
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

	public void setLastUpdatedDate(OffsetDateTime lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	public void setCreateDate(OffsetDateTime createDate) {
		this.createDate = createDate;
	}

	public void setExpiryDate(OffsetDateTime expiryDate) {
		this.expiryDate = expiryDate;
	}

	public void setReportDescription(String reportDescription) {
		this.reportDescription = reportDescription;
	}

	public void setReportTitle(String reportTitle) {
		this.reportTitle = reportTitle;
	}

	public void setBatchMapping(ProjectionBatchMappingModel batchMapping) {
		this.batchMapping = batchMapping;
	}

	public ProjectionBatchMappingModel getBatchMapping() {
		return batchMapping;
	}

}
