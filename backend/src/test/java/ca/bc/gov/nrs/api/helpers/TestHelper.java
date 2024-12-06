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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;

import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
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

	public Parameters addSelectedOptions(Parameters params, Parameters.ExecutionOption... executionOptions) {

		params.setSelectedExecutionOptions(List.of(executionOptions));

		return params;
	}

	public static void verifyMessageSetIs(List<ValidationMessage> validationMessages, ValidationMessageKind... kinds) {
		Set<ValidationMessageKind> expectedKinds = Set.of(kinds);
		Set<ValidationMessageKind> presentKinds = new HashSet<>();
	
		for (var message : validationMessages) {
			presentKinds.add(message.getKind());
		}
	
		Assert.assertEquals(expectedKinds, presentKinds);
	}

	public static Parameters buildValidParametersObject() {
		return new Parameters().ageEnd(400).ageStart(1);
	}
}
