package ca.bc.gov.nrs.vdyp.backend.v1.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;

public class FileHelper {

	protected FileHelper() {
	}

	public static InputStream getStubResourceFile(String fileName) throws IOException {

		String resourceFilePath = "VDYP7Console-sample-files/hcsv/vdyp-240/" + fileName;
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
