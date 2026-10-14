package ca.bc.gov.nrs.vdyp.logging;

import org.slf4j.MDC;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

/** Enables calculation DEBUG events only within the requesting projection's thread. */
public class RequestDebugLoggingFilter extends TurboFilter {

	public static final String MDC_KEY = "vdypDebugLog";

	@Override
	public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
		if (level != Level.DEBUG) {
			return FilterReply.NEUTRAL;
		}
		String requestId = MDC.get(MDC_KEY);
		if (requestId != null) {
			return "disabled".equals(requestId) ? FilterReply.DENY : FilterReply.ACCEPT;
		}
		String name = logger.getName();
		boolean calculationLogger = name.startsWith("ca.bc.gov.nrs.vdyp.")
				&& !name.startsWith("ca.bc.gov.nrs.vdyp.batch.") && !name.startsWith("ca.bc.gov.nrs.vdyp.backend.");
		return calculationLogger ? FilterReply.DENY : FilterReply.NEUTRAL;
	}
}
