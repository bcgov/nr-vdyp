package ca.bc.gov.nrs.vdyp.ecore.model.v1;

public enum StandYieldMessageKind {
	AGE_OUT_OF_RANGE("stand total age is not in the range {0} to {1}, inclusive"), //
	YEAR_OUT_OF_RANGE("calendar year must be at least zero");

	public enum Category {
		ERROR, WARNING, INFO
	};

	public final String template;

	StandYieldMessageKind(String template) {
		this.template = template;
	}

	public String getTemplate() {
		return template;
	}
}
