package ca.bc.gov.nrs.vdyp.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * File resolver that delegates to a list of delegates in descending order of priority
 */
public class FailoverFileResolver implements FileResolver {
	private List<FileResolver> delegates;

	public FailoverFileResolver(FileResolver... delegates) {
		this(Arrays.asList(delegates));
	}

	public FailoverFileResolver(List<FileResolver> delegates) {
		this.delegates = delegates;
	}

	@Override
	public InputStream resolveForInput(String filename) throws IOException {
		for (var delegate : delegates) {
			try {
				return delegate.resolveForInput(filename);
			} catch (NoSuchFileException | FileNotFoundException ex) {
				// Do Nothing
			}
		}
		throw new FileNotFoundException(filename);
	}

	@Override
	public OutputStream resolveForOutput(String filename) throws IOException {
		for (var delegate : delegates) {
			try {
				return delegate.resolveForOutput(filename);
			} catch (NoSuchFileException | FileNotFoundException ex) {
				// Do Nothing
			}
		}
		throw new FileNotFoundException(filename);
	}

	@Override
	public String toString(String filename) throws IOException {
		return delegates.iterator().next().toString(filename);
	}

	@Override
	public Path toPath(String filename) throws IOException {
		return delegates.iterator().next().toPath(filename);
	}

	@Override
	public FileResolver relative(String path) throws IOException {
		List<FileResolver> result = new ArrayList<>(delegates.size());
		for (var delegate : delegates) {
			result.add(delegate.relative(path));
		}
		return new FailoverFileResolver(result);
	}

	@Override
	public FileResolver relativeToParent(String path) throws IOException {
		List<FileResolver> result = new ArrayList<>(delegates.size());
		for (var delegate : delegates) {
			result.add(delegate.relativeToParent(path));
		}
		return new FailoverFileResolver(result);
	}

}
