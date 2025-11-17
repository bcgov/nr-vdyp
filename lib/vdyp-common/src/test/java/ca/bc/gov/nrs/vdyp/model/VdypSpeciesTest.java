package ca.bc.gov.nrs.vdyp.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.isPolyId;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.InitializationIncompleteException;

class VdypSpeciesTest {

	@Test
	void build() {
		var species1 = VdypSpecies.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.PRIMARY);
			builder.genus("B", 3);
			builder.percentGenus(50f);
			builder.volumeGroup(1);
			builder.decayGroup(2);
			builder.breakageGroup(3);
		});
		assertThat(species1, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(species1, hasProperty("layerType", is(LayerType.PRIMARY)));
		assertThat(species1, hasProperty("genus", is("B")));
		assertThat(species1, hasProperty("percentGenus", is(50f)));
		assertThat(species1, hasProperty("volumeGroup", is(1)));
		assertThat(species1, hasProperty("decayGroup", is(2)));
		assertThat(species1, hasProperty("breakageGroup", is(3)));
		assertThat(species1, hasProperty("sp64DistributionSet", hasProperty("sp64DistributionMap", anEmptyMap())));

		assertThat(species1.toString(), is("Test(2024)-PRIMARY-B"));
		assertThat(species1.equals(species1), is(true));

		var species2 = VdypSpecies.build(builder -> {
			builder.polygonIdentifier("Test2", 2024);
			builder.layerType(LayerType.PRIMARY);
			builder.genus("B", 3);
			builder.percentGenus(50f);
			builder.volumeGroup(1);
			builder.decayGroup(2);
			builder.breakageGroup(3);
		});

		assertThat(species1.equals(species2), is(false));

		var species3 = VdypSpecies.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.PRIMARY);
			builder.genus("D", 5);
			builder.percentGenus(50f);
			builder.volumeGroup(1);
			builder.decayGroup(2);
			builder.breakageGroup(3);
		});

		assertThat(species1.equals(species3), is(false));

		var species4 = VdypSpecies.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.VETERAN);
			builder.genus("B", 3);
			builder.percentGenus(50f);
			builder.volumeGroup(1);
			builder.decayGroup(2);
			builder.breakageGroup(3);
		});

		assertThat(species1.equals(species4), is(false));
	};

	@Test
	void testAdditionalBuildMethods() {
		VdypSpecies sp = VdypSpecies.build(sb -> {
			sb.polygonIdentifier(new PolygonIdentifier("Poly1", 2024));
			sb.layerType(LayerType.PRIMARY);
			sb.percentGenus(100.0f);
			sb.genus("Species1", 5);
			sb.baseArea(0.00155f, 0.01412f, 0.05128f, 0.45736f, 28.77972f);
			sb.treesPerHectare(0.47f, 1.64f, 2.69f, 13.82f, 269.56f);
			sb.loreyHeight(10.6033f, 33.7440f);
			sb.wholeStemVolume(0.0078f, 0.1091f, 0.5602f, 6.0129f, 452.8412f);
			sb.quadMeanDiameter(6.5f, 36.0f, 10.5f, 15.6f, 20.5f, 36.9f);
			sb.closeUtilizationVolumeByUtilization(0.0078f, 0.1091f, 0.5602f, 6.0129f, 452.8412f);
			sb.closeUtilizationVolumeNetOfDecayByUtilization(0.0000f, 0.0571f, 0.5048f, 5.6414f, 437.8810f);
			sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(0.0000f, 0.0566f, 0.5007f, 5.5975f, 430.3732f);
			sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					0.0000f, 0.0565f, 0.5005f, 5.5948f, 429.1300f
			);
		});

		assertThrows(NoSuchElementException.class, () -> sp.getVolumeGroup());
		assertThrows(NoSuchElementException.class, () -> sp.getDecayGroup());
		assertThrows(NoSuchElementException.class, () -> sp.getBreakageGroup());

		sp.setVolumeGroup(0);
		sp.setDecayGroup(0);
		sp.setBreakageGroup(0);

		assertThrows(IllegalStateException.class, () -> sp.setVolumeGroup(1));
		assertThrows(IllegalStateException.class, () -> sp.setDecayGroup(1));
		assertThrows(IllegalStateException.class, () -> sp.setBreakageGroup(1));

		List<UtilizationClass> ucs = Arrays.asList(UtilizationClass.values());
		List<VolumeVariable> vvs = Arrays.asList(VolumeVariable.values());
		List<LayerType> lts = Arrays.asList(LayerType.values());

		assertThrows(InitializationIncompleteException.class, () -> sp.getCvVolume(null, null, null));
		assertThrows(InitializationIncompleteException.class, () -> sp.getCvBasalArea(null, null));
		assertThrows(InitializationIncompleteException.class, () -> sp.getCvQuadraticMeanDiameter(null, null));
		assertThrows(InitializationIncompleteException.class, () -> sp.getCvPrimaryLayerSmall(null));

		var cvVolume = new MatrixMap3Impl<UtilizationClass, VolumeVariable, LayerType, Float>(
				ucs, vvs, lts, (x, y, z) -> 1.0f
		);
		var cvBasalArea = new MatrixMap2Impl<UtilizationClass, LayerType, Float>(ucs, lts, (x, y) -> 1.0f);
		var cvQuadraticMeanDiameter = new MatrixMap2Impl<UtilizationClass, LayerType, Float>(ucs, lts, (x, y) -> 1.0f);
		var cvPrimaryLayerSmall = new HashMap<UtilizationClassVariable, Float>();

		sp.setCompatibilityVariables(cvVolume, cvBasalArea, cvQuadraticMeanDiameter, cvPrimaryLayerSmall);
	}

	@Test
	void buildNoProperties() {
		var ex = assertThrows(IllegalStateException.class, () -> VdypSpecies.build(builder -> {
		}));
		assertThat(
				ex,
				hasProperty(
						"message",
						allOf(containsString("polygonIdentifier"), containsString("layer"), containsString("genus"))
				)
		);
	}

	@Test
	void buildForLayer() {

		var layer = VdypLayer.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.PRIMARY);
		});

		var result = VdypSpecies.build(layer, builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.genus("B", 3);
			builder.percentGenus(50f);
			builder.volumeGroup(1);
			builder.decayGroup(2);
			builder.breakageGroup(3);
			builder.addSite(ib -> {
				ib.siteCurveNumber(0);
				ib.ageTotal(42f);
				ib.yearsToBreastHeight(2f);
				ib.yearsAtBreastHeightAuto();
				ib.height(10f);
			});
		});

		assertThat(result, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(result, hasProperty("layerType", is(LayerType.PRIMARY)));
		assertThat(result, hasProperty("genus", is("B")));
		assertThat(result, hasProperty("percentGenus", is(50f)));
		assertThat(result, hasProperty("volumeGroup", is(1)));
		assertThat(result, hasProperty("decayGroup", is(2)));
		assertThat(result, hasProperty("breakageGroup", is(3)));
		assertThat(result, hasProperty("sp64DistributionSet", hasProperty("size", is(0))));

		assertThat(layer.getSpecies(), hasEntry("B", result));
	}

	@Test
	void buildAddSpeciesPercent() {
		var result = VdypSpecies.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.PRIMARY);
			builder.genus("B", 3);
			builder.percentGenus(50f);
			builder.volumeGroup(1);
			builder.decayGroup(2);
			builder.breakageGroup(3);
			builder.addSp64Distribution("B", 100f);
		});
		assertThat(
				result,
				hasProperty(
						"sp64DistributionSet",
						hasProperty(
								"sp64DistributionMap",
								hasEntry(
										is(1),
										allOf(hasProperty("genusAlias", is("B")), hasProperty("percentage", is(100f)))
								)
						)
				)
		);
	}

	@Test
	void adaptSite() {
		var toCopy = VdypSpecies.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.layerType(LayerType.PRIMARY);
			builder.genus("B", 3);
			builder.percentGenus(50f);
			builder.volumeGroup(1);
			builder.decayGroup(2);
			builder.breakageGroup(3);
			builder.addSp64Distribution("B", 100f);
			builder.addSite(ib -> {
				ib.ageTotal(42);
				ib.yearsToBreastHeight(5);
				ib.yearsAtBreastHeightAuto();
				ib.siteCurveNumber(2);
				ib.siteIndex(5.5f);
			});
		});
		var result = VdypSpecies.build(builder -> {
			builder.adapt(toCopy);

			builder.volumeGroup(1);
			builder.decayGroup(2);
			builder.breakageGroup(3);

			builder.adaptSiteFrom(toCopy, (ib, siteToCopy) -> {
			});
		});

		var siteResult = result.getSite().get();

		assertThat(siteResult, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(siteResult, hasProperty("layerType", is(LayerType.PRIMARY)));
		assertThat(siteResult, hasProperty("siteGenus", is("B")));
		assertThat(siteResult, hasProperty("yearsToBreastHeight", present(is(5f))));
		assertThat(siteResult, hasProperty("ageTotal", present(is(42f))));
		assertThat(siteResult, hasProperty("siteCurveNumber", present(is(2))));
		assertThat(siteResult, hasProperty("siteIndex", present(is(5.5f))));
	}

	@Test
	void testSpeciesBuilder() {
		var species = new VdypSpecies.Builder().isPrimary(true) //
				.polygonIdentifier("01002 S000002 00     1970") //
				.layerType(LayerType.PRIMARY) //
				.genus("P", 1) //
				.build();

		assertThat(species.getIsPrimary(), is(true));
		species.setIsPrimary(false);
		assertThat(species.getIsPrimary(), is(false));
	}
}
