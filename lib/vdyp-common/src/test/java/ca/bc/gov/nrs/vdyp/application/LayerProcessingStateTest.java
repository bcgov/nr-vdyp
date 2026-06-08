package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.compatibilityVariable;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.model.projection.ControlVariable;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.PrimarySpeciesDetails;
import ca.bc.gov.nrs.vdyp.processing_state.ProcessingState;
import ca.bc.gov.nrs.vdyp.processing_state.SpeciesRankingDetails;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class LayerProcessingStateTest {

	static class TestLayerProcessingState extends LayerProcessingState<TestLayerProcessingState> {

		protected TestLayerProcessingState(
				ProcessingState<TestLayerProcessingState> ps, VdypPolygon polygon, LayerType subjectLayerType
		) throws ProcessingException {
			super(ps, polygon, subjectLayerType);
		}

		@Override
		protected Predicate<VdypSpecies> getBankFilter() {
			return spec -> true;
		}

		@Override
		protected void applyCompatibilityVariables(VdypSpecies species, int i) {
			// TODO Auto-generated method stub

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
			ProcessingState<TestLayerProcessingState> parent = em.createMock("parent", ProcessingState.class);
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
			ProcessingState<TestLayerProcessingState> parent = em.createMock("parent", ProcessingState.class);
			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);
				pb.biogeoclimaticZone(TestUtils.mockBec());
				pb.forestInventoryZone("A");
				pb.percentAvailable(90f);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("A", 1);
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
		LayerProcessingState<?> unit;

		MatrixMap3<UtilizationClass, VolumeVariable, LayerType, Float>[] cvVolume;
		MatrixMap2<UtilizationClass, LayerType, Float>[] cvBa;
		MatrixMap2<UtilizationClass, LayerType, Float>[] cvDq;
		Map<UtilizationClassVariable, Float>[] cvSm;

		@BeforeEach
		@SuppressWarnings("unchecked")
		void setup() throws ProcessingException {
			var em = EasyMock.createStrictControl();
			ProcessingState<TestLayerProcessingState> parent = em.createMock("parent", ProcessingState.class);
			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);
				pb.biogeoclimaticZone(TestUtils.mockBec());
				pb.forestInventoryZone("A");
				pb.percentAvailable(90f);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("A", 1);
					});
				});
			});

			em.replay();

			unit = new TestLayerProcessingState(parent, polygon, LayerType.PRIMARY);

			cvVolume = new MatrixMap3[] { null, new MatrixMap3Impl<UtilizationClass, VolumeVariable, LayerType, Float>(
					List.of(UtilizationClass.values()), List.of(VolumeVariable.values()), List.of(LayerType.values()),
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

		@Test
		void testFailBeforeSet() throws ProcessingException {
			assertThrows(
					IllegalStateException.class,
					() -> unit.getCVVolume(1, UtilizationClass.ALL, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY),
					"getCVVolume"
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
							"getCVVolume", 1, UtilizationClass.ALL, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY,
							is(16f)
					)
			);
			assertThat(unit, testCV("getCVBasalArea", 1, UtilizationClass.ALL, LayerType.PRIMARY, is(16f)));
			assertThat(
					unit,
					testCV("getCVQuadraticMeanDiameter", 1, UtilizationClass.U125TO175, LayerType.PRIMARY, is(26f))
			);
			assertThat(unit, testCV("getCVSmall", 1, UtilizationClassVariable.QUAD_MEAN_DIAMETER, is(7f)));
		}

		@Test
		void testFailDoubleSet() throws ProcessingException {
			unit.setCompatibilityVariableDetails(cvVolume, cvBa, cvDq, cvSm);
			assertThrows(
					IllegalStateException.class, () -> unit.setCompatibilityVariableDetails(cvVolume, cvBa, cvDq, cvSm)
			);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Matcher<LayerProcessingState<?>>
				testCV(String name, Object p1, Object p2, Object p3, Object p4, Matcher<Float> match) {
			return (Matcher) compatibilityVariable(name, match, LayerProcessingState.class, p1, p2, p3, p4);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Matcher<LayerProcessingState<?>> testCV(String name, Object p1, Object p2, Object p3, Matcher<Float> match) {
			return (Matcher) compatibilityVariable(name, match, LayerProcessingState.class, p1, p2, p3);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Matcher<LayerProcessingState<?>> testCV(String name, Object p1, Object p2, Matcher<Float> match) {
			return (Matcher) compatibilityVariable(name, match, LayerProcessingState.class, p1, p2);
		}

	}

	@Nested
	class RankingDetails {
		LayerProcessingState<?> unit;
		IMocksControl em;

		@BeforeEach
		void setup() throws Exception {
			em = EasyMock.createStrictControl();
			ProcessingState<TestLayerProcessingState> parent = em.createMock("parent", ProcessingState.class);
			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);
				pb.biogeoclimaticZone(TestUtils.mockBec());
				pb.forestInventoryZone("A");
				pb.percentAvailable(90f);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("A", 1);
					});
				});
			});

			em.replay();

			unit = new TestLayerProcessingState(parent, polygon, LayerType.PRIMARY);

			assertThat(unit, hasProperty("NSpecies", is(1)));

		}

		@AfterEach
		void verify() {
			em.verify();
		}

		@Test
		void testUnset() {

			assertThat(unit, hasProperty("areRankingDetailsSet", is(false)));
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getInventoryTypeGroup()),
					hasProperty("message", containsString("inventoryTypeGroup"))
			);
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getPrimarySpeciesIndex()),
					hasProperty("message", containsString("primarySpeciesIndex"))
			);
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getPrimarySpeciesAlias()),
					hasProperty("message", containsString("primarySpeciesAlias"))
			);
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getSecondarySpeciesIndex()),
					hasProperty("message", containsString("secondarySpeciesIndex"))
			);
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getPrimarySpeciesGroupNumber()),
					hasProperty("message", containsString("primarySpeciesGroupNumber"))
			);
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getPrimarySpeciesStratumNumber()),
					hasProperty("message", containsString("primarySpeciesStratumNumber"))
			);

		}

		@Test
		void testSetNoSecondary() {
			unit.setSpeciesRankingDetails(new SpeciesRankingDetails(1, Optional.empty(), 12, 13, 14));
			assertThat(unit, hasProperty("areRankingDetailsSet", is(true)));
			assertThat(unit, hasProperty("inventoryTypeGroup", is(12)));
			assertThat(unit, hasProperty("primarySpeciesIndex", is(1)));
			assertThat(unit, hasProperty("primarySpeciesAlias", is("A")));
			assertThat(unit, hasProperty("secondarySpeciesIndex", notPresent()));
			assertThat(unit, hasProperty("primarySpeciesGroupNumber", is(13)));
			assertThat(unit, hasProperty("primarySpeciesStratumNumber", is(14)));

		}

		@Test
		void testSetWithSecondary() {
			unit.setSpeciesRankingDetails(new SpeciesRankingDetails(1, Optional.of(2), 12, 13, 14));
			assertThat(unit, hasProperty("areRankingDetailsSet", is(true)));
			assertThat(unit, hasProperty("inventoryTypeGroup", is(12)));
			assertThat(unit, hasProperty("primarySpeciesIndex", is(1)));
			assertThat(unit, hasProperty("primarySpeciesAlias", is("A")));
			assertThat(unit, hasProperty("secondarySpeciesIndex", present(is(2))));
			assertThat(unit, hasProperty("primarySpeciesGroupNumber", is(13)));
			assertThat(unit, hasProperty("primarySpeciesStratumNumber", is(14)));

		}

		@Test
		void testSetTwice() {
			unit.setSpeciesRankingDetails(new SpeciesRankingDetails(1, Optional.empty(), 12, 13, 14));
			assertThat(
					assertThrows(
							IllegalStateException.class,
							() -> unit.setSpeciesRankingDetails(
									new SpeciesRankingDetails(1, Optional.empty(), 12, 13, 14)
							)
					),
					hasProperty("message", equalTo(LayerProcessingState.SPECIES_RANKING_DETAILS_CAN_BE_SET_ONCE_ONLY))
			);
		}

	}

	@Nested
	class PrimaryDetails {
		LayerProcessingState<?> unit;
		IMocksControl em;
		ProcessingControlVariables controlVariables;

		@BeforeEach
		void setup() throws Exception {
			controlVariables = new ProcessingControlVariables(new Integer[] {});

			em = EasyMock.createStrictControl();
			ProcessingState<TestLayerProcessingState> parent = em.createMock("parent", ProcessingState.class);
			ProcessingResolvedControlMap controlMap = em.createMock("controlMap", ProcessingResolvedControlMap.class);

			var polygon = VdypPolygon.build(pb -> {
				pb.polygonIdentifier("Test", 2024);
				pb.biogeoclimaticZone(TestUtils.mockBec());
				pb.forestInventoryZone("A");
				pb.percentAvailable(90f);

				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);

					lb.addSpecies(sb -> {
						sb.genus("A", 1);
					});
				});
			});

			EasyMock.expect(parent.getControlMap()).andStubReturn(controlMap);
			EasyMock.expect(controlMap.getControlVariables()).andStubReturn(controlVariables);

			em.replay();

			unit = new TestLayerProcessingState(parent, polygon, LayerType.PRIMARY);

			assertThat(unit, hasProperty("NSpecies", is(1)));

		}

		@AfterEach
		void verify() {
			em.verify();
		}

		@Test
		void testUnset() {
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getPrimarySpeciesDominantHeight()),
					hasProperty("message", containsString("primarySpeciesDominantHeight"))
			);
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getPrimarySpeciesSiteIndex()),
					hasProperty("message", containsString("primarySpeciesSiteIndex"))
			);
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getPrimarySpeciesTotalAge()),
					hasProperty("message", containsString("primarySpeciesTotalAge"))
			);
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getPrimarySpeciesAgeAtBreastHeight()),
					hasProperty("message", containsString("primarySpeciesAgeAtBreastHeight"))
			);
			assertThat(
					assertThrows(IllegalStateException.class, () -> unit.getPrimarySpeciesAgeToBreastHeight()),
					hasProperty("message", containsString("primarySpeciesAgeToBreastHeight"))
			);
		}

		@Test
		void testSetWithBankEmpty() {

			unit.setPrimarySpeciesDetails(new PrimarySpeciesDetails(11f, 12f, 13f, 14f, 15f));

			// Properties should change

			assertThat(unit, hasProperty("primarySpeciesDominantHeight", is(11f)));
			assertThat(unit, hasProperty("primarySpeciesSiteIndex", is(12f)));
			assertThat(unit, hasProperty("primarySpeciesTotalAge", is(13f)));
			assertThat(unit, hasProperty("primarySpeciesAgeAtBreastHeight", is(14f)));
			assertThat(unit, hasProperty("primarySpeciesAgeToBreastHeight", is(15f)));

			var bank = unit.getBank();

			// So should bank as it was empty.

			assertThat("bank.dominantHeights", bank.dominantHeights[0], is(11f));
			assertThat("bank.siteIndices", bank.siteIndices[0], is(12f));
			assertThat("bank.ageTotals", bank.ageTotals[0], is(13f));
			assertThat("bank.yearsAtBreastHeight", bank.yearsAtBreastHeight[0], is(14f));
			assertThat("bank.yearsToBreastHeight", bank.yearsToBreastHeight[0], is(15f));
		}

		@Test
		void testSetWithBankSet() {
			var bank = unit.getBank();
			bank.dominantHeights[0] = 21f;
			bank.siteIndices[0] = 22f;
			bank.ageTotals[0] = 23f;
			bank.yearsAtBreastHeight[0] = 24f;
			bank.yearsToBreastHeight[0] = 25f;

			unit.setPrimarySpeciesDetails(new PrimarySpeciesDetails(11f, 12f, 13f, 14f, 15f));

			// Properties should change

			assertThat(unit, hasProperty("primarySpeciesDominantHeight", is(11f)));
			assertThat(unit, hasProperty("primarySpeciesSiteIndex", is(12f)));
			assertThat(unit, hasProperty("primarySpeciesTotalAge", is(13f)));
			assertThat(unit, hasProperty("primarySpeciesAgeAtBreastHeight", is(14f)));
			assertThat(unit, hasProperty("primarySpeciesAgeToBreastHeight", is(15f)));

			bank = unit.getBank();

			// Bank was already set so no change

			assertThat("bank.dominantHeights", bank.dominantHeights[0], is(21f));
			assertThat("bank.siteIndices", bank.siteIndices[0], is(22f));
			assertThat("bank.ageTotals", bank.ageTotals[0], is(23f));
			assertThat("bank.yearsAtBreastHeight", bank.yearsAtBreastHeight[0], is(24f));
			assertThat("bank.yearsToBreastHeight", bank.yearsToBreastHeight[0], is(25f));
		}

		@Test
		void testSetTwiceDisallowed() {
			unit.setPrimarySpeciesDetails(new PrimarySpeciesDetails(11f, 12f, 13f, 14f, 15f));
			var ex = assertThrows(
					IllegalStateException.class,
					() -> unit.setPrimarySpeciesDetails(new PrimarySpeciesDetails(21f, 22f, 23f, 24f, 25f))
			);
			assertThat(
					ex, hasProperty("message", is(LayerProcessingState.PRIMARY_SPECIES_DETAILS_CAN_BE_SET_ONCE_ONLY))
			);

			// No change to properties

			assertThat(unit, hasProperty("primarySpeciesDominantHeight", is(11f)));
			assertThat(unit, hasProperty("primarySpeciesSiteIndex", is(12f)));
			assertThat(unit, hasProperty("primarySpeciesTotalAge", is(13f)));
			assertThat(unit, hasProperty("primarySpeciesAgeAtBreastHeight", is(14f)));
			assertThat(unit, hasProperty("primarySpeciesAgeToBreastHeight", is(15f)));

			var bank = unit.getBank();

			// No change to bank

			assertThat("bank.dominantHeights", bank.dominantHeights[0], is(11f));
			assertThat("bank.siteIndices", bank.siteIndices[0], is(12f));
			assertThat("bank.ageTotals", bank.ageTotals[0], is(13f));
			assertThat("bank.yearsAtBreastHeight", bank.yearsAtBreastHeight[0], is(14f));
			assertThat("bank.yearsToBreastHeight", bank.yearsToBreastHeight[0], is(15f));
		}

		@Test
		void testSetTwiceAllowed() throws Exception {
			controlVariables.setControlVariable(ControlVariable.UPDATE_DURING_GROWTH_6, 1);
			unit.setPrimarySpeciesDetails(new PrimarySpeciesDetails(11f, 12f, 13f, 14f, 15f));
			assertDoesNotThrow(() -> unit.setPrimarySpeciesDetails(new PrimarySpeciesDetails(21f, 22f, 23f, 24f, 25f)));

			// Properties should change

			assertThat(unit, hasProperty("primarySpeciesDominantHeight", is(21f)));
			assertThat(unit, hasProperty("primarySpeciesSiteIndex", is(22f)));
			assertThat(unit, hasProperty("primarySpeciesTotalAge", is(23f)));
			assertThat(unit, hasProperty("primarySpeciesAgeAtBreastHeight", is(24f)));
			assertThat(unit, hasProperty("primarySpeciesAgeToBreastHeight", is(25f)));

			var bank = unit.getBank();

			// But bank should not

			assertThat("bank.dominantHeights", bank.dominantHeights[0], is(11f));
			assertThat("bank.siteIndices", bank.siteIndices[0], is(12f));
			assertThat("bank.ageTotals", bank.ageTotals[0], is(13f));
			assertThat("bank.yearsAtBreastHeight", bank.yearsAtBreastHeight[0], is(14f));
			assertThat("bank.yearsToBreastHeight", bank.yearsToBreastHeight[0], is(15f));
		}

	}
}
