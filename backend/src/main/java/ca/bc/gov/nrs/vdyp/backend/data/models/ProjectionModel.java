package ca.bc.gov.nrs.vdyp.backend.data.models;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
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
}
