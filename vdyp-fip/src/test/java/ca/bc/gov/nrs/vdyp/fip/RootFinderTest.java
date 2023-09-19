package ca.bc.gov.nrs.vdyp.fip;

import static ca.bc.gov.nrs.vdyp.fip.FipStart.UTIL_ALL;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.array;
import static org.hamcrest.Matchers.contains;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;

import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.MultivariateDifferentiableVectorFunction;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.util.FastMath;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.fip.test.FipTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.model.Layer;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;

public class RootFinderTest {
	static public class ForwardDifferenceApproximateJacobianFunction implements MultivariateDifferentiableVectorFunction {
		private final MultivariateVectorFunction delegate;
		private final double epsilon;

		public ForwardDifferenceApproximateJacobianFunction(MultivariateVectorFunction delegate, double epsilon) {
			this.delegate = delegate;
			this.epsilon = epsilon;
		}

		public DerivativeStructure[] value(DerivativeStructure[] point) throws MathIllegalArgumentException {
			return null;
		}

		@Override
		public double[] value(double[] point) throws IllegalArgumentException {
			return delegate.value(point);
		}
	}

	@Test
	public void testRootFunction() {
		var control = FipTestUtils.loadControlMap();
		var app = new FipStart();
		app.setControlMap(control);

		var diameterBase = new double[] { 31.7022133, 26.4500256, 33.9676628, 21.4272919, 34.4568748 };
		var goal = new double[] { 1d, 7d, 74d, 9d, 30.2601795d };
		var x = new double[] { 1d, 7d, 74d, 9d, 0d };

		final var layer = new VdypLayer("Test", Layer.PRIMARY);
		final var specMap = new LinkedHashMap<String, VdypSpecies>();

		var spec3 = new VdypSpecies("Test", Layer.PRIMARY, GenusDefinitionParser.getSpeciesByIndex(3, control).getAlias());
		var spec4 = new VdypSpecies("Test", Layer.PRIMARY, GenusDefinitionParser.getSpeciesByIndex(4, control).getAlias());
		var spec5 = new VdypSpecies("Test", Layer.PRIMARY, GenusDefinitionParser.getSpeciesByIndex(5, control).getAlias());
		var spec8 = new VdypSpecies("Test", Layer.PRIMARY, GenusDefinitionParser.getSpeciesByIndex(8, control).getAlias());
		var spec15 = new VdypSpecies(
				"Test", Layer.PRIMARY, GenusDefinitionParser.getSpeciesByIndex(15, control).getAlias()
		);

		spec3.getLoreyHeightByUtilization().setCoe(UTIL_ALL, 38.7456512f);
		spec4.getLoreyHeightByUtilization().setCoe(UTIL_ALL, 22.8001652f);
		spec5.getLoreyHeightByUtilization().setCoe(UTIL_ALL, 33.6889763f);
		spec8.getLoreyHeightByUtilization().setCoe(UTIL_ALL, 24.3451157f);
		spec15.getLoreyHeightByUtilization().setCoe(UTIL_ALL, 34.6888771f);

		layer.getBaseAreaByUtilization().setCoe(UTIL_ALL, 44.6249847f);
		layer.getTreesPerHectareByUtilization().setCoe(UTIL_ALL, 620.504883f);
		layer.getQuadraticMeanDiameterByUtilization().setCoe(UTIL_ALL, 30.2601795f);
		
		spec3.setVolumeGroup(12);
		spec4.setVolumeGroup(20);
		spec5.setVolumeGroup(25);
		spec8.setVolumeGroup(37);
		spec15.setVolumeGroup(66);
		
		specMap.put(spec3.getGenus(), spec3);
		specMap.put(spec4.getGenus(), spec4);
		specMap.put(spec5.getGenus(), spec5);
		specMap.put(spec8.getGenus(), spec8);
		specMap.put(spec15.getGenus(), spec15);

		layer.setSpecies(specMap);

		MultivariateVectorFunction func = (point) -> app.rootFinderFunction(point, layer, diameterBase, goal);

		double[] y = func.value(x);
		assertThat(
				Arrays.stream(y).mapToObj(d->d).toList(),
				contains(
						closeTo(8.190178e-2), closeTo(-2.869991e0), closeTo(5.996042e0), closeTo(-2.689271e0), closeTo(1.002164e0)
				)
		);

	}

	Matcher<Double> closeTo(double v) {
		double eps = FastMath.abs(v * 0.00001);
		return Matchers.closeTo(v, eps);
	}
}
