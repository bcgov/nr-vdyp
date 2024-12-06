package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.input;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.CsvToBeanBuilder;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonRecordBean;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class HscvPolygonReaderTest {

	private static Logger logger = LoggerFactory.getLogger(HscvPolygonReaderTest.class);

	private final TestHelper testHelper;

	@Inject
	HscvPolygonReaderTest(TestHelper testHelper) {
		this.testHelper = testHelper;
	}

	@Test
	void testHscvPolygonReader() throws IOException {

		logger.info("Starting testHscvPolygonReader");

		Path resourceFolderPath = Path.of("VDYP7Console-sample-files", "hcsv", "vdyp-240");

		byte[] csvBytes = Files.readAllBytes(testHelper.getResourceFile(resourceFolderPath, "VDYP7_INPUT_POLY.csv"));
		
		var polygonCsvStream = new CsvToBeanBuilder<HcsvPolygonRecordBean>(
				new BufferedReader(
						new InputStreamReader(
								new ByteArrayInputStream(
										csvBytes))))
				.withSeparator(',')
                .withType(HcsvPolygonRecordBean.class)
                .build();
		
		for (var polygon: polygonCsvStream) {
			logger.info("Read polygon {}", polygon.getPolygonNumber());
		}
	}
}
