package ca.bc.gov.nrs.api.v1.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class FileHelperTest {

	@Test
	void testGetStubResourceFile() {
		InputStream is = FileHelper.getStubResourceFile("Output_Log.txt");
		Assert.assertNotNull(is);
	}

	@Test
	void testGetTestResourceFile() {
		InputStream is = FileHelper.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "Output_Log.txt");
		Assert.assertNotNull(is);
	}

	@Test
	void testGetAndDeleteFile() throws IOException {
		Path tempFilePath = Files.createTempFile("pre_", "_post");

		InputStream is1 = FileHelper.getForReading(tempFilePath);
		Assert.assertNotNull(is1);

		FileHelper.delete(tempFilePath);

		Assert.assertThrows(NoSuchFileException.class, () -> FileHelper.getForReading(tempFilePath));

		Assert.assertThrows(IllegalStateException.class, () -> FileHelper.delete(tempFilePath));
	}
}
