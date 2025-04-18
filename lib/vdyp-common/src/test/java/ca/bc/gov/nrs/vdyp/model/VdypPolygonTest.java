package ca.bc.gov.nrs.vdyp.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class VdypPolygonTest {

	@Test
	void build() throws Exception {
		Map<String, Object> controlMap = new HashMap<>();
		TestUtils.populateControlMapBecReal(controlMap);

		var poly1 = VdypPolygon.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.percentAvailable(90f);
			builder.forestInventoryZone("?");
			builder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
			builder.targetYear(Optional.of(2024));
		});
		assertThat(poly1, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(poly1, hasProperty("percentAvailable", is(90f)));
		assertThat(poly1, hasProperty("forestInventoryZone", is("?")));
		assertThat(poly1, hasProperty("biogeoclimaticZone", is(Utils.getBec("IDF", controlMap))));

		assertThat(poly1, hasProperty("layers", anEmptyMap()));

		assertThat(poly1, hasProperty("targetYear", present(is(2024))));

		var poly2 = new VdypPolygon(poly1, x -> x);
		assertThat(poly2, hasProperty("targetYear", present(is(2024))));

		assertThat(poly1.toString(), is("Test                 2024"));

		assertThrows(IllegalArgumentException.class, () -> poly1.setTargetYear(2025));
	}

	@Test
	void construct() throws Exception {
		Map<String, Object> controlMap = new HashMap<>();
		TestUtils.populateControlMapBecReal(controlMap);

		var poly1 = new VdypPolygon(
				new PolygonIdentifier("Test", 2024), 90f, "?", Utils.getBec("IDF", controlMap),
				Optional.of(PolygonMode.START), Optional.of(1)
		);

		assertThat(poly1, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(poly1, hasProperty("percentAvailable", is(90f)));
		assertThat(poly1, hasProperty("targetYear", notPresent()));
		assertThat(poly1, hasProperty("forestInventoryZone", is("?")));
		assertThat(poly1, hasProperty("biogeoclimaticZone", is(Utils.getBec("IDF", controlMap))));
		assertThat(poly1, hasProperty("mode", present(is(PolygonMode.START))));
		assertThat(poly1, hasProperty("inventoryTypeGroup", present(is(1))));
		assertThat(poly1, hasProperty("layers", anEmptyMap()));

		var poly2 = new VdypPolygon(poly1, x -> x);
		assertThat(poly2, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(poly2, hasProperty("percentAvailable", is(90f)));
		assertThat(poly2, hasProperty("targetYear", notPresent()));
		assertThat(poly2, hasProperty("forestInventoryZone", is("?")));
		assertThat(poly2, hasProperty("biogeoclimaticZone", is(Utils.getBec("IDF", controlMap))));
		assertThat(poly2, hasProperty("mode", present(is(PolygonMode.START))));
		assertThat(poly2, hasProperty("inventoryTypeGroup", present(is(1))));
		assertThat(poly2, hasProperty("layers", anEmptyMap()));

		poly1.setTargetYear(2025);
		assertThat(poly1, hasProperty("targetYear", present(is(2025))));

	}

	@Test
	void buildNoProperties() {
		var ex = assertThrows(IllegalStateException.class, () -> VdypPolygon.build(builder -> {
		}));
		assertThat(
				ex,
				hasProperty("message", allOf(containsString("polygonIdentifier"), containsString("percentAvailable")))
		);
	}

	@Test
	void buildAddLayer() {
		Map<String, Object> controlMap = new HashMap<>();
		TestUtils.populateControlMapBecReal(controlMap);

		VdypLayer mock = EasyMock.mock(VdypLayer.class);
		EasyMock.expect(mock.getLayerType()).andStubReturn(LayerType.PRIMARY);
		EasyMock.replay(mock);
		var result = VdypPolygon.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.percentAvailable(90f);

			builder.forestInventoryZone("?");
			builder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));

			builder.addLayer(mock);
		});
		assertThat(result, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(result, hasProperty("percentAvailable", is(90f)));
		assertThat(result, hasProperty("layers", hasEntry(LayerType.PRIMARY, mock)));
		assertThat(result, hasProperty("targetYear", notPresent()));
	}

	@Test
	void buildAddLayerSubBuild() {
		Map<String, Object> controlMap = new HashMap<>();
		TestUtils.populateControlMapBecReal(controlMap);

		var result = VdypPolygon.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.percentAvailable(90f);

			builder.forestInventoryZone("?");
			builder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));

			builder.addLayer(layerBuilder -> {
				layerBuilder.layerType(LayerType.PRIMARY);
			});
		});
		assertThat(result, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(result, hasProperty("percentAvailable", is(90f)));
		assertThat(result, hasProperty("layers", hasEntry(is(LayerType.PRIMARY), anything())));
		var resultLayer = result.getLayers().get(LayerType.PRIMARY);

		assertThat(resultLayer, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(resultLayer, hasProperty("layerType", is(LayerType.PRIMARY)));
	}

	@Test
	void copyWithoutLayers() {

		var controlMap = TestUtils.loadControlMap();

		var toCopy = VdypPolygon.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.percentAvailable(90f);

			builder.forestInventoryZone("Z");
			builder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));

			builder.addLayer(layerBuilder -> {
				layerBuilder.layerType(LayerType.PRIMARY);
			});
		});

		var result = VdypPolygon.build(builder -> {
			builder.copy(toCopy);
		});
		assertThat(result, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(result, hasProperty("percentAvailable", is(90f)));
		assertThat(result, hasProperty("forestInventoryZone", is("Z")));
		assertThat(result, hasProperty("biogeoclimaticZone", hasProperty("alias", is("IDF"))));
		assertThat(result, hasProperty("layers", anEmptyMap()));
	}

	@Test
	void copyWithLayers() {

		var controlMap = TestUtils.loadControlMap();

		var toCopy = VdypPolygon.build(builder -> {
			builder.polygonIdentifier("Test", 2024);
			builder.percentAvailable(90f);

			builder.forestInventoryZone("Z");
			builder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));

			builder.addLayer(layerBuilder -> {
				layerBuilder.layerType(LayerType.PRIMARY);
			});
		});

		var result = VdypPolygon.build(builder -> {
			builder.copy(toCopy);
			builder.copyLayers(toCopy, (layerBuilder, layer) -> {
				// Do nothing
			});
		});
		assertThat(result, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(result, hasProperty("percentAvailable", is(90f)));
		assertThat(result, hasProperty("forestInventoryZone", is("Z")));
		assertThat(result, hasProperty("biogeoclimaticZone", hasProperty("alias", is("IDF"))));
		assertThat(result, hasProperty("layers", hasEntry(is(LayerType.PRIMARY), anything())));
		var resultLayer = result.getLayers().get(LayerType.PRIMARY);

		assertThat(resultLayer, hasProperty("polygonIdentifier", isPolyId("Test", 2024)));
		assertThat(resultLayer, hasProperty("layerType", is(LayerType.PRIMARY)));
	}

}
