package ca.bc.gov.nrs.vdyp.backend.projection.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;

class SpeciesTest {
	private static final double ERROR_TOLERANCE = 0.00001;

	@Test
	void testInvalidSpeciesBuilder() {
		var polygon = new Polygon.Builder().build();
		var layer = new Layer.Builder().polygon(polygon).layerId("Test").build();
		var stand = new Stand.Builder().sp0Code("P").layer(layer).build();
		var speciesBuilder =
		assertThrows(IllegalStateException.class, () -> new Species.Builder().speciesCode("PL").build());
		assertThrows(IllegalStateException.class, () -> new Species.Builder().stand(stand).build());
		assertThrows(IllegalStateException.class, () -> new Species.Builder().stand(stand).speciesCode("PL").build());
	}

	@Nested
	class EquivalentSiteInfoSpecies {

		Polygon polygon;
		Layer layer;
		Stand stand;

		@BeforeEach
		void setup() {
			polygon = new Polygon.Builder().build();
			layer = new Layer.Builder().polygon(polygon).layerId("Test").build();
			stand = new Stand.Builder().sp0Code("P").layer(layer).build();
		}

		Species.Builder baseConfig(Species.Builder builder) {
			return builder.stand(stand).speciesCode("PL").speciesPercent(70).ageAtBreastHeight(39d).totalAge(42d)
					.yearsToBreastHeight(3d).siteIndex(2d).siteCurve(SiteIndexEquation.SI_ACB_HUANG)
					.dominantHeight(20d);
		}

		@Test
		void testIdentical() {
			var unit = baseConfig(new Species.Builder()).build();
			var collaborator = baseConfig(new Species.Builder()).build();

			assertThat(unit.equivalentSiteInfo(collaborator), is(true));
		}

		static List<UnaryOperator<Species.Builder>> irrelevantChanges() {
			return List.of(
					// These fields are ignored
					b -> b.speciesCode("PY"), b -> b.speciesPercent(76d),

					// For these fields, null matches everything
					b -> b.ageAtBreastHeight(null), b -> b.totalAge(null), b -> b.yearsToBreastHeight(null),
					b -> b.siteIndex(null), b -> b.siteCurve(null), b -> b.dominantHeight(null)
			);
		}

		static List<UnaryOperator<Species.Builder>> relevantChanges() {
			return List.of(
					b -> b.ageAtBreastHeight(100d), b -> b.totalAge(100d), b -> b.yearsToBreastHeight(100d),
					b -> b.siteIndex(100d), b -> b.siteCurve(SiteIndexEquation.SI_ACT_THROWER),
					b -> b.dominantHeight(100d)
			);
		}

		@ParameterizedTest
		@MethodSource("irrelevantChanges")
		void testIrrelevantChangeToReceiver(UnaryOperator<Species.Builder> mutation) {
			var unit = mutation.apply(baseConfig(new Species.Builder())).build();
			var collaborator = baseConfig(new Species.Builder()).build();

			assertThat(unit.equivalentSiteInfo(collaborator), is(true));
		}

		@ParameterizedTest
		@MethodSource("relevantChanges")
		void testRrelevantChangeToReceiver(UnaryOperator<Species.Builder> mutation) {
			var unit = mutation.apply(baseConfig(new Species.Builder())).build();
			var collaborator = baseConfig(new Species.Builder()).build();

			assertThat(unit.equivalentSiteInfo(collaborator), is(false));
		}

		@ParameterizedTest
		@MethodSource("irrelevantChanges")
		void testIrrelevantChangeToCollaborator(UnaryOperator<Species.Builder> mutation) {
			var unit = baseConfig(new Species.Builder()).build();
			var collaborator = mutation.apply(baseConfig(new Species.Builder())).build();

			assertThat(unit.equivalentSiteInfo(collaborator), is(true));
		}

