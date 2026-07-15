package ca.bc.gov.nrs.vdyp.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;

/**
 * Test related utilities that need to be in the ca.bc.gov.nrs.vdyp.application package for visibility
 */
public class ApplicationTestUtils {
	/**
	 * Allows tests to set the control map of a VdypStartApplication
	 *
	 * @param app
	 * @param controlMap
	 */
	public static void setControlMap(VdypStartApplication<?, ?, ?, ?, ?> app, Map<String, Object> controlMap) {
		app.setControlMap(controlMap);
	}

	public static void runInit(
			VdypApplication<?> app, FileSystemFileResolver resolver, PrintStream writeToIfNoArgs,
			InputStream readFromIfNoArgs, String... controlFilePaths
	) throws IOException, ResourceParseException {
		app.init(resolver, writeToIfNoArgs, readFromIfNoArgs, controlFilePaths);
	}

	public static void runInit(VdypApplication<?> app, FileSystemFileResolver resolver, Map<String, Object> controlMap)
			throws ProcessingException, IOException {
		app.init(resolver, controlMap);
	}

	public static void runProcess(VdypApplication<?> app) throws ProcessingException {
		app.process();
	}
}
