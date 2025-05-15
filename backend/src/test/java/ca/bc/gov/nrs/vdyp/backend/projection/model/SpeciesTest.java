package ca.bc.gov.nrs.vdyp.backend.projection.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;

class SpeciesTest {

	@Nested
	class EquivalentSiteInfoSpecies {

		Polygon polygon;
		Layer layer;
		Stand stand;

		@BeforeEach
		void setup() {
			polygon = new Polygon.Builder()
					.build();
			layer = new Layer.Builder()
					.polygon(polygon)
					.layerId("Test")
					.build();
			stand = new Stand.Builder()
					.sp0Code("P")
					.layer(layer)
					.build();
		}

		Species.Builder baseConfig(Species.Builder builder) {
			return builder
					.stand(stand)
					.speciesCode("PL")
					.speciesPercent(70)
					.ageAtBreastHeight(39d)
					.totalAge(42d)
					.yearsToBreastHeight(3d)
					.siteIndex(2d)
					.siteCurve(SiteIndexEquation.SI_ACB_HUANG)
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
					b -> b.speciesCode("PY"),
					b -> b.speciesPercent(76d),

					// For these fields, null matches everything
					b -> b.ageAtBreastHeight(null),
					b -> b.totalAge(null),
					b -> b.yearsToBreastHeight(null),
					b -> b.siteIndex(null),
					b -> b.siteCurve(null),
					b -> b.dominantHeight(null)
			);
		}

		static List<UnaryOperator<Species.Builder>> relevantChanges() {
			return List.of(
					b -> b.ageAtBreastHeight(100d),
					b -> b.totalAge(100d),
					b -> b.yearsToBreastHeight(100d),
					b -> b.siteIndex(100d),
					b -> b.siteCurve(SiteIndexEquation.SI_ACT_THROWER),
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
		void testRrelevantChangeToCollaborator(UnaryOperator<Species.Builder> mutation) {
			var unit = baseConfig(new Species.Builder()).build();
			var collaborator = mutation.apply(baseConfig(new Species.Builder())).build();

			assertThat(unit.equivalentSiteInfo(collaborator), is(false));
		}
	}

}
