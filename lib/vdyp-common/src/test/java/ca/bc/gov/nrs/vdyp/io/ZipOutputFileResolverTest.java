package ca.bc.gov.nrs.vdyp.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZipOutputFileResolverTest {

	@TempDir
	Path outputLocation;

	@Test
	void testZipOutputFileResolver() throws IOException {

		ZipOutputFileResolver resolver = new ZipOutputFileResolver();
		
		assertThat(resolver.toString(), equalTo("ZipOutputFileResolver"));
		
		assertThat(resolver.getOutputFileResolver(), equalTo(resolver));
		assertThrows(UnsupportedOperationException.class, () -> resolver.getInputFileResolver());

		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();

		assertThat(resolver.toPath("file"), is(Paths.get(currentPath, "file")));

		assertThrows(UnsupportedOperationException.class, () -> resolver.resolveForInput("file"));

		assertThat(resolver.toString("file"), endsWith(Paths.get(currentPath, "file").toString()));

		for (int i = 0; i < 5; i++) {
			OutputStream os = resolver.resolveForOutput("file" + i);
			os.write(String.format("%d", i).getBytes());
		}

		Path zipFileFromFile = outputLocation.resolve(this.getClass().getSimpleName() + ".zip");
		Path zipFileFromStream = outputLocation.resolve(this.getClass().getSimpleName() + "-from-stream.zip");

		resolver.generate(zipFileFromFile);

		System.out.println("Output zip file written to " + zipFileFromFile.toString());

		try (ZipFile zip = new ZipFile(zipFileFromFile.toFile())) {
			var entries = zip.entries();

			byte[] buffer = new byte[16];
			while (entries.hasMoreElements()) {
				ZipEntry e = entries.nextElement();

				InputStream is = zip.getInputStream(e);
				int nBytesRead = is.read(buffer, 0, 10);
				assertTrue(nBytesRead == 1);
				String fileNumber = e.getName().substring(e.getName().length() - 1, e.getName().length());
				assertTrue(new String(Arrays.copyOf(buffer, nBytesRead)).equals(fileNumber));
			}
		}

		InputStream zipByteStream = resolver.generateStream();
		Files.write(zipFileFromStream, zipByteStream.readAllBytes());

		try (ZipFile zip = new ZipFile(zipFileFromStream.toFile())) {
			var entries = zip.entries();

			byte[] buffer = new byte[16];
			while (entries.hasMoreElements()) {
				ZipEntry e = entries.nextElement();

				InputStream is = zip.getInputStream(e);
				int nBytesRead = is.read(buffer, 0, 10);
				assertTrue(nBytesRead == 1);
				String fileNumber = e.getName().substring(e.getName().length() - 1, e.getName().length());
				assertTrue(new String(Arrays.copyOf(buffer, nBytesRead)).equals(fileNumber));
			}
		}
	}
}
