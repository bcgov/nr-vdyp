package ca.bc.gov.nrs.vdyp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileResolver {

	InputStream resolveForInput(String filename) throws IOException;

	OutputStream resolveForOutput(String filename) throws IOException;

	FileResolver getOutputFileResolver();

	FileResolver getInputFileResolver();
}