package ca.bc.gov.nrs.vdyp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FileSystemFileResolver implements ConcreteFileResolver {

	private Optional<Path> currentDirectory;

	public FileSystemFileResolver(Path currentDirectory) {
		this.currentDirectory = Optional.of(currentDirectory);
	}

	public FileSystemFileResolver() {
		this.currentDirectory = Optional.empty();
	}

	@Override
	public Path toPath(String filename) {
		return currentDirectory.map(x -> x.resolve(filename)).orElseGet(() -> Path.of(filename).toAbsolutePath());
	}

	@Override
	public InputStream resolveForInput(String filename) throws IOException {
		return Files.newInputStream(toPath(filename));
	}

	@Override
	public OutputStream resolveForOutput(String filename) throws IOException {
		return Files.newOutputStream(toPath(filename));
	}

	@Override
	public String toString(String filename) throws IOException {
		return String.format("file:%s", toPath(filename));
	}

	public FileSystemFileResolver relative(String path) throws IOException {
		return new FileSystemFileResolver(toPath(path));
	}

	@Override
	public FileResolver getOutputFileResolver() {
		return this;
	}

	@Override
	public FileResolver getInputFileResolver() {
		return this;
	}

	@Override
	public String toString() {
		return "FileSystemFileResolver ("
				+ (currentDirectory.isEmpty() ? "absolute" : "relative [" + currentDirectory.get() + "]") + ")";
	}
}