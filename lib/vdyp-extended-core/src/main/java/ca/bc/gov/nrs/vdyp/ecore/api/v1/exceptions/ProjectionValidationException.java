package ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions;

import java.util.List;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;

public class ProjectionValidationException extends AbstractProjectionRequestException {

	private static final long serialVersionUID = 1L;
	private final long featureID;

	public long getFeatureID() {
		return featureID;
	}

	public ProjectionValidationException(List<ValidationMessage> validationMessages) {
		super(validationMessages);
		featureID = 0;
	}

	public ProjectionValidationException(long featureId, List<ValidationMessage> validationMessages) {
		super(buildContextPrefix(featureId), validationMessages);
		this.featureID = featureId;
	}

	public ProjectionValidationException(long featureId, String layerId, List<ValidationMessage> validationMessages) {
		super(buildContextPrefix(featureId, layerId), validationMessages);
		this.featureID = featureId;
	}
}
