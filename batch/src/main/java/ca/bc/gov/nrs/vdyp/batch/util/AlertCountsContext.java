package ca.bc.gov.nrs.vdyp.batch.util;

import org.springframework.batch.item.ExecutionContext;

import ca.bc.gov.nrs.vdyp.ecore.projection.AlertCounts;

public final class AlertCountsContext {

	private static final String ERROR_MESSAGE_COUNT_KEY = "errorMessageCount";
	private static final String ERROR_POLYGON_COUNT_KEY = "errorPolygonCount";
	private static final String WARNING_MESSAGE_COUNT_KEY = "warningMessageCount";
	private static final String WARNING_POLYGON_COUNT_KEY = "warningPolygonCount";
	private static final String INFO_MESSAGE_COUNT_KEY = "infoMessageCount";
	private static final String INFO_POLYGON_COUNT_KEY = "infoPolygonCount";

	private AlertCountsContext() {
	}

	// Missing values are read as 0 (e.g. a job started before this feature)
	public static AlertCounts read(ExecutionContext context, String prefix) {
		return new AlertCounts(
				context.getInt(prefix + ERROR_MESSAGE_COUNT_KEY, 0),
				context.getInt(prefix + ERROR_POLYGON_COUNT_KEY, 0),
				context.getInt(prefix + WARNING_MESSAGE_COUNT_KEY, 0),
				context.getInt(prefix + WARNING_POLYGON_COUNT_KEY, 0),
				context.getInt(prefix + INFO_MESSAGE_COUNT_KEY, 0), context.getInt(prefix + INFO_POLYGON_COUNT_KEY, 0)
		);
	}

	public static void write(ExecutionContext context, String prefix, AlertCounts counts) {
		context.putInt(prefix + ERROR_MESSAGE_COUNT_KEY, counts.errorMessageCount());
		context.putInt(prefix + ERROR_POLYGON_COUNT_KEY, counts.errorPolygonCount());
		context.putInt(prefix + WARNING_MESSAGE_COUNT_KEY, counts.warningMessageCount());
		context.putInt(prefix + WARNING_POLYGON_COUNT_KEY, counts.warningPolygonCount());
		context.putInt(prefix + INFO_MESSAGE_COUNT_KEY, counts.infoMessageCount());
		context.putInt(prefix + INFO_POLYGON_COUNT_KEY, counts.infoPolygonCount());
	}
}
