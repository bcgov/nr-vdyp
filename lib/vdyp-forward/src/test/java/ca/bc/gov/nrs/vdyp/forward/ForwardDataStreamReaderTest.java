package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilization;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ForwardDataStreamReaderTest {

	@Nested
	class CalculateSpeciesCoverage {
		@Test
		void test100Percent() throws ProcessingException {
			var controlMap = TestUtils.loadControlMap();
			var spec = VdypSpecies.build(sb -> {
				sb.polygonIdentifier("Test", 2025);
				sb.layerType(LayerType.PRIMARY);
				sb.genus("F", controlMap);
				sb.baseArea(40f);
			});

			Map<UtilizationClass, VdypUtilization> defaultUtilization = Utils.constMap(map -> {
				map.put(
						UtilizationClass.ALL,
						new VdypUtilization(
								spec.getPolygonIdentifier(), spec.getLayerType(), 0, java.util.Optional.empty(),
								UtilizationClass.ALL, 40f, // The only thing that matters for the test is basal area
								Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN
						)
				);
			});
			ForwardDataStreamReader.calculateSpeciesCoverage(spec, defaultUtilization);

			assertThat(spec, hasProperty("percentGenus", closeTo(100f)));
			assertThat(spec, hasProperty("fractionGenus", closeTo(1f)));
		}

		@Test
		void test40Percent() throws ProcessingException {
			var controlMap = TestUtils.loadControlMap();
			var spec = VdypSpecies.build(sb -> {
				sb.polygonIdentifier("Test", 2025);
				sb.layerType(LayerType.PRIMARY);
				sb.genus("F", controlMap);
				sb.baseArea(40f);
			});

			Map<UtilizationClass, VdypUtilization> defaultUtilization = Utils.constMap(map -> {
				map.put(
						UtilizationClass.ALL,
						new VdypUtilization(
								spec.getPolygonIdentifier(), spec.getLayerType(), 0, java.util.Optional.empty(),
								UtilizationClass.ALL, 100f, // The only thing that matters for the test is basal area
								Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN
						)
				);
			});
			ForwardDataStreamReader.calculateSpeciesCoverage(spec, defaultUtilization);

			assertThat(spec, hasProperty("percentGenus", closeTo(40f)));
			assertThat(spec, hasProperty("fractionGenus", closeTo(0.4f)));
		}
	}
}
