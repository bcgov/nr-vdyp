package ca.bc.gov.nrs.vdyp.backend.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;

public class FileHelper {

	public static final String STUBBED_RUNNER_DATA_FILES = "stubbed-runner-data-files";
	public static final String TEST_DATA_FILES = "test-data";
	public static final String HCSV = "hcsv";
	public static final String COMMON = "common";

	protected FileHelper() {
	}

	public static InputStream getStubResourceFile(String... pathComponents) {

		String resourceFilePath = Path.of(STUBBED_RUNNER_DATA_FILES, pathComponents).toString();
		return FileHelper.class.getClassLoader().getResourceAsStream(resourceFilePath);
	}

	public static InputStream getTestResourceFile(String... pathComponents) {

		String resourceFilePath = Path.of(TEST_DATA_FILES, pathComponents).toString();
		return FileHelper.class.getClassLoader().getResourceAsStream(resourceFilePath);
	}

	public static InputStream getForReading(Path filePath) throws IOException {
		return Files.newInputStream(filePath, StandardOpenOption.READ);
	}

	public static void delete(Path debugLogPath) {

		try {
			Files.delete(debugLogPath);
		} catch (IOException e) {
			throw new IllegalStateException(
					MessageFormat.format("Unable to delete debug log {0}", debugLogPath.toString()), e
			);
		}
	}
}
