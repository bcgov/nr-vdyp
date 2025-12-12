package ca.bc.gov.nrs.api.v1.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;

public class FileHelperTest {

	@Test
	void testGetStubResourceFile() throws IOException {
		try (InputStream is = FileHelper.getStubResourceFile("Output_Log.txt");) {
			assertNotNull(is);
		}
	}

	@Test
	void testGetTestResourceFile() throws IOException {
		try (InputStream is = FileHelper.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "Output_Log.txt");) {
			assertNotNull(is);
		}
	}

	@Test
	void testGetAndDeleteFile() throws IOException {
		Path tempFilePath = Files.createTempFile("pre_", "_post");

		try (InputStream is1 = FileHelper.getForReading(tempFilePath);) {
			assertNotNull(is1);
		}

		FileHelper.delete(tempFilePath);

		assertThrows(NoSuchFileException.class, () -> FileHelper.getForReading(tempFilePath));

		assertThrows(IllegalStateException.class, () -> FileHelper.delete(tempFilePath));
	}
}
