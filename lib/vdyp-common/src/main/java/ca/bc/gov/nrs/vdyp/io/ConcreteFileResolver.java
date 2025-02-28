package ca.bc.gov.nrs.vdyp.io;

import java.io.IOException;
import java.nio.file.Path;

public interface ConcreteFileResolver extends FileResolver {

	String toString(String filename) throws IOException;

	Path toPath(String filename) throws IOException;
}