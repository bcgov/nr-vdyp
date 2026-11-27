package ca.bc.gov.nrs.vdyp.controlmap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;

/**
 * Process wide cache of shared control maps. A shared control map holds the definitions and coefficients read from a
 * set of control files (see {@link BaseControlParser#parseSharedByName}). It is built once per source and control file
 * parser class, then copied cheaply for each run with {@link BaseControlParser#parseForRun}, so that the definition and
 * coefficient files are not read again for every polygon, layer and stage.
 * <p>
 * The cached maps are unmodifiable, and the parsed objects in them are shared by every run in every thread. Code that
 * uses a control map must therefore treat the parsed definitions and coefficients as read only.
 */
public final class ControlMapCache {

	private static final Logger logger = LoggerFactory.getLogger(ControlMapCache.class);

	private static final ControlMapCache INSTANCE = new ControlMapCache();

	/**
	 * Identifies the control files a shared map is built from.
	 *
	 * @param id               a name that is unique for this set of files, for example the archive they come from
	 * @param resolver         locates the control files and the files they name
	 * @param controlFileNames the control files to read, in order
	 */
	public record Source(String id, FileResolver resolver, List<String> controlFileNames) {
		public Source {
			controlFileNames = List.copyOf(controlFileNames);
		}
	}

	private record Key(String sourceId, List<String> controlFileNames, Class<?> parserClass) {
	}

	private final Map<Key, Map<String, Object>> sharedMaps = new ConcurrentHashMap<>();

	public static ControlMapCache getInstance() {
		return INSTANCE;
	}

	/**
	 * Get the shared control map for the given source and parser, parsing it on first use.
	 *
	 * @param source the control files to read
	 * @param parser the parser for the application that will use the map; its class is part of the cache key, since
	 *               each application reads a different set of entries (and applies RD_E198 modifiers for its own
	 *               program)
	 * @return an unmodifiable shared control map
	 */
	public Map<String, Object> getShared(Source source, BaseControlParser<?> parser)
			throws IOException, ResourceParseException {
		var key = new Key(source.id(), source.controlFileNames(), parser.getClass());
		try {
			return sharedMaps.computeIfAbsent(key, k -> {
				logger.info(
						"Building shared control map for {} from {} {}", parser.getClass().getSimpleName(), source.id(),
						source.controlFileNames()
				);
				try {
					return parser.parseSharedByName(source.controlFileNames(), source.resolver());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				} catch (ResourceParseException e) {
					throw new SharedParseException(e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		} catch (SharedParseException e) {
			throw e.getCause();
		}
	}

	/**
	 * Build the control map for one run: the shared map for the source and parser, with the run's own control files
	 * applied and its input and output files resolved with {@code runResolver}.
	 *
	 * @param source              the shared control files
	 * @param parser              the parser for the application that will use the map
	 * @param runControlFileNames control files for this run only, such as a per stand control file
	 * @param runResolver         resolver for this run's control files, input files and output files
	 * @return a new control map for the run
	 */
	public Map<String, Object> createRunControlMap(
			Source source, BaseControlParser<?> parser, List<String> runControlFileNames, FileResolver runResolver
	) throws IOException, ResourceParseException {
		return parser.parseForRun(getShared(source, parser), runControlFileNames, runResolver);
	}

	/**
	 * Discard all cached maps. Intended for tests.
	 */
	public void clear() {
		sharedMaps.clear();
	}

	private static class SharedParseException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		SharedParseException(ResourceParseException cause) {
			super(cause);
		}

		@Override
		public synchronized ResourceParseException getCause() {
			return (ResourceParseException) super.getCause();
		}
	}
}