		@ParameterizedTest
		@MethodSource("relevantChanges")
		void testRelevantChangeToCollaborator(UnaryOperator<Species.Builder> mutation) {
			var unit = baseConfig(new Species.Builder()).build();
			var collaborator = mutation.apply(baseConfig(new Species.Builder())).build();

			assertThat(unit.equivalentSiteInfo(collaborator), is(false));
		}

	}

	@Nested
	class UndefinedValueCalculations {

		//
		Polygon polygon;
		Layer layer;
		Stand stand;

		@BeforeEach
		void setup() {
			polygon = new Polygon.Builder().build();
			layer = new Layer.Builder().polygon(polygon).layerId("Test").build();
			stand = new Stand.Builder().sp0Code("P").layer(layer).build();
		}

		Species.Builder baseConfig(Species.Builder builder) {
			return builder.stand(stand).speciesCode("PL").speciesPercent(70);
		}

		@Test
		void testInsufficientData() {
			var unit = baseConfig(new Species.Builder()).siteIndex(Vdyp7Constants.EMPTY_DECIMAL)
					.dominantHeight(Vdyp7Constants.EMPTY_DECIMAL).totalAge(Vdyp7Constants.EMPTY_DECIMAL)
					.yearsToBreastHeight(Vdyp7Constants.EMPTY_DECIMAL).build();

			unit.calculateUndefinedFieldValues();

			assertThat(unit.getSiteIndex(), is((Double) null));
			assertThat(unit.getDominantHeight(), is((Double) null));
			assertThat(unit.getTotalAge(), is((Double) null));
			assertThat(unit.getYearsToBreastHeight(), is((Double) null));

			unit.setSiteCurve(SiteIndexEquation.SI_ACB_HUANG);

			unit.calculateUndefinedFieldValues();

			assertThat(unit.getSiteIndex(), is((Double) null));
			assertThat(unit.getDominantHeight(), is((Double) null));
			assertThat(unit.getTotalAge(), is((Double) null));
			assertThat(unit.getYearsToBreastHeight(), is((Double) null));

		}

		@Test
		void testMinimalTotalAgeCalculation() {
			var unit = baseConfig(new Species.Builder()).ageAtBreastHeight(42.0).yearsToBreastHeight(42.0).build();

			unit.calculateUndefinedFieldValues();

			assertThat(unit.getTotalAge(), is(83.5));

		}

		@Test
		void testMinimalYearsToBreastHeight() {
			var unit = baseConfig(new Species.Builder()).totalAge(42.0).yearsToBreastHeight(21.0).build();

			unit.calculateUndefinedFieldValues();

			assertThat(unit.getTotalAge(), is(42.0));

		}

		static Stream<Arguments> FIPStartNonProductiveParams() {
			return Stream.of(
					Arguments.of(0.6, 0.5, "NP", 0.1), Arguments.of(0.5, 0.5, "AF", 0.1),
					Arguments.of(0.5, 0.5, "UNK", 0.5), Arguments.of(1.6, 0.5, "UNK", 0.5),
					Arguments.of(1.6, 0.5, "AF", 0.5)
			);
		}

		@ParameterizedTest
		@MethodSource("FIPStartNonProductiveParams")
		void testFIPStartNonProductive(
				double totalAge, double yearsToBreastHeight, String nonProductiveDescriptor, double expectedValue
		) {
			// Create new base definitions so the polygon in this case is FIP nonproductive
			var polygon = new Polygon.Builder().inventoryStandard(InventoryStandard.FIP)
					.nonProductiveDescriptor(nonProductiveDescriptor).build();
			var layer = new Layer.Builder().polygon(polygon).layerId("Test").build();
			var stand = new Stand.Builder().sp0Code("P").layer(layer).build();

			var unit = baseConfig(new Species.Builder()).stand(stand).totalAge(totalAge)
					.yearsToBreastHeight(yearsToBreastHeight).build();

			unit.calculateUndefinedFieldValues();

			assertThat(unit.getYearsToBreastHeight(), is(expectedValue));
		}

