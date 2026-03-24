package ca.bc.gov.nrs.vdyp.vri.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.model.LayerType;

class VriLayerTest {

	@Test
	void testGetPriorityOrderedSites() {
		var result = VriLayer.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.PRIMARY);
			builder.primaryGenus("PL");
			builder.crownClosure(55.0f);
			builder.utilization(7.5f);
			builder.addSpecies(sb -> {
				sb.genus("AC", 1);
				sb.percentGenus(45);
				sb.addSite(ib -> {
					ib.height(10f);
					ib.ageTotal(42f);
					ib.yearsToBreastHeight(2f);
					ib.yearsAtBreastHeightAuto();
					ib.siteCurveNumber(0);
					ib.siteSpecies("AC");
				});
			});
			builder.addSpecies(sb -> {
				sb.genus("PL", 12);
				sb.percentGenus(45);
				sb.addSite(ib -> {
					ib.height(10f);
					ib.ageTotal(42f);
					ib.yearsToBreastHeight(2f);
					ib.yearsAtBreastHeightAuto();
					ib.siteCurveNumber(0);
					ib.siteSpecies("P");
				});
			});
			builder.addSpecies(sb -> {
				sb.genus("B", 8);
				sb.percentGenus(10);
				sb.addSite(ib -> {
					ib.height(10f);
					ib.ageTotal(42f);
					ib.yearsToBreastHeight(2f);
					ib.yearsAtBreastHeightAuto();
					ib.siteCurveNumber(0);
					ib.siteSpecies("B");
				});
			});
		});

		List<VriSite> sites = result.getPriorityOrderedSites();
		assertThat(
				sites, contains(
						hasProperty("siteGenus", is("PL")), //
						hasProperty("siteGenus", is("AC")), //
						hasProperty("siteGenus", is("B"))
				)
		);
	}
}
