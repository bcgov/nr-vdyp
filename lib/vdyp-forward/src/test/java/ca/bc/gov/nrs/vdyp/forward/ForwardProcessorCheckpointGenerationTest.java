package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_1;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_2;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_3;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_4;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_5;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ForwardProcessorCheckpointGenerationTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ForwardProcessorCheckpointGenerationTest.class);

	private static Set<ForwardPass> vdypPassSet = new HashSet<>(Arrays.asList(PASS_1, PASS_2, PASS_3, PASS_4, PASS_5));

	@Test
	void test() throws IOException, ResourceParseException, ProcessingException {

		ForwardProcessor fp = new ForwardProcessor();

		FileResolver inputFileResolver = TestUtils.fileResolver(TestUtils.class);

		Path vdyp7OutputPath = Path.of(System.getenv().get("HOME"), "tmp", "vdyp-deltas", "vdyp8");
		Files.createDirectories(vdyp7OutputPath);

		var outputResolver = new FileSystemFileResolver(vdyp7OutputPath);

		fp.run(inputFileResolver, outputResolver, List.of("VDYP-Checkpoint.CTR"), vdypPassSet);

		assertTrue(true);
	}

	// @Test
	void testForSpecificPolygon() throws IOException, ResourceParseException, ProcessingException {

		ForwardProcessor fp = new ForwardProcessor();

		FileResolver inputFileResolver = TestUtils.fileResolver(TestUtils.class);

		Path vdyp7OutputPath = Path.of(System.getenv().get("HOME"), "tmp", "vdyp-deltas", "vdyp8");
		Files.createDirectories(vdyp7OutputPath);

		var outputResolver = new FileSystemFileResolver(vdyp7OutputPath);

		fp.run(inputFileResolver, outputResolver, List.of("VDYP-Checkpoint.CTR"), vdypPassSet,
				p -> p.getPolygonIdentifier().getBase().indexOf("01002 S000004 00") >= 0);

		assertTrue(true);
	}
}
