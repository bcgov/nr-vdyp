package ca.bc.gov.nrs.vdyp.ecore.projection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.controlmap.ControlMapCache;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.InMemoryFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;

/**
 * Control maps built from the execution folder template (<code>ExecutionFolderTemplate.zip</code>).
 * <p>
 * The template's control files and coefficient files are read from the class path once per process. The definitions and
 * coefficients are parsed once per application, through {@link ControlMapCache}, and shared by every polygon,
 * projection type and stage. Only the input and output files of a run, and its per stand control file, are resolved
 * against the run's execution folder.
 */
public final class TemplateControlMaps {

	public static final String EXECUTION_FOLDER_TEMPLATE_ZIP_FILE_NAME = "ExecutionFolderTemplate.zip";

	private static volatile InMemoryFileResolver templateResolver;

	private TemplateControlMaps() {
	}

	/**
	 * @return a resolver over the files of the execution folder template, loaded on first use
	 */
	static FileResolver templateResolver() throws IOException {
		var resolver = templateResolver;
		if (resolver == null) {
			synchronized (TemplateControlMaps.class) {
				resolver = templateResolver;
				if (resolver == null) {
					try (
							var is = TemplateControlMaps.class.getClassLoader()
									.getResourceAsStream(EXECUTION_FOLDER_TEMPLATE_ZIP_FILE_NAME)
					) {
						if (is == null) {
							throw new FileNotFoundException(
									"Class path resource " + EXECUTION_FOLDER_TEMPLATE_ZIP_FILE_NAME + " not found"
							);
						}
						resolver = InMemoryFileResolver.fromZip(is);
					}
					templateResolver = resolver;
				}
			}
		}
		return resolver;
	}

	/**
	 * Build the control map for one run of an application.
	 *
	 * @param parser              a new control file parser for the application
	 * @param templateControlFile the template control file for the application, for example <code>VDYP.CTR</code>
	 * @param runControlFiles     control files for this run only, in the run's folder, applied over the template
	 * @param runResolver         resolver for the run's folder, which holds its input files and receives its output
	 * @return a new control map for the run. The definitions and coefficients in it are shared and must not be
	 *         modified.
	 */
	public static Map<String, Object> forRun(
			BaseControlParser<?> parser, String templateControlFile, List<String> runControlFiles,
			FileResolver runResolver
	) throws IOException, ResourceParseException {
		return ControlMapCache.getInstance()
				.createRunControlMap(source(templateControlFile), parser, runControlFiles, runResolver);
	}

	/**
	 * @param templateControlFile a control file in the execution folder template
	 * @return the cache source for that control file
	 */
	static ControlMapCache.Source source(String templateControlFile) throws IOException {
		return new ControlMapCache.Source(
				EXECUTION_FOLDER_TEMPLATE_ZIP_FILE_NAME, templateResolver(), List.of(templateControlFile)
		);
	}
}
