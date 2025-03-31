package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.scenarios;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;

public class Scenario {
	protected static final String CSV_OUTPUT_FILE_NAME = "Output_YldTbl.csv";
	protected static final String TXT_OUTPUT_FILE_NAME = "Output_YldTbl.txt";

	protected final TestHelper testHelper;
	protected final String scenario;
	protected final URI rootResourceFilePathUri;

	protected Scenario(String scenario) {
		this.testHelper = new TestHelper();
		this.scenario = scenario;

		try {
			this.rootResourceFilePathUri = this.getClass().getClassLoader().getResource("scenarios/scenarios.txt")
					.toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeEach
	protected void setup() {
	}

	protected InputStream record(InputStream is, String outputFileName) throws IOException {

		var baos = new ByteArrayOutputStream();

		is.transferTo(baos);

		var outputFilePath = getScenarioOutputFileAbsolutePath(outputFileName, "actual");

		Files.deleteIfExists(outputFilePath);
		var outputFile = Files.createFile(outputFilePath).toFile();

		try (
				var bais = new ByteArrayInputStream(baos.toByteArray());
				var outputFileStream = new FileOutputStream(outputFile)
		) {
			IOUtils.copy(bais, outputFileStream);
		}

		return new ByteArrayInputStream(baos.toByteArray());
	}

	protected Path getResourceFolderPath() {

		return Path.of("scenarios", scenario);
	}

	protected Path getScenarioAbsolutePath() {

		Path paramsFilePath = Path.of("/scenarios", scenario, "parms.txt");
		var absoluteParmsFileUrl = this.getClass().getResource(paramsFilePath.toString());

		try {
			return Path.of(absoluteParmsFileUrl.toURI()).getParent();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	protected Path getScenarioOutputFileAbsolutePath(String outputFileName, String... trailingComponents) {

		try {
			var absoluteScenarioFolderPath = getScenarioAbsolutePath();
			var absoluteOutputFileFolderPath = Path.of(absoluteScenarioFolderPath.toString(), trailingComponents);

			// Create any directories that don't yet exist.
			Files.createDirectories(absoluteOutputFileFolderPath);

			return Path.of(absoluteOutputFileFolderPath.toString(), outputFileName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void recordScenarioParameters(Parameters parameters) {

		Path parametersFilePath = getScenarioOutputFileAbsolutePath("parameters.json");

		try {
			// Included to generate JSON text of parameters as needed
			ObjectMapper mapper = new ObjectMapper();
			String serializedParametersText = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters);

			Files.write(parametersFilePath, serializedParametersText.getBytes());

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
