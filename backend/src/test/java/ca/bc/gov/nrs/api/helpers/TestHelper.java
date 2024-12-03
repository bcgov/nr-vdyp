package ca.bc.gov.nrs.api.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestHelper {

	public static final String ROOT_PATH = "/api/v8";

	public Path getResourceFile(Path testResourceFolderPath, String fileName) throws IOException {

		String resourceFilePath = Path.of(testResourceFolderPath.toString(), fileName).toString();

		URL testFileURL = this.getClass().getClassLoader().getResource(resourceFilePath);
		try {
			File resourceFile = new File(testFileURL.toURI());
			return Path.of(resourceFile.getAbsolutePath());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(MessageFormat.format("Unable to find test resource {0}", resourceFilePath));
		}
	}

	public byte[] readZipEntry(ZipInputStream zipInputStream, ZipEntry zipEntry) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int bytesRead;
		while ( (bytesRead = zipInputStream.read(buffer, 0, 1024)) != -1) {
			baos.write(buffer, 0, bytesRead);
		}

		return baos.toByteArray();
	}

	public InputStream buildTestFile() throws IOException {
		return new ByteArrayInputStream("Test data".getBytes());
	}

	public Parameters
			addSelectedOptions(Parameters params, Parameters.SelectedExecutionOptionsEnum... executionOptions) {

		var options = new ArrayList<String>();
		for (var e : executionOptions) {
			options.add(e.toString());
		}
		params.setSelectedExecutionOptions(options);

		return params;
	}
}
