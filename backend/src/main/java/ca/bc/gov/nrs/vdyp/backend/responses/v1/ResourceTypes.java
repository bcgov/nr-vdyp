package ca.bc.gov.nrs.vdyp.backend.responses.v1;

public class ResourceTypes {
	public static final String NAMESPACE = VdypApiResource.NAMESPACE;

	public static final String ROOT_RESOURCE_NAME = "rootResource";
	public static final String ROOT_RESOURCE = NAMESPACE + ROOT_RESOURCE_NAME;

	public static final String HELP_RESOURCE_NAME = "helpResource";
	public static final String HELP_RESOURCE = NAMESPACE + HELP_RESOURCE_NAME;

	public static final String PROJECTION_RESOURCE_NAME = "projectionResource";
	public static final String PROJECTION_RESOURCE = NAMESPACE + PROJECTION_RESOURCE_NAME;

	public static final String VALIDATION_MESSAGE_RESOURCE_NAME = "validationMessageResource";
	public static final String VALIDATION_MESSAGE_RESOURCE = NAMESPACE + VALIDATION_MESSAGE_RESOURCE_NAME;

	public static final String VALIDATION_MESSAGE_LIST_RESOURCE_NAME = "validationMessageListResource";
	public static final String VALIDATION_MESSAGE_LIST_RESOURCE = NAMESPACE + VALIDATION_MESSAGE_LIST_RESOURCE_NAME;

	public static final String HCSV_PROJECTION = NAMESPACE + "hcsvProjection";
	public static final String DCSV_PROJECTION = NAMESPACE + "dcsvProjection";
	public static final String SCSV_PROJECTION = NAMESPACE + "scsvProjection";
}