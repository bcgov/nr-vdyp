package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.back.io.parse.BackControlParser;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.controlmap.ControlMapCache;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;
import ca.bc.gov.nrs.vdyp.fip.FipControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.ForwardControlParser;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.test.DeepFingerprint;
import ca.bc.gov.nrs.vdyp.vri.VriControlParser;

class TemplateControlMapsTest {

	/** Each application's template control file and a supplier of its control file parser */
	private static final Map<String, Supplier<BaseControlParser<?>>> APPLICATIONS = new LinkedHashMap<>();

	static {
		APPLICATIONS.put(Vdyp7Constants.FIP_START_CONTROL_FILE_NAME, FipControlParser::new);
		APPLICATIONS.put(Vdyp7Constants.VRI_START_CONTROL_FILE_NAME, VriControlParser::new);
		APPLICATIONS.put(Vdyp7Constants.FORWARD_CONTROL_FILE_NAME, ForwardControlParser::new);
		APPLICATIONS.put(Vdyp7Constants.BACK_CONTROL_FILE_NAME, BackControlParser::new);
	}

	@BeforeEach
	@AfterEach
	void clearCache() {
		ControlMapCache.getInstance().clear();
	}

	private static Map<String, Object> shared(String templateControlFile) throws Exception {
		return ControlMapCache.getInstance().getShared(
				TemplateControlMaps.source(templateControlFile), APPLICATIONS.get(templateControlFile).get()
		);
	}

	@Test
	void testEachTemplateControlFileBuildsASharedMap() throws Exception {
		for (var templateControlFile : APPLICATIONS.keySet()) {
			var map = shared(templateControlFile);
			assertThat(templateControlFile, map.get(ControlKey.BEC_DEF.name()), instanceOf(BecLookup.class));
		}
	}

	/**
	 * Builds the shared maps, runs FIP and VRI projections forward and back through the real applications, and checks
	 * that the runs used the cached maps and left every object in them unchanged.
	 */
	@Test
	void testProjectionsDoNotModifySharedMaps() throws Exception {
		Map<String, Map<String, Object>> before = new LinkedHashMap<>();
		Map<String, String> fingerprints = new LinkedHashMap<>();
		for (var templateControlFile : APPLICATIONS.keySet()) {
			var map = shared(templateControlFile);
			before.put(templateControlFile, map);
			fingerprints.put(templateControlFile, DeepFingerprint.of(map));
		}

		runProjection(
				"VRI", "test-data/vdyp7-comparison/projection_spec_vol_errorPoly.csv",
				"test-data/vdyp7-comparison/projection_spec_vol_errorLayer.csv"
		);
		runProjection(
				"FIP", "test-data/hcsv/single-layer-multiple-sp0s-fip/VDYP7_INPUT_POLY_FIP.csv",
				"test-data/hcsv/single-layer-multiple-sp0s-fip/VDYP7_INPUT_LAYER_FIP.csv"
		);

		for (var templateControlFile : APPLICATIONS.keySet()) {
			var map = shared(templateControlFile);
			assertThat(templateControlFile, map, sameInstance(before.get(templateControlFile)));
			assertThat(templateControlFile, DeepFingerprint.of(map), equalTo(fingerprints.get(templateControlFile)));
		}
	}

	private void runProjection(String id, String polygonResource, String layerResource) throws Exception {
		// Starting at age 0 projects back from the measurement year as well as forward
		Parameters params = new Parameters().ageStart(0).ageEnd(250).ageIncrement(10)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.FORWARD_GROW_ENABLED)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.BACK_GROW_ENABLED);

		var loader = getClass().getClassLoader();
		try (
				var polygons = loader.getResourceAsStream(polygonResource);
				var layers = loader.getResourceAsStream(layerResource);
				var runner = new ProjectionRunner(ProjectionRequestKind.HCSV, id, params, false)
		) {
			runner.run(
					Map.of(
							ParameterNames.HCSV_POLYGON_INPUT_DATA, polygons, ParameterNames.HCSV_LAYERS_INPUT_DATA,
							layers
					)
			);
		}
	}
}