		// Values verified via VDYP7 Testing
		static Stream<Arguments> siteIndexFromAgeAndDominantHeight() {
			return Stream.of(
					Arguments.of(30.0, 1.3, 2.6), // force an invalid computed siteindex
					Arguments.of(2000.0, 2.0, null), // Trigger Business logic for too low Site index
					Arguments.of(10.0, 2.0, null), // age too low to trigger site index calculation
					Arguments.of(42.0, 42.0, 46.51) // randomly chosen data for a valid computation
			);
		}

		@ParameterizedTest
		@MethodSource("siteIndexFromAgeAndDominantHeight")
		void testSiteIndexFromAgeAndDominantHeight(Double totalAge, Double dominantHeight, Double expectedSiteIndex) {
			var unit = baseConfig(new Species.Builder()).totalAge(totalAge).dominantHeight(dominantHeight)
					.siteIndex(null).build();

			unit.calculateUndefinedFieldValues();

			assertThat(unit.getSiteIndex(), is(expectedSiteIndex));
			if (expectedSiteIndex == null) {
				// for failed calculations projection should be disabled and there should be a warning message
				assertThat(polygon.doAllowProjectionOfType(layer.determineProjectionType(polygon)), is(false));
			}
		}

		// NOTE This is not how data is passed via the HCSV so these tests are more about setting values at all
		// rather than the values being correct. Value correctness is handled by lower level unit tests
		static Stream<Arguments> ageFromDominantHeightAndSiteIndex() {
			return Stream.of(
					Arguments.of(42.0, 10.0, 1.9, SiteIndexEquation.SI_PLI_NIGHGI97, null),
					Arguments.of(42.0, 12.0, 2.0, SiteIndexEquation.SI_ACT_THROWERAC, 430.27343),
					Arguments.of(42.0, 15.0, 2.1, SiteIndexEquation.SI_ACB_HUANG, null),
					Arguments.of(42.0, 12.0, 42.0, SiteIndexEquation.SI_ACT_THROWERAC, 50.58593)
			);
		}

		@ParameterizedTest
		@MethodSource("ageFromDominantHeightAndSiteIndex")
		void testDetermineAgeFromDominantHeightAndSiteIndex(
				Double yearsToBreastHeight, Double dominantHeight, Double siteIndex, SiteIndexEquation siteCurve,
				Double expected
		) {
			var unit = baseConfig(new Species.Builder()).totalAge(null).yearsToBreastHeight(yearsToBreastHeight)
					.dominantHeight(dominantHeight).siteIndex(siteIndex).siteCurve(siteCurve).build();

			unit.calculateUndefinedFieldValues();

			if (expected == null) {
				assertThat(unit.getTotalAge(), is(expected));
				assertThat(polygon.doAllowProjectionOfType(layer.determineProjectionType(polygon)), is(false));
			} else {
				assertThat(unit.getTotalAge(), closeTo(expected, ERROR_TOLERANCE));
			}
		}

		// NOTE This is not how data is passed via the HCSV so these tests are more about setting values at all
		// rather than the values being correct. Value correctness is handled by lower level unit tests
		static Stream<Arguments> dominantHeightFromAgeAndSiteIndex() {
			return Stream.of(
					Arguments.of(42.0, 42.0, 1.9, SiteIndexEquation.SI_ACB_HUANG, 0.01), // siteindex too low
					Arguments.of(null, 42.0, 4.0, SiteIndexEquation.SI_ACB_HUANG, 1.506711), // siteindex too low
					Arguments.of(10.0, 42.0, 42.0, SiteIndexEquation.SI_PLI_NIGHGI97, 34.94531) // valid example
			);
		}

