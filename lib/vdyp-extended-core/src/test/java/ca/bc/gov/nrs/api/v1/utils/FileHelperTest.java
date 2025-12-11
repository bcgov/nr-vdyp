package ca.bc.gov.nrs.api.v1.utils;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.exists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;

class FileHelperTest {

	@TempDir
	protected static Path temp;

	@Test
	void testGetStubResourceFile() {
		InputStream is = FileHelper.getStubResourceFile("Output_Log.txt");
		assertNotNull(is);
	}

	@Test
	void testGetTestResourceFile() {
		InputStream is = FileHelper.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "Output_Log.txt");
		assertNotNull(is);
	}

	@Test
	void testGetAndDeleteFile() throws IOException {
		Path tempFilePath = Files.createFile(temp.resolve("tempfile"));

		InputStream is1 = FileHelper.getForReading(tempFilePath);
		assertNotNull(is1);

		FileHelper.delete(tempFilePath);

		assertThrows(NoSuchFileException.class, () -> FileHelper.getForReading(tempFilePath));

		assertThrows(IllegalStateException.class, () -> FileHelper.delete(tempFilePath));

		assertThat(tempFilePath, not(exists()));
	}
}
