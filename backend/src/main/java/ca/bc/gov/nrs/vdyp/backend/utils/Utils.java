package ca.bc.gov.nrs.vdyp.backend.utils;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	private static final Logger logger = LoggerFactory.getLogger(Utils.class);

	public static void sleep(long n_ms) {
		try {
			Thread.sleep(n_ms);
		} catch (InterruptedException e) {
			logger.error("Saw InterruptedException during \"sleep\" of " + n_ms + "ms");
			throw new RuntimeException("Unexpected InterruptException seen");
		}
	}

	public static <T extends Closeable> void close(T s, String streamName) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException e) {
				throw new RuntimeException(
						"Failed to close " + s.getClass().getName() + " stream with name " + streamName, e
				);
			}
		}
	}
}