		@ParameterizedTest
		@MethodSource("dominantHeightFromAgeAndSiteIndex")
		void testDetermineDominantHeightFromAgeAndSiteIndex(
				Double yearsToBreastHeight, Double age, Double siteIndex, SiteIndexEquation siteCurve, Double expected
		) {
			var unit = baseConfig(new Species.Builder()).dominantHeight(null).yearsToBreastHeight(yearsToBreastHeight)
					.totalAge(age).siteIndex(siteIndex).siteCurve(siteCurve).build();

			unit.calculateUndefinedFieldValues();

			if (expected == 0.01) {
				// dominant height is set nominally on failure
				assertThat(unit.getDominantHeight(), is(expected));
				assertThat(polygon.doAllowProjectionOfType(layer.determineProjectionType(polygon)), is(false));
			} else {
				assertThat(unit.getDominantHeight(), closeTo(expected, ERROR_TOLERANCE));
			}
		}

		@ParameterizedTest
		@MethodSource("dominantHeightFromAgeAndSiteIndex")
		void testDirectDetermineDominantHeightFromAgeAndSiteIndex(
				Double yearsToBreastHeight, Double age, Double siteIndex, SiteIndexEquation siteCurve, Double expected
		) {
			var unit = baseConfig(new Species.Builder()).dominantHeight(null).yearsToBreastHeight(yearsToBreastHeight)
					.totalAge(age).siteIndex(siteIndex).siteCurve(siteCurve).build();

			// Direct Call is supported and gets around some bad state handling provided by calculateUndefinedValues
			unit.setDominantHeight(unit.determineDominantHeightFromAgeAndSiteIndex(null, null, null, null));

			if (expected == 0.01) {
				// dominant height is set nominally on failure
				assertThat(unit.getDominantHeight(), is((Double) null));
			} else {
				assertThat(unit.getDominantHeight(), closeTo(expected, ERROR_TOLERANCE));
			}
		}

		@Test
		void testAddDuplicate() {
			var polygon = new Polygon.Builder().build();
			var layer = new Layer.Builder().polygon(polygon).layerId("1").build();
			var stand = new Stand.Builder().sp0Code("P").layer(layer).build();

			double speciesPercent = 25;
			var unit = new Species.Builder().stand(stand).speciesCode("PL").speciesPercent(25).build();
			var collaborator = new Species.Builder().stand(stand).speciesCode("PL").speciesPercent(25).totalAge(42.0)
					.dominantHeight(42.0).siteIndex(42.0).siteCurve(SiteIndexEquation.SI_ACB_HUANG)
					.yearsToBreastHeight(42.0).ageAtBreastHeight(42.0).build();

			unit.addDuplicate(collaborator);
			assertThat(unit.getSpeciesPercent(), is(speciesPercent * 2));
			assertThat(unit.getTotalAge(), is(collaborator.getTotalAge()));
			assertThat(unit.getDominantHeight(), is(collaborator.getDominantHeight()));
			assertThat(unit.getSiteIndex(), is(collaborator.getSiteIndex()));
			assertThat(unit.getYearsToBreastHeight(), is(collaborator.getYearsToBreastHeight()));
			assertThat(unit.getAgeAtBreastHeight(), is(collaborator.getAgeAtBreastHeight()));
			assertThat(unit.getSiteCurve(), is(collaborator.getSiteCurve()));
			assertThat(unit.getPercentsPerDuplicate().size(), is(1));
			assertThat(unit.getPercentsPerDuplicate().get(0), is(speciesPercent));

			unit.addDuplicate(collaborator);
			assertThat(unit.getSpeciesPercent(), is(speciesPercent * 3));
			assertThat(unit.getTotalAge(), is(collaborator.getTotalAge()));
			assertThat(unit.getDominantHeight(), is(collaborator.getDominantHeight()));
			assertThat(unit.getSiteIndex(), is(collaborator.getSiteIndex()));
			assertThat(unit.getYearsToBreastHeight(), is(collaborator.getYearsToBreastHeight()));
			assertThat(unit.getAgeAtBreastHeight(), is(collaborator.getAgeAtBreastHeight()));
			assertThat(unit.getSiteCurve(), is(collaborator.getSiteCurve()));
			assertThat(unit.getPercentsPerDuplicate().size(), is(2));
			assertThat(unit.getPercentsPerDuplicate().get(1), is(speciesPercent));
		}
	}
}
