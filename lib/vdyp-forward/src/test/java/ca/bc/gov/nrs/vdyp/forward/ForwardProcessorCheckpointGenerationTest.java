package ca.bc.gov.nrs.vdyp.forward;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.Pass;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.model.VdypPolygonParser;
import ca.bc.gov.nrs.vdyp.io.parse.model.VdypSpeciesParser;
import ca.bc.gov.nrs.vdyp.io.parse.model.VdypUtilizationParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;
import ca.bc.gov.nrs.vdyp.test.ProcessingTestUtils;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ForwardProcessorCheckpointGenerationTest {

	@TempDir(cleanup = CleanupMode.ON_SUCCESS)
	protected static Path vdyp8OutputPath;

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ForwardProcessorCheckpointGenerationTest.class);

	private static EnumSet<Pass> vdypPassSet = EnumSet
			.of(Pass.PASS_1, Pass.PASS_2, Pass.PASS_3, Pass.PASS_4, Pass.PASS_5);

	@Test
	void test() throws IOException, ResourceParseException, ProcessingException, ValueParseException {

		ForwardProcessor fp = new ForwardProcessor();

		var vdyp8OutputResolver = new FileSystemFileResolver(vdyp8OutputPath);

		ProcessingTestUtils.runForwardProcessor(fp, vdyp8OutputResolver, "VDYP-Checkpoint.CTR", vdypPassSet);

		// Verify that polygons are output 14 times for each year of growth.

		Map<String, Object> controlMap = new HashMap<>();
		var polygonParser = new VdypPolygonParser();
		controlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_POLY.name(),
				polygonParser.map("vp_grow2.dat", vdyp8OutputResolver, controlMap)
		);
		var speciesParser = new VdypSpeciesParser();
		controlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(),
				speciesParser.map("vs_grow2.dat", vdyp8OutputResolver, controlMap)
		);
		var utilizationParser = new VdypUtilizationParser();
		controlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(),
				utilizationParser.map("vu_grow2.dat", vdyp8OutputResolver, controlMap)
		);
		var becDefinitionParser = new BecDefinitionParser();
		controlMap.put(
				ControlKey.BEC_DEF.name(),
				becDefinitionParser.parse(TestUtils.class, "coe/Becdef.dat", Collections.emptyMap())
		);
		var genusDefinitionParser = new GenusDefinitionParser();
		controlMap.put(
				ControlKey.SP0_DEF.name(),
				genusDefinitionParser.parse(TestUtils.class, "coe/SP0DEF_v0.dat", Collections.emptyMap())
		);
		controlMap.put(ControlKey.VTROL.name(), new ProcessingControlVariables(new Integer[] { -1, 1, 2, 2, 1, 1, 1 }));

		try (var reader = new ForwardDataStreamReader(controlMap);) {
			Optional<VdypPolygon> polygon = reader.readNextPolygon();
			var polygonName = polygon.orElseThrow().getPolygonIdentifier().getBase();

			int count = 0;
			var nextPolygonIdentifier = reader.readNextPolygon().get().getPolygonIdentifier();
			var year = nextPolygonIdentifier.getYear();
			while (nextPolygonIdentifier.getYear() == year && nextPolygonIdentifier.getBase().equals(polygonName)) {
				count += 1;
				nextPolygonIdentifier = reader.readNextPolygon().get().getPolygonIdentifier();
			}

			assertEquals(count, 14, "polygon count (same polygon, same year");
		}
	}
}
