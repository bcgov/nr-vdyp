package ca.bc.gov.nrs.vdyp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public class CompositeFileResolver implements FileResolver {

	private final ConcreteFileResolver inputFileResolver;
	private final ConcreteFileResolver outputFileResolver;

	public CompositeFileResolver(ConcreteFileResolver inputFileResolver, ConcreteFileResolver outputFileResolver) {
		this.inputFileResolver = inputFileResolver;
		this.outputFileResolver = outputFileResolver;
	}

	@Override
	public InputStream resolveForInput(String filename) throws IOException {
		return inputFileResolver.resolveForInput(filename);
	}

	@Override
	public OutputStream resolveForOutput(String filename) throws IOException {
		return outputFileResolver.resolveForOutput(filename);
	}

	@Override
	public ConcreteFileResolver getOutputFileResolver() {
		return outputFileResolver;
	}

	@Override
	public ConcreteFileResolver getInputFileResolver() {
		return inputFileResolver;
	}

	public Path toInputPath(String fileName) throws IOException {
		return this.getInputFileResolver().toPath(fileName);
	}

	public Path toOutputPath(String fileName) throws IOException {
		return this.getOutputFileResolver().toPath(fileName);
	}
}