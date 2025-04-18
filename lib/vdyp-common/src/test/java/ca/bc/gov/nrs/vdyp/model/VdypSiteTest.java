package ca.bc.gov.nrs.vdyp.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.isPolyId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class VdypSiteTest {

	@Test
	void build() {
		var site1 = VdypSite.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.PRIMARY);
			builder.ageTotal(35.0f);
			builder.height(53.0f);
			builder.siteCurveNumber(22);
			builder.siteGenus("B");
			builder.siteIndex(42.5f);
			builder.yearsToBreastHeight(5.0f);
		});
		assertThat(site1, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(site1, hasProperty("layerType", is(LayerType.PRIMARY)));
		assertThat(site1, hasProperty("ageTotal", is(Optional.of(35.0f))));
		assertThat(site1, hasProperty("height", is(Optional.of(53.0f))));
		assertThat(site1, hasProperty("siteCurveNumber", is(Optional.of(22))));
		assertThat(site1, hasProperty("siteGenus", is("B")));
		assertThat(site1, hasProperty("siteIndex", is(Optional.of(42.5f))));
		assertThat(site1, hasProperty("yearsToBreastHeight", is(Optional.of(5.0f))));
		assertThat(site1, hasProperty("yearsAtBreastHeight", is(Optional.of(30.0f))));

		assertThat(site1.toString(), is("Test(2024)-PRIMARY-B"));
		assertThat(site1.equals(site1), is(true));

		var site2 = VdypSite.build(builder -> {
			builder.polygonIdentifier("Test2", 2024);
			builder.layerType(LayerType.PRIMARY);
			builder.ageTotal(35.0f);
			builder.height(53.0f);
			builder.siteCurveNumber(22);
			builder.siteGenus("B");
			builder.siteIndex(42.5f);
			builder.yearsToBreastHeight(5.0f);
		});

		assertThat(site1.equals(site2), is(false));

		var site3 = VdypSite.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.PRIMARY);
			builder.ageTotal(35.0f);
			builder.height(53.0f);
			builder.siteCurveNumber(22);
			builder.siteGenus("C");
			builder.siteIndex(42.5f);
			builder.yearsToBreastHeight(5.0f);
		});

		assertThat(site1.equals(site3), is(false));

		var site4 = VdypSite.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.VETERAN);
			builder.ageTotal(35.0f);
			builder.height(53.0f);
			builder.siteCurveNumber(22);
			builder.siteGenus("B");
			builder.siteIndex(42.5f);
			builder.yearsToBreastHeight(5.0f);
		});

		assertThat(site1.equals(site4), is(false));
	}

	@Test
	void buildNoProperties() {
		var ex = assertThrows(IllegalStateException.class, () -> VdypSite.build(builder -> {
		}));
		assertThat(
				ex,
				hasProperty(
						"message",
						allOf(
								containsString("polygonIdentifier"), containsString("layerType"),
								containsString("siteGenus")
						)
				)
		);
	}

	@Test
	void buildCopy() {
		var result = VdypSite.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.PRIMARY);
			builder.ageTotal(35.0f);
			builder.height(53.0f);
			builder.siteCurveNumber(22);
			builder.siteGenus("B");
			builder.siteIndex(42.5f);
			builder.yearsToBreastHeight(5.0f);
		});

		var copiedResult = VdypSite.build(builder -> builder.copy(result));

		assertThat(copiedResult, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(copiedResult, hasProperty("layerType", is(LayerType.PRIMARY)));
		assertThat(copiedResult, hasProperty("ageTotal", is(Optional.of(35.0f))));
		assertThat(copiedResult, hasProperty("height", is(Optional.of(53.0f))));
		assertThat(copiedResult, hasProperty("siteCurveNumber", is(Optional.of(22))));
		assertThat(copiedResult, hasProperty("siteGenus", is("B")));
		assertThat(copiedResult, hasProperty("siteIndex", is(Optional.of(42.5f))));
		assertThat(copiedResult, hasProperty("yearsToBreastHeight", is(Optional.of(5.0f))));
		assertThat(copiedResult, hasProperty("yearsAtBreastHeight", is(Optional.of(30.0f))));
	}
}
