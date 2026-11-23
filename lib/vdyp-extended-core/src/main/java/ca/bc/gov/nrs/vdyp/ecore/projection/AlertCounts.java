package ca.bc.gov.nrs.vdyp.ecore.projection;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;

/**
 * Error log message count and polygon count for each alert type (Error, Warning, Info). A polygon is counted once per
 * type, even if it has several messages of that type.
 */
public record AlertCounts(
		int errorMessageCount, int errorPolygonCount, int warningMessageCount, int warningPolygonCount,
		int infoMessageCount, int infoPolygonCount
) {

	public static final AlertCounts NONE = new AlertCounts(0, 0, 0, 0, 0, 0);

	/** F is counted as ERROR and S as INFO. */
	public enum AlertType {
		ERROR, WARNING, INFO;

		public static AlertType of(MessageSeverityCode severity) {
			return switch (severity) {
			case FATAL_ERROR, ERROR -> ERROR;
			case WARNING -> WARNING;
			case INFORMATION, STATUS -> INFO;
			};
		}
	}

	public AlertCounts plus(AlertCounts other) {
		if (other == null) {
			return this;
		}
		return new AlertCounts(
				errorMessageCount + other.errorMessageCount, errorPolygonCount + other.errorPolygonCount,
				warningMessageCount + other.warningMessageCount, warningPolygonCount + other.warningPolygonCount,
				infoMessageCount + other.infoMessageCount, infoPolygonCount + other.infoPolygonCount
		);
	}
}
