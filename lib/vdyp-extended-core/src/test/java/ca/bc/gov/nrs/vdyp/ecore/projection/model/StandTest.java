package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StandTest {

	@Test
	void testDetermineSpeciesAgeAtYearReturnsCalculatedAgeFromMeasurementYear() {
		var stand = buildStand(2020, null, 10.6);

		assertEquals(16, stand.determineSpeciesAgeAtYear(2025));
	}

	@Test
	void testDetermineSpeciesAgeAtYearReturnsNullWhenSpeciesGroupAgeMissing() {
		var stand = buildStand(2020, null, null);

		assertNull(stand.determineSpeciesAgeAtYear(2025));
	}

	@Test
	void testDetermineSpeciesAgeAtYearReturnsNullWhenMeasurementYearNotPositive() {
		var stand = buildStand(null, null, 10.6);

		assertNull(stand.determineSpeciesAgeAtYear(2025));
	}

	@Test
	void testDetermineSpeciesAgeAtYearReturnsNullWhenRoundedSpeciesAgeNotPositive() {
		var stand = buildStand(2020, null, 0.4);

		assertNull(stand.determineSpeciesAgeAtYear(2025));
	}

	@Test
	void testDetermineSpeciesAgeAtYearUsesPolygonMeasurementYear() {
		var stand = buildStand(2020, 2023, 10.6);

		assertEquals(13, stand.determineSpeciesAgeAtYear(2025));
	}

	private Stand buildStand(Integer referenceYear, Integer yearOfDeath, Double totalAge) {
		var polygonBuilder = new Polygon.Builder();
		if (referenceYear != null) {
			polygonBuilder.referenceYear(referenceYear);
		}
		if (yearOfDeath != null) {
			polygonBuilder.yearOfDeath(yearOfDeath);
		}

		var polygon = polygonBuilder.build();
		var layer = new Layer.Builder().polygon(polygon).layerId("1").build();
		var stand = new Stand.Builder().layer(layer).sp0Code("P").build();

		var speciesGroupBuilder = new Species.Builder().stand(stand).speciesCode("PL").speciesPercent(100.0);
		if (totalAge != null) {
			speciesGroupBuilder.totalAge(totalAge);
		}

		stand.addSpeciesGroup(speciesGroupBuilder.build(), 0);
		layer.addStand(stand);

		return stand;
	}
}
