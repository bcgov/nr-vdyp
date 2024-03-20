package ca.bc.gov.nrs.vdyp.vri;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.StandProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.vri.model.VriLayer;
import ca.bc.gov.nrs.vdyp.vri.model.VriPolygon;
import ca.bc.gov.nrs.vdyp.vri.model.VriSite;

class VriInputValidationTest {

	@Test
	void testPassValid() throws Exception {
		var app = new VriStart();

		var controlMap = new HashMap<String, Object>();

		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_POLYGON.name(), "DUMMY1");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SPECIES.name(), "DUMMY2");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), "DUMMY3");

		final var polygonId = "Test";
		final var layerType = LayerType.PRIMARY;

		MockFileResolver resolver = new MockFileResolver("Test");
		resolver.addStream("DUMMY1", (OutputStream) new ByteArrayOutputStream());
		resolver.addStream("DUMMY2", (OutputStream) new ByteArrayOutputStream());
		resolver.addStream("DUMMY3", (OutputStream) new ByteArrayOutputStream());

		controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
			map.put(VriControlParser.MINIMUM_BASE_AREA, 0f);
			map.put(VriControlParser.MINIMUM_HEIGHT, 6f);
			map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
		}));

		app.init(resolver, controlMap);

		var poly = VriPolygon.build(pBuilder -> {
			pBuilder.polygonIdentifier("082F074/0071         2001");
			pBuilder.biogeoclimaticZone("IDF");
			pBuilder.forestInventoryZone(" ");
			pBuilder.yieldFactor(1.0f);
			pBuilder.buildLayer(lBuilder -> {
				lBuilder.layerType(LayerType.PRIMARY);
				((VriLayer.Builder) lBuilder).crownClosure(57.8f);
				((VriLayer.Builder) lBuilder).baseArea(66.0f);
				((VriLayer.Builder) lBuilder).treesPerHectare(850f);
				((VriLayer.Builder) lBuilder).utilization(7.5f);

				// Sites
				lBuilder.addSite(iBuilder -> {
					iBuilder.siteGenus("B");
					((VriSite.Builder) iBuilder).siteSpecies("BL");
					iBuilder.siteCurveNumber(8);
				});
				lBuilder.addSite(iBuilder -> {
					iBuilder.ageTotal(200);
					iBuilder.height(28.0f);
					iBuilder.siteIndex(14.3f);
					iBuilder.siteGenus("C");
					((VriSite.Builder) iBuilder).siteSpecies("CW");
					iBuilder.yearsToBreastHeight(10.9f);
					((VriSite.Builder) iBuilder).breastHeightAge(189.1f);
					iBuilder.siteCurveNumber(11);
				});
				lBuilder.addSite(iBuilder -> {
					iBuilder.ageTotal(200);
					iBuilder.height(32.0f);
					iBuilder.siteIndex(14.6f);
					iBuilder.siteGenus("H");
					((VriSite.Builder) iBuilder).siteSpecies("HW");
					iBuilder.yearsToBreastHeight(9.7f);
					((VriSite.Builder) iBuilder).breastHeightAge(190.3f);
					iBuilder.siteCurveNumber(37);
				});
				lBuilder.addSite(iBuilder -> {
					iBuilder.siteGenus("S");
					((VriSite.Builder) iBuilder).siteSpecies("SE");
					iBuilder.siteCurveNumber(71);
				});

				// Species
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("B");
					sBuilder.percentGenus(3f);
					sBuilder.addSpecies("BL", 100);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("C");
					sBuilder.percentGenus(30f);
					sBuilder.addSpecies("CW", 100);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("H");
					sBuilder.percentGenus(48.9f);
					sBuilder.addSpecies("HW", 100);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("S");
					sBuilder.percentGenus(18.1f);
					sBuilder.addSpecies("SE", 100);
				});
			});
			pBuilder.buildLayer(lBuilder -> {
				lBuilder.layerType(LayerType.VETERAN);
				((VriLayer.Builder) lBuilder).crownClosure(0f);
				((VriLayer.Builder) lBuilder).baseArea(0f);
				((VriLayer.Builder) lBuilder).treesPerHectare(0f);
				((VriLayer.Builder) lBuilder).utilization(0f);
			});
		});

		app.checkPolygon(poly);
	}

	@Test
	void testFailPrimarySpeciesDontSumTo100() throws Exception {
		var app = new VriStart();

		var controlMap = new HashMap<String, Object>();

		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_POLYGON.name(), "DUMMY1");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SPECIES.name(), "DUMMY2");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), "DUMMY3");

		MockFileResolver resolver = new MockFileResolver("Test");
		resolver.addStream("DUMMY1", (OutputStream) new ByteArrayOutputStream());
		resolver.addStream("DUMMY2", (OutputStream) new ByteArrayOutputStream());
		resolver.addStream("DUMMY3", (OutputStream) new ByteArrayOutputStream());

		app.init(resolver, controlMap);

		var poly = VriPolygon.build(pBuilder -> {
			pBuilder.polygonIdentifier("082F074/0071         2001");
			pBuilder.biogeoclimaticZone("IDF");
			pBuilder.forestInventoryZone(" ");
			pBuilder.yieldFactor(1.0f);
			pBuilder.buildLayer(lBuilder -> {
				lBuilder.layerType(LayerType.PRIMARY);
				((VriLayer.Builder) lBuilder).crownClosure(57.8f);
				((VriLayer.Builder) lBuilder).baseArea(66.0f);
				((VriLayer.Builder) lBuilder).treesPerHectare(850f);
				((VriLayer.Builder) lBuilder).utilization(7.5f);

				// Sites
				lBuilder.addSite(iBuilder -> {
					iBuilder.siteGenus("B");
					((VriSite.Builder) iBuilder).siteSpecies("BL");
					iBuilder.siteCurveNumber(8);
				});
				lBuilder.addSite(iBuilder -> {
					iBuilder.ageTotal(200);
					iBuilder.height(28.0f);
					iBuilder.siteIndex(14.3f);
					iBuilder.siteGenus("C");
					((VriSite.Builder) iBuilder).siteSpecies("CW");
					iBuilder.yearsToBreastHeight(10.9f);
					((VriSite.Builder) iBuilder).breastHeightAge(189.1f);
					iBuilder.siteCurveNumber(11);
				});
				lBuilder.addSite(iBuilder -> {
					iBuilder.ageTotal(200);
					iBuilder.height(32.0f);
					iBuilder.siteIndex(14.6f);
					iBuilder.siteGenus("H");
					((VriSite.Builder) iBuilder).siteSpecies("HW");
					iBuilder.yearsToBreastHeight(9.7f);
					((VriSite.Builder) iBuilder).breastHeightAge(190.3f);
					iBuilder.siteCurveNumber(37);
				});
				lBuilder.addSite(iBuilder -> {
					iBuilder.siteGenus("S");
					((VriSite.Builder) iBuilder).siteSpecies("SE");
					iBuilder.siteCurveNumber(71);
				});

				// Species
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("B");
					sBuilder.percentGenus(3f);
					sBuilder.addSpecies("BL", 100);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("C");
					sBuilder.percentGenus(30f);
					sBuilder.addSpecies("CW", 100);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("H");
					sBuilder.percentGenus(48.7f);
					sBuilder.addSpecies("HW", 100);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("S");
					sBuilder.percentGenus(18.1f);
					sBuilder.addSpecies("SE", 100);
				});
			});
			pBuilder.buildLayer(lBuilder -> {
				lBuilder.layerType(LayerType.VETERAN);
				((VriLayer.Builder) lBuilder).crownClosure(0f);
				((VriLayer.Builder) lBuilder).baseArea(0f);
				((VriLayer.Builder) lBuilder).treesPerHectare(0f);
				((VriLayer.Builder) lBuilder).utilization(0f);
			});
		});

		assertThrows(StandProcessingException.class, () -> app.checkPolygon(poly));
	}

	@Test
	void testFailIfMissingPrimary() throws Exception {
		var app = new VriStart();

		var controlMap = new HashMap<String, Object>();

		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_POLYGON.name(), "DUMMY1");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SPECIES.name(), "DUMMY2");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), "DUMMY3");

		final var polygonId = "Test";
		final var layerType = LayerType.PRIMARY;

		MockFileResolver resolver = new MockFileResolver("Test");
		resolver.addStream("DUMMY1", (OutputStream) new ByteArrayOutputStream());
		resolver.addStream("DUMMY2", (OutputStream) new ByteArrayOutputStream());
		resolver.addStream("DUMMY3", (OutputStream) new ByteArrayOutputStream());

		app.init(resolver, controlMap);

		var poly = VriPolygon.build(pBuilder -> {
			pBuilder.polygonIdentifier("082F074/0071         2001");
			pBuilder.biogeoclimaticZone("IDF");
			pBuilder.forestInventoryZone(" ");
			pBuilder.yieldFactor(1.0f);
			pBuilder.buildLayer(lBuilder -> {
				lBuilder.layerType(LayerType.VETERAN);
				((VriLayer.Builder) lBuilder).crownClosure(57.8f);
				((VriLayer.Builder) lBuilder).baseArea(66.0f);
				((VriLayer.Builder) lBuilder).treesPerHectare(850f);
				((VriLayer.Builder) lBuilder).utilization(7.5f);

				// Sites
				lBuilder.addSite(iBuilder -> {
					iBuilder.siteGenus("B");
					((VriSite.Builder) iBuilder).siteSpecies("BL");
					iBuilder.siteCurveNumber(8);
				});
				lBuilder.addSite(iBuilder -> {
					iBuilder.ageTotal(200);
					iBuilder.height(28.0f);
					iBuilder.siteIndex(14.3f);
					iBuilder.siteGenus("C");
					((VriSite.Builder) iBuilder).siteSpecies("CW");
					iBuilder.yearsToBreastHeight(10.9f);
					((VriSite.Builder) iBuilder).breastHeightAge(189.1f);
					iBuilder.siteCurveNumber(11);
				});
				lBuilder.addSite(iBuilder -> {
					iBuilder.ageTotal(200);
					iBuilder.height(32.0f);
					iBuilder.siteIndex(14.6f);
					iBuilder.siteGenus("H");
					((VriSite.Builder) iBuilder).siteSpecies("HW");
					iBuilder.yearsToBreastHeight(9.7f);
					((VriSite.Builder) iBuilder).breastHeightAge(190.3f);
					iBuilder.siteCurveNumber(37);
				});
				lBuilder.addSite(iBuilder -> {
					iBuilder.siteGenus("S");
					((VriSite.Builder) iBuilder).siteSpecies("SE");
					iBuilder.siteCurveNumber(71);
				});

				// Species
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("B");
					sBuilder.percentGenus(3f);
					sBuilder.addSpecies("BL", 100);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("C");
					sBuilder.percentGenus(30f);
					sBuilder.addSpecies("CW", 100);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("H");
					sBuilder.percentGenus(48.7f);
					sBuilder.addSpecies("HW", 100);
				});
				lBuilder.addSpecies(sBuilder -> {
					sBuilder.genus("S");
					sBuilder.percentGenus(18.1f);
					sBuilder.addSpecies("SE", 100);
				});
			});
		});

		assertThrows(StandProcessingException.class, () -> app.checkPolygon(poly));
	}

	@Test
	void testFindDefaultMode() throws Exception {
		var app = new VriStart();

		var controlMap = new HashMap<String, Object>();

		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_POLYGON.name(), "DUMMY1");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SPECIES.name(), "DUMMY2");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), "DUMMY3");

		controlMap.put(ControlKey.MINIMA.name(), Utils.constMap(map -> {
			map.put(VriControlParser.MINIMUM_BASE_AREA, 0f);
			map.put(VriControlParser.MINIMUM_HEIGHT, 6f);
			map.put(VriControlParser.MINIMUM_PREDICTED_BASE_AREA, 2f);
		}));

		final var polygonId = "Test";
		final var layerType = LayerType.PRIMARY;

		MockFileResolver resolver = new MockFileResolver("Test");
		resolver.addStream("DUMMY1", (OutputStream) new ByteArrayOutputStream());
		resolver.addStream("DUMMY2", (OutputStream) new ByteArrayOutputStream());
		resolver.addStream("DUMMY3", (OutputStream) new ByteArrayOutputStream());

		app.init(resolver, controlMap);

		Optional<Float> ageTotal = Optional.of(200f);
		Optional<Float> yearsToBreastHeight = Optional.of(190f);
		Optional<Float> height = Optional.of(10f);
		Optional<Float> baseArea = Optional.of(30f);
		Optional<Float> treesPerHectare = Optional.of(300f);
		Optional<Float> percentForest = Optional.of(90f);

		app.findDefaultPolygonMode(ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest);
	}
}