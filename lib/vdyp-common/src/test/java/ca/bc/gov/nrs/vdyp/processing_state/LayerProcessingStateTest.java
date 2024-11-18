package ca.bc.gov.nrs.vdyp.processing_state;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.compatibilityVariable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypCompatibilityVariables;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class LayerProcessingStateTest {

	static class TestLayerProcessingState extends LayerProcessingState<ResolvedControlMap, TestLayerProcessingState> {

		protected TestLayerProcessingState(
				ProcessingState<ResolvedControlMap, TestLayerProcessingState> ps, VdypPolygon polygon,
				LayerType subjectLayerType
		) throws ProcessingException {
			super(ps, polygon, subjectLayerType);
		}

		@Override
		protected Predicate<VdypSpecies> getBankFilter() {
			return spec -> true;
		}

		@Override
		protected VdypLayer updateLayerFromBank() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	@Nested
	class Constructor {

		@Test
		void testConstructNoSpecies() throws ProcessingException {
			var em = EasyMock.createStrictControl();
			ProcessingState<ResolvedControlMap, TestLayerProcessingState> parent = em
					.createMock("parent", ProcessingState.class);
			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);
				pb.biogeoclimaticZone(TestUtils.mockBec());
				pb.forestInventoryZone("A");
				pb.percentAvailable(90f);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

				});
			});

			em.replay();

			var unit = new TestLayerProcessingState(parent, polygon, LayerType.PRIMARY);

			assertThat(unit, hasProperty("NSpecies", is(0)));

			em.verify();

		}

		@Test
		void testConstructOneSpecies() throws ProcessingException {
			var em = EasyMock.createStrictControl();
			ProcessingState<ResolvedControlMap, TestLayerProcessingState> parent = em
					.createMock("parent", ProcessingState.class);
			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);
				pb.biogeoclimaticZone(TestUtils.mockBec());
				pb.forestInventoryZone("A");
				pb.percentAvailable(90f);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("A");
						sb.genus(1);
					});
				});
			});

			em.replay();

			var unit = new TestLayerProcessingState(parent, polygon, LayerType.PRIMARY);

			assertThat(unit, hasProperty("NSpecies", is(1)));

			em.verify();

		}
	}

	@Nested
	class SetCompatibilityVariables {
		IMocksControl em;
		LayerProcessingState<?, ?> unit;

		MatrixMap3<UtilizationClass, UtilizationClassVariable, LayerType, Float>[] cvVolume;
		MatrixMap2<UtilizationClass, LayerType, Float>[] cvBa;
		MatrixMap2<UtilizationClass, LayerType, Float>[] cvDq;
		Map<UtilizationClassVariable, Float>[] cvSm;

		@BeforeEach
		@SuppressWarnings("unchecked")
		void setup() throws ProcessingException {
			em = EasyMock.createStrictControl();
			ProcessingState<ResolvedControlMap, TestLayerProcessingState> parent = em
					.createMock("parent", ProcessingState.class);
			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);
				pb.biogeoclimaticZone(TestUtils.mockBec());
				pb.forestInventoryZone("A");
				pb.percentAvailable(90f);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("A");
						sb.genus(1);
					});
				});
			});

			em.replay();

			unit = new TestLayerProcessingState(parent, polygon, LayerType.PRIMARY);

			cvVolume = new MatrixMap3[] { null,
					new MatrixMap3Impl<UtilizationClass, UtilizationClassVariable, LayerType, Float>(
							List.of(UtilizationClass.values()), VdypCompatibilityVariables.VOLUME_UTILIZATION_VARIABLES,
							List.of(LayerType.values()),
							(uc, vv, lt) -> 11f + vv.ordinal() * 2f + uc.ordinal() * 3f + lt.ordinal() * 5f
					) };

			cvBa = new MatrixMap2[] { null,
					new MatrixMap2Impl<UtilizationClass, LayerType, Float>(
							List.of(UtilizationClass.values()), List.of(LayerType.values()),
							(uc, lt) -> 13f + uc.ordinal() * 3f + lt.ordinal() * 5f
					) };

			cvDq = new MatrixMap2[] { null,
					new MatrixMap2Impl<UtilizationClass, LayerType, Float>(
							List.of(UtilizationClass.values()), List.of(LayerType.values()),
							(uc, lt) -> 17f + uc.ordinal() * 3f + lt.ordinal() * 5f
					) };
			cvSm = new EnumMap[] { null, new EnumMap<UtilizationClassVariable, Float>(UtilizationClassVariable.class) };

			for (var uc : UtilizationClassVariable.values()) {
				cvSm[1].put(uc, uc.ordinal() * 7f);
			}

		}

		@AfterEach
		void cleanup() {
			em.verify();
		}

		@Test
		void testFailBeforeSet() throws ProcessingException {
			assertThrows(
					IllegalStateException.class,
					() -> unit.getCVVolume(
							1, UtilizationClass.ALL, UtilizationClassVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY
					), "getCVVolume"
			);
			assertThrows(
					IllegalStateException.class, () -> unit.getCVBasalArea(1, UtilizationClass.ALL, LayerType.PRIMARY),
					"getCVBasalArea"
			);
			assertThrows(
					IllegalStateException.class,
					() -> unit.getCVQuadraticMeanDiameter(1, UtilizationClass.ALL, LayerType.PRIMARY),
					"getCVQuadraticMeanDiameter"
			);
			assertThrows(
					IllegalStateException.class, () -> unit.getCVSmall(1, UtilizationClassVariable.BASAL_AREA),
					"getCVSmall"
			);
		}

		@Test
		void testSucceedAfterSet() throws ProcessingException {
			unit.setCompatibilityVariableDetails(cvVolume, cvBa, cvDq, cvSm);
			assertThat(
					unit,
					testCV(
							"getCVVolume", 1, UtilizationClass.ALL, UtilizationClassVariable.CLOSE_UTIL_VOL,
							LayerType.PRIMARY,
							is(
									11f + UtilizationClassVariable.CLOSE_UTIL_VOL.ordinal() * 2f
											+ UtilizationClass.ALL.ordinal() * 3f + LayerType.PRIMARY.ordinal() * 5f
							)
					)
			);
			assertThat(
					unit,
					testCV(
							"getCVBasalArea", 1, UtilizationClass.ALL, LayerType.PRIMARY,
							is(13f + UtilizationClass.ALL.ordinal() * 3f + LayerType.PRIMARY.ordinal() * 5f)
					)
			);
			assertThat(
					unit,
					testCV(
							"getCVQuadraticMeanDiameter", 1, UtilizationClass.U125TO175, LayerType.PRIMARY,
							is(17f + UtilizationClass.U125TO175.ordinal() * 3f + LayerType.PRIMARY.ordinal() * 5f)
					)
			);
			assertThat(
					unit,
					testCV(
							"getCVSmall", 1, UtilizationClassVariable.QUAD_MEAN_DIAMETER,
							is(UtilizationClassVariable.QUAD_MEAN_DIAMETER.ordinal() * 7f)
					)
			);
		}

		@Test
		void testFailDoubleSet() throws ProcessingException {
			unit.setCompatibilityVariableDetails(cvVolume, cvBa, cvDq, cvSm);
			assertThrows(
					IllegalStateException.class, () -> unit.setCompatibilityVariableDetails(cvVolume, cvBa, cvDq, cvSm)
			);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Matcher<LayerProcessingState<?, ?>>
				testCV(String name, Object p1, Object p2, Object p3, Object p4, Matcher<Float> match) {
			return (Matcher) compatibilityVariable(name, match, LayerProcessingState.class, p1, p2, p3, p4);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Matcher<LayerProcessingState<?, ?>> testCV(String name, Object p1, Object p2, Object p3, Matcher<Float> match) {
			return (Matcher) compatibilityVariable(name, match, LayerProcessingState.class, p1, p2, p3);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Matcher<LayerProcessingState<?, ?>> testCV(String name, Object p1, Object p2, Matcher<Float> match) {
			return (Matcher) compatibilityVariable(name, match, LayerProcessingState.class, p1, p2);
		}

	}
}
