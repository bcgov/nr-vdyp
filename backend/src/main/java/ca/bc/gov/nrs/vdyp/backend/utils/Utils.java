package ca.bc.gov.nrs.vdyp.backend.utils;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.projection.model.Vdyp7Constants;

public class Utils {

	private static final Logger logger = LoggerFactory.getLogger(Utils.class);

	public static <T extends Closeable> void close(T s, String streamName) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException e) {
				logger.error("Saw IOException during close of " + streamName, e);
				throw new RuntimeException(
						"Failed to close " + s.getClass().getName() + " stream with name " + streamName, e
				);
			}
		}
	}

	/**
	 * @param d a (possibly null) Double
	 * @return if null, return -9.0 (the VDYP7 standard) and otherwise return <code>d</code>
	 */
	public static double safeGet(Double d) {
		return d == null ? Vdyp7Constants.EMPTY_DECIMAL : d;
	}

	/**
	 * @param i a (possibly null) Integer
	 * @return if null, return -9 (the VDYP7 standard) and otherwise return <code>i</code>
	 */
	public static int safeGet(Integer i) {
		return i == null ? Vdyp7Constants.EMPTY_INT : i;
	}
}
