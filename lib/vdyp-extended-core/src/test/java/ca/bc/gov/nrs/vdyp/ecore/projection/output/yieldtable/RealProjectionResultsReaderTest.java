package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class RealProjectionResultsReaderTest {

	private static final Path relativeResourcePath = Path
			.of(FileHelper.TEST_DATA_FILES, FileHelper.YIELD_TABLE_TEST_DATA, "1");

	@Test
	void testReadFailsWhenOutputFilesAreMissing() {

		var controlMap = new HashMap<String, Object>();
		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name(), "does-not-exist.dat");
		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES.name(), "does-not-exist.dat");
		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), "does-not-exist.dat");

		var polygon = new Polygon.Builder().featureId(13919428).mapSheet("093C090").polygonNumber(94833422L).build();

		var unit = new RealProjectionResultsReader(controlMap);

		var ex = assertThrows(YieldTableGenerationException.class, () -> unit.read(polygon));
		assertTrue(ex.getMessage().startsWith("Polygon 13919428"));
	}

	@Test
	void testReadFailsWhenExpectedPolygonDoesNotMatchOutput() {

		var testHelper = new TestHelper();

		var controlMap = new HashMap<String, Object>();
		controlMap.put(
				ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name(),
				testHelper.getResourceFile(relativeResourcePath, "vp_grow.dat").toString()
		);
		controlMap.put(
				ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES.name(),
				testHelper.getResourceFile(relativeResourcePath, "vs_grow.dat").toString()
		);
		controlMap.put(
				ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(),
				testHelper.getResourceFile(relativeResourcePath, "vu_grow.dat").toString()
		);
		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);

		var polygon = new Polygon.Builder().featureId(13919428).mapSheet("999999").polygonNumber(1L).build();

		var unit = new RealProjectionResultsReader(controlMap);

		var ex = assertThrows(YieldTableGenerationException.class, () -> unit.read(polygon));
		assertTrue(ex.getMessage().startsWith("Polygon 13919428"));
	}
}
