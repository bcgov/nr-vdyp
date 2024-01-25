package ca.bc.gov.nrs.vdyp.forward;

class RootFinderTest {
	private final double epsilon = 0.00001;

//	@Test
//	void testRootFunction() {
//		var control = VDYPForwardTestUtils.loadControlMap();
//		var app = new VDYPForwardStart();
//		app.setControlMap(control);
//
//		var diameterBase = new double[] { 31.7022133, 26.4500256, 33.9676628, 21.4272919, 34.4568748 };
//		var x = new double[] { 1d, 7d, 74d, 9d, 0d };
//
//		var layer = mockLayer1(control);
//
//		MultivariateVectorFunction func = (point) -> app.rootFinderFunction(point, layer, diameterBase);
//
//		double[] y = func.value(x);
//		assertThat(
//				Arrays.stream(y).mapToObj(d -> d).toList(),
//				contains(
//						closeTo(1 + 8.190178e-2), closeTo(7 - 2.869991e0), closeTo(74 + 5.996042e0),
//						closeTo(9 - 2.689271e0), closeTo(30.2601795 + 1.002164e0)
//				)
//		);
//
//	}
//
//	@Test
//	void testRootFunctionJacobian() {
//		var control = VDYPForwardTestUtils.loadControlMap();
//		var app = new VDYPForwardStart();
//		app.setControlMap(control);
//
//		var diameterBase = new double[] { 31.7022133, 26.4500256, 33.9676628, 21.4272919, 34.4568748 };
//		var x = new double[] { 1d, 7d, 74d, 9d, 0d };
//
//		var layer = mockLayer1(control);
//
//		MultivariateVectorFunction func = (point) -> app.rootFinderFunction(point, layer, diameterBase);
//
//		MultivariateMatrixFunction jacFunc = (point) -> app.estimateJacobian(point, func);
//
//		RealMatrix jacobian = new Array2DRowRealMatrix(jacFunc.value(x));
//
////		0x004F1B78   1.080512e0      -5.529128e-3     -1.078180e-1     -8.984832e-3     -2.384436e-2
////		0x004F1B8C   3.801275e-3      6.043534e-1      2.809981e-1      2.211651e-2     -8.970021e-2
////		0x004F1BA0  -1.501072e-3     -5.725262e-3      9.703245e-1     -8.760679e-3     -3.791935e-3
////		0x004F1BB4   2.603777e-3      9.983147e-3      1.924444e-1      7.162524e-1     -2.040018e-1
////		0x004F1BC8   1.723533e-2     -1.313168e-2     -5.943812e-2      4.215959e-2      1.170447e0
//
//		assertThat(
//				jacobian, matrixCloseTo(
//						new double[][] { //
//								{ 1.080512e0, 3.801275e-3, -1.501072e-3, 2.603777e-3, 1.723533e-2 }, //
//								{ -5.529128e-3, 6.043534e-1, -5.725262e-3, 9.983147e-3, -1.313168e-2 }, //
//								{ -1.078180e-1, 2.809981e-1, 9.703245e-1, 1.924444e-1, -5.943812e-2 }, //
//								{ -8.984832e-3, 2.211651e-2, -8.760679e-3, 7.162524e-1, 4.215959e-2 }, //
//								{ -2.384436e-2, -8.970021e-2, -3.791935e-3, -2.040018e-1, 1.170447e0 } //
//						}, 0.005
//				)
//		);
//
//	}
//
//	@Test
//	void testRootFunctionSolve() {
//		var control = VDYPForwardTestUtils.loadControlMap();
//		var app = new VDYPForwardStart();
//		app.setControlMap(control);
//
//		var diameterBase = new double[] { 31.7022133, 26.4500256, 33.9676628, 21.4272919, 34.4568748 };
//		var goal = new double[] { 1d, 7d, 74d, 9d, 30.2601795d };
//		var x = new double[] { 1d, 7d, 74d, 9d, 0d };
//
//		var layer = mockLayer1(control);
//
//		var point = app.findRoot(diameterBase, goal, x, layer, 2.0e-3f);
//
//		assertThat(
//				point,
//				vectorCloseTo(new double[] { 0.891877294, 11.4491625, 66.0574265, 12.3855982, 0.00443319743 }, 2.0E-03)
//		);
//	}
//
//	VdypLayer mockLayer1(Map<String, Object> control) {
//
//		final var layer = new VdypLayer("Test", Layer.PRIMARY);
//		final var specMap = new LinkedHashMap<String, VdypSpecies>();
//
//		var spec3 = new VdypSpecies(
//				"Test", Layer.PRIMARY, GenusDefinitionParser.getSpeciesByIndex(3, control).getAlias()
//		);
//		var spec4 = new VdypSpecies(
//				"Test", Layer.PRIMARY, GenusDefinitionParser.getSpeciesByIndex(4, control).getAlias()
//		);
//		var spec5 = new VdypSpecies(
//				"Test", Layer.PRIMARY, GenusDefinitionParser.getSpeciesByIndex(5, control).getAlias()
//		);
//		var spec8 = new VdypSpecies(
//				"Test", Layer.PRIMARY, GenusDefinitionParser.getSpeciesByIndex(8, control).getAlias()
//		);
//		var spec15 = new VdypSpecies(
//				"Test", Layer.PRIMARY, GenusDefinitionParser.getSpeciesByIndex(15, control).getAlias()
//		);
//
//		spec3.getLoreyHeightByUtilization().setCoe(UTIL_ALL, 38.7456512f);
//		spec4.getLoreyHeightByUtilization().setCoe(UTIL_ALL, 22.8001652f);
//		spec5.getLoreyHeightByUtilization().setCoe(UTIL_ALL, 33.6889763f);
//		spec8.getLoreyHeightByUtilization().setCoe(UTIL_ALL, 24.3451157f);
//		spec15.getLoreyHeightByUtilization().setCoe(UTIL_ALL, 34.6888771f);
//
//		layer.getBaseAreaByUtilization().setCoe(UTIL_ALL, 44.6249847f);
//		layer.getTreesPerHectareByUtilization().setCoe(UTIL_ALL, 620.504883f);
//		layer.getQuadraticMeanDiameterByUtilization().setCoe(UTIL_ALL, 30.2601795f);
//
//		spec3.setVolumeGroup(12);
//		spec4.setVolumeGroup(20);
//		spec5.setVolumeGroup(25);
//		spec8.setVolumeGroup(37);
//		spec15.setVolumeGroup(66);
//
//		specMap.put(spec3.getGenus(), spec3);
//		specMap.put(spec4.getGenus(), spec4);
//		specMap.put(spec5.getGenus(), spec5);
//		specMap.put(spec8.getGenus(), spec8);
//		specMap.put(spec15.getGenus(), spec15);
//
//		layer.setSpecies(specMap);
//
//		return layer;
//	}
//
//	Matcher<RealMatrix> matrixCloseTo(double[][] value, double epsilon) {
//		var expected = new Array2DRowRealMatrix(value);
//		return new CustomTypeSafeMatcher<RealMatrix>("expected matrix " + expected) {
//			@Override
//			protected boolean matchesSafely(RealMatrix item) {
//				if (item.getColumnDimension() != expected.getColumnDimension()
//						|| item.getRowDimension() != expected.getRowDimension()) {
//					return false;
//				}
//				for (int j = 0; j < item.getColumnDimension(); j++) {
//					for (int i = 0; i < item.getRowDimension(); i++) {
//						final double expectedEntry = expected.getEntry(i, j);
//						final double itemEntry = item.getEntry(i, j);
//						if (FastMath.abs(expectedEntry - itemEntry) > epsilon) {
//							return false;
//						}
//					}
//				}
//				return true;
//			}
//		};
//	}
//
//	Matcher<RealVector> vectorCloseTo(double[] value, double epsilon) {
//		var expected = new ArrayRealVector(value);
//		return new CustomTypeSafeMatcher<RealVector>("expected Vector " + expected) {
//			@Override
//			protected boolean matchesSafely(RealVector item) {
//				if (item.getDimension() != expected.getDimension()) {
//					return false;
//				}
//				for (int i = 0; i < item.getDimension(); i++) {
//					final double expectedEntry = expected.getEntry(i);
//					final double itemEntry = item.getEntry(i);
//					if (FastMath.abs(expectedEntry - itemEntry) > epsilon) {
//						return false;
//					}
//				}
//				return true;
//			}
//		};
//	}
//
//	Matcher<Double> closeTo(double v) {
//		double eps = FastMath.abs(v * epsilon);
//		return Matchers.closeTo(v, eps);
//	}
}