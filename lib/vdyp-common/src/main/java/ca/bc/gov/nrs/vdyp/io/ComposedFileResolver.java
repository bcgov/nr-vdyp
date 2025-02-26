package ca.bc.gov.nrs.vdyp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ComposedFileResolver implements FileResolver {

	private final ConcreteFileResolver inputFileResolver;
	private final ConcreteFileResolver outputFileResolver;

	public ComposedFileResolver(ConcreteFileResolver inputFileResolver, ConcreteFileResolver outputFileResolver) {
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
}