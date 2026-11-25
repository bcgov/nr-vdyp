package ca.bc.gov.nrs.vdyp.io;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A read only {@link FileResolver} over a set of files held in memory, such as the entries of a zip archive. File names
 * use '/' as the separator and are resolved relative to the resolver's current directory.
 * <p>
 * A name that does not match a file exactly is matched ignoring case, provided only one file matches. The control files
 * were written for case insensitive file systems and do not always use the same case as the files they name.
 */
public class InMemoryFileResolver implements FileResolver {

	private final Map<String, byte[]> files;
	/** File paths keyed by their lower case form; paths that differ only in case map to null (ambiguous) */
	private final Map<String, String> pathsIgnoringCase;
	private final String currentDirectory;

	/**
	 * @param files file content keyed by path, using '/' as the separator and no leading '/'
	 */
	public InMemoryFileResolver(Map<String, byte[]> files) {
		this(Collections.unmodifiableMap(new HashMap<>(files)), indexIgnoringCase(files.keySet()), "");
	}

	private InMemoryFileResolver(
			Map<String, byte[]> files, Map<String, String> pathsIgnoringCase, String currentDirectory
	) {
		this.files = files;
		this.pathsIgnoringCase = pathsIgnoringCase;
		this.currentDirectory = currentDirectory;
	}

	private static Map<String, String> indexIgnoringCase(Set<String> paths) {
		Map<String, String> index = new HashMap<>();
		for (var path : paths) {
			var key = path.toLowerCase(Locale.ROOT);
			if (index.containsKey(key)) {
				index.put(key, null); // Ambiguous, so only an exact match will do
			} else {
				index.put(key, path);
			}
		}
		return Collections.unmodifiableMap(index);
	}

	/**
	 * Read every file entry of a zip archive into memory.
	 *
	 * @param zip the archive content; it is read to the end but not closed
	 */
	public static InMemoryFileResolver fromZip(InputStream zip) throws IOException {
		Map<String, byte[]> files = new HashMap<>();
		var zis = new ZipInputStream(zip);
		ZipEntry entry;
		while ( (entry = zis.getNextEntry()) != null) {
			if (!entry.isDirectory() && !entry.getName().startsWith("__MACOSX")) {
				files.put(normalize(entry.getName()), zis.readAllBytes());
			}
		}
		return new InMemoryFileResolver(files);
	}

	/**
	 * Normalize a path without using the platform file system: accept '/' or '\' as separators, drop empty and "."
	 * segments, and apply ".." segments, never going above the root.
	 */
	static String normalize(String path) {
		Deque<String> segments = new ArrayDeque<>();
		for (var segment : path.replace('\\', '/').split("/")) {
			if (segment.isEmpty() || segment.equals(".")) {
				continue;
			}
			if (segment.equals("..")) {
				segments.pollLast();
			} else {
				segments.addLast(segment);
			}
		}
		return String.join("/", segments);
	}

	private String resolve(String filename) {
		return normalize(currentDirectory.isEmpty() ? filename : currentDirectory + "/" + filename);
	}

	@Override
	public InputStream resolveForInput(String filename) throws IOException {
		var path = resolve(filename);
		var content = files.get(path);
		if (content == null) {
			var matchIgnoringCase = pathsIgnoringCase.get(path.toLowerCase(Locale.ROOT));
			if (matchIgnoringCase != null) {
				content = files.get(matchIgnoringCase);
			}
		}
		if (content == null) {
			throw new FileNotFoundException(toString(filename));
		}
		return new ByteArrayInputStream(content);
	}

	@Override
	public OutputStream resolveForOutput(String filename) throws IOException {
		throw new UnsupportedOperationException("InMemoryFileResolver is read only; cannot write " + filename);
	}

	@Override
	public String toString(String filename) {
		return String.format("memory:%s", resolve(filename));
	}

	@Override
	public Path toPath(String filename) {
		return Path.of(resolve(filename));
	}

	@Override
	public FileResolver relative(String path) {
		return new InMemoryFileResolver(files, pathsIgnoringCase, resolve(path));
	}

	@Override
	public FileResolver relativeToParent(String path) {
		var resolved = resolve(path);
		int lastSlash = resolved.lastIndexOf('/');
		return new InMemoryFileResolver(
				files, pathsIgnoringCase, lastSlash < 0 ? "" : resolved.substring(0, lastSlash)
		);
	}

	@Override
	public String toString() {
		return "InMemoryFileResolver (" + files.size() + " files, relative [" + currentDirectory + "])";
	}
}
