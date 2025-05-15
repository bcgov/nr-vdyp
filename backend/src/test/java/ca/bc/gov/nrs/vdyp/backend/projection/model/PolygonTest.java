package ca.bc.gov.nrs.vdyp.backend.projection.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

public class PolygonTest {

	@Nested
	class TweakSpeciesPercentages {
		Polygon polygon;

		Layer layer;

		@BeforeEach
		void setup() {
			polygon = new Polygon.Builder().build();

			layer = new Layer.Builder().polygon(polygon).layerId("Test").build();
		}

		@Test
		void noSpecies() {
			var message = polygon.tweakPercentages(layer);

			// No error

			assertThat(message, notPresent());

			// No change

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(0));
		}

		@Test
		void oneStandExact() {
			var sp64Code = "PL";
			var sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

			var stand = new Stand.Builder() //
					.sp0Code("P") //
					.layer(layer) //
					.build();

			var sp64 = new Species.Builder() //
					.stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(100d) //
					.totalAge(40d) //
					.dominantHeight(20d) //
					.build();

			Species sp0 = new Species.Builder().stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(0) //
					.build();
			stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
			layer.addStand(stand);

			sp64.calculateUndefinedFieldValues();

			stand.addSp64(sp64);
			layer.addSp64(sp64);

			var message = polygon.tweakPercentages(layer);

			// No error

			assertThat(message, notPresent());

			// No change

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(1));
			assertThat(layer.getSp0sAsSupplied().get(0), hasProperty("speciesByPercent", iterableWithSize(1)));
			assertThat(
					layer.getSp0sAsSupplied().get(0).getSpeciesByPercent().get(0), hasProperty(
							"speciesPercent", closeTo(100d, 0.1d)
					)
			);

		}

		@Test
		void oneStandSlightlyOver() {
			var sp64Code = "PL";
			var sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

			var stand = new Stand.Builder() //
					.sp0Code("P") //
					.layer(layer) //
					.build();

			var sp64 = new Species.Builder() //
					.stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(100.5d) //
					.totalAge(40d) //
					.dominantHeight(20d) //
					.build();

			Species sp0 = new Species.Builder().stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(0) //
					.build();
			stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
			layer.addStand(stand);

			sp64.calculateUndefinedFieldValues();

			stand.addSp64(sp64);
			layer.addSp64(sp64);

			var message = polygon.tweakPercentages(layer);

			// No error

			assertThat(message, notPresent());

			// Corrected

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(1));
			assertThat(layer.getSp0sAsSupplied().get(0), hasProperty("speciesByPercent", iterableWithSize(1)));
			assertThat(
					layer.getSp0sAsSupplied().get(0).getSpeciesByPercent().get(0), hasProperty(
							"speciesPercent", closeTo(100d, 0.1d)
					)
			);

		}

		@Test
		void oneStandSlightlyUnder() {
			var sp64Code = "PL";
			var sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

			var stand = new Stand.Builder() //
					.sp0Code("P") //
					.layer(layer) //
					.build();

			var sp64 = new Species.Builder() //
					.stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(99.75d) //
					.totalAge(40d) //
					.dominantHeight(20d) //
					.build();

			Species sp0 = new Species.Builder().stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(0) //
					.build();
			stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
			layer.addStand(stand);

			sp64.calculateUndefinedFieldValues();

			stand.addSp64(sp64);
			layer.addSp64(sp64);

			var message = polygon.tweakPercentages(layer);

			// No error

			assertThat(message, notPresent());

			// Corrected

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(1));
			assertThat(layer.getSp0sAsSupplied().get(0), hasProperty("speciesByPercent", iterableWithSize(1)));
			assertThat(
					layer.getSp0sAsSupplied().get(0).getSpeciesByPercent().get(0), hasProperty(
							"speciesPercent", closeTo(100d, 0.1d)
					)
			);

		}

		@Test
		void oneStandWayOver() {
			var sp64Code = "PL";
			var sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

			var stand = new Stand.Builder() //
					.sp0Code("P") //
					.layer(layer) //
					.build();

			var sp64 = new Species.Builder() //
					.stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(102d) //
					.totalAge(40d) //
					.dominantHeight(20d) //
					.build();

			Species sp0 = new Species.Builder().stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(0) //
					.build();
			stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
			layer.addStand(stand);

			sp64.calculateUndefinedFieldValues();

			stand.addSp64(sp64);
			layer.addSp64(sp64);

			var message = polygon.tweakPercentages(layer);

			// Error

			assertThat(message, present(hasProperty("kind", is(PolygonMessageKind.LAYER_PERCENTAGES_TOO_INACCURATE))));

			// No change

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(1));
			assertThat(layer.getSp0sAsSupplied().get(0), hasProperty("speciesByPercent", iterableWithSize(1)));
			assertThat(
					layer.getSp0sAsSupplied().get(0).getSpeciesByPercent().get(0), hasProperty(
							"speciesPercent", closeTo(102, 0.1d)
					)
			);

		}

		@Test
		void oneStandWayUnder() {
			var sp64Code = "PL";
			var sp0Code = SiteTool.getSpeciesVDYP7Code(sp64Code);

			var stand = new Stand.Builder() //
					.sp0Code("P") //
					.layer(layer) //
					.build();

			var sp64 = new Species.Builder() //
					.stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(98d) //
					.totalAge(40d) //
					.dominantHeight(20d) //
					.build();

			Species sp0 = new Species.Builder().stand(stand) //
					.speciesCode(sp64Code) //
					.speciesPercent(0) //
					.build();
			stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());
			layer.addStand(stand);

			sp64.calculateUndefinedFieldValues();

			stand.addSp64(sp64);
			layer.addSp64(sp64);

			var message = polygon.tweakPercentages(layer);

			// Error

			assertThat(message, present(hasProperty("kind", is(PolygonMessageKind.LAYER_PERCENTAGES_TOO_INACCURATE))));

			// No change

			assertThat(layer.getSp0sAsSupplied(), iterableWithSize(1));
			assertThat(layer.getSp0sAsSupplied().get(0), hasProperty("speciesByPercent", iterableWithSize(1)));
			assertThat(
					layer.getSp0sAsSupplied().get(0).getSpeciesByPercent().get(0), hasProperty(
							"speciesPercent", closeTo(98d, 0.1d)
					)
			);

		}
	}
}
