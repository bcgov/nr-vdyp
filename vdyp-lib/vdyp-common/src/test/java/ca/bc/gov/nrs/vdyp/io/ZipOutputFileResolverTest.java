package ca.bc.gov.nrs.vdyp.io;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class ZipOutputFileResolverTest {

	@Test
	void testZipOutputFileResolver() throws IOException {

		ZipOutputFileResolver resolver = new ZipOutputFileResolver();

		MatcherAssert.assertThat(
				resolver.toPath("file").toString(),
				Matchers.is("/Users/mjunkin/source/vdyp-ssh/vdyp-lib/vdyp-common/file")
		);

		assertThrows(UnsupportedOperationException.class, () -> resolver.resolveForInput("file"));

		MatcherAssert.assertThat(
				resolver.toString("file"), Matchers.is("zip:/Users/mjunkin/source/vdyp-ssh/vdyp-lib/vdyp-common/file")
		);

		for (int i = 0; i < 5; i++) {
			OutputStream os = resolver.resolveForOutput("file" + i);
			os.write(String.format("%d", i).getBytes());
		}

		Path zipFileFromFile = Files.createTempFile(this.getClass().getName(), ".zip");
		Path zipFileFromStream = Files.createTempFile(this.getClass().getName(), "-from-stream.zip");
		try {
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
		} finally {
			Files.delete(zipFileFromFile);
			Files.delete(zipFileFromStream);
		}
	}
}
