package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class ProjectionModel {
	private String projectionGUID;
	private VDYPUserModel ownerUser;
	private FileMappingModel polygonFileMapping;
	private FileMappingModel layerFileMapping;
	private FileMappingModel resultFileMapping;
	private String projectionParameters;
	private OffsetDateTime startDate;
	private OffsetDateTime endDate;
	private CalculationEngineCodeModel calculationEngineCode;
	private ProjectionStatusCodeModel projectionStatusCode;
}
