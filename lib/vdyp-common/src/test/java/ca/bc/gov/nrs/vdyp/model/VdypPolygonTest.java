package ca.bc.gov.nrs.vdyp.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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

	@Test
	void dumpState() throws IOException {
		var controlMap = TestUtils.loadControlMap();
		float percentAvailable = 83f;

		final var inputPoly = VdypPolygon.build(pb -> {
			pb.polygonIdentifier("082L025       459", 1964);

			pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
			pb.forestInventoryZone(" ");
			pb.inventoryTypeGroup(7);
			pb.mode(PolygonMode.START);
			pb.percentAvailable(percentAvailable);
			pb.targetYear(2123);

			pb.addLayer(lb -> {
				lb.layerType(LayerType.PRIMARY);

				lb.empiricalRelationshipParameterIndex(60);
				lb.inventoryTypeGroup(7);
				lb.primaryGenus("F");

				lb.loreyHeightByUtilization(8.0215f, 19.6178f);

				lb.baseAreaByUtilization(0.027385544f, /* 24.936737f, */ 2.570687f, 4.922446f, 5.950603f, 11.493001f);
				lb.quadraticMeanDiameterByUtilization(
						6.037325f, 18.183805f, 10.097809f, 15.024368f, 19.883558f, 29.343294f
				);
				lb.treesPerHectareByUtilization(9.566265f, /* 960.241f, */ 321.0f, 277.6506f, 191.63857f, 169.95181f);

				lb.wholeStemVolumeByUtilization(
						0.10180723f, /* 181.18024f, */ 11.493495f, 30.97241f, 45.31157f, 93.40277f
				);
				lb.closeUtilizationVolumeByUtilization(
						0.0f, /* 146.82976f, */ 0.093855426f, 20.476025f, 39.108315f, 87.15169f
				);
				lb.closeUtilizationVolumeNetOfDecayByUtilization(
						0.0f, /* 142.9464f, */ 0.09301206f, 20.251808f, 38.45217f, 84.14952f
				);
				lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
						0.0f, /* 141.65495f, */ 0.09277109f, 20.190603f, 38.26458f, 83.10699f
				);
				lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						0.0f, /* 137.93434f, */ 0.09060241f, 19.696987f, 37.2794f, 80.867226f
				);

				lb.addSpecies(sb -> {
					sb.genus("F", controlMap);

					sb.breakageGroup(16);
					sb.decayGroup(27);
					sb.volumeGroup(33);

					sb.percentGenus(60f);

					sb.addSite(ib -> {
						ib.siteIndex(18.33f);
						ib.siteCurveNumber(96);

						ib.height(21f);

						ib.ageTotal(70f);
						ib.yearsAtBreastHeight(61.1f);
						ib.yearsToBreastHeight(9.4f);
					});

					sb.sp64DistributionList(
							List.of(new Sp64Distribution(1, "F1", 60f), new Sp64Distribution(2, "F2", 40f))
					);

					sb.loreyHeight(7.7875f, 18.0594f);

					sb.baseArea(0.021361446f, /* 14.962049f, */ 2.1693978f, 3.157458f, 3.080265f, 6.554928f);
					sb.quadMeanDiameter(5.902309f, 17.314062f, 10.060733f, 15.029455f, 20.035698f, 30.987747f);
					sb.treesPerHectare(7.807229f, /* 635.482f, */ 272.89157f, 177.9759f, 97.69879f, 86.915665f);

					sb.wholeStemVolume(0.07903615f, /* 99.580246f, */ 9.413856f, 18.730844f, 21.876747f, 49.558796f);
					sb.closeUtilizationVolumeByUtilization(
							0.0f, /* 77.24904f, */ 0.06674699f, 12.073013f, 18.811207f, 46.298073f
					);
					sb.closeUtilizationVolumeNetOfDecayByUtilization(
							0.0f, /* 76.662056f, */ 0.066385545f, 12.01494f, 18.71253f, 45.868195f
					);
					sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
							0.0f, /* 76.4747f, */ 0.06626506f, 12.000121f, 18.680603f, 45.727592f
					);
					sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
							0.0f, /* 74.87422f, */ 0.06493976f, 11.758675f, 18.304459f, 44.746147f
					);
				});

				lb.addSpecies(sb -> {
					sb.genus("L", controlMap);

					sb.breakageGroup(20);
					sb.decayGroup(38);
					sb.volumeGroup(46);

					sb.percentGenus(40f);

					sb.sp64DistributionList(
							List.of(new Sp64Distribution(1, "L1", 70f), new Sp64Distribution(2, "L2", 30f))
					);

					sb.loreyHeight(8.8518f, 21.9554f);

					sb.baseArea(0.0060240966f, /* 9.9747f, */ 0.4012892f, 1.764988f, 2.8703375f, 4.938072f);
					sb.quadMeanDiameter(6.603339f, 19.775358f, 10.304308f, 15.016189f, 19.724083f, 27.516943f);
					sb.treesPerHectare(1.7590363f, /* 324.75903f, */ 48.120483f, 99.66266f, 93.939766f, 83.03615f);

					sb.wholeStemVolume(0.022771085f, /* 81.59988f, */ 2.0796385f, 12.241567f, 23.434818f, 43.843975f);
					sb.closeUtilizationVolumeByUtilization(
							0.0f, /* 69.58085f, */ 0.027108436f, 8.403012f, 20.29711f, 40.85362f
					);
					sb.closeUtilizationVolumeNetOfDecayByUtilization(
							0.0f, /* 66.28422f, */ 0.026626507f, 8.236748f, 19.73964f, 38.281326f
					);
					sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
							0.0f, /* 65.180244f, */ 0.026506025f, 8.190482f, 19.583857f, 37.3794f
					);
					sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
							0.0f, /* 63.060005f, */ 0.025662651f, 7.9383135f, 18.974941f, 36.121086f
					);
				});
			});
		});

		var out = new StringBuilder();

		inputPoly.dumpState(out);

		// This is somewhat brittle, but it's good enough to get coverage of something that's only there for debugging.
		assertThat(
				out.toString(),
				equalTo(
						"""
								VdypPolygon (082L025       459    1964)
								  percentAvailable = 83.0
								  biogeoclimaticZone = IDF (Interior DougFir)
								  forestInventoryZone = " "
								  mode = Optional[START]
								  inventoryTypeGroup = Optional[7]
								  layers:
								    VdypLayer (082L025       459(1964)-PRIMARY)
								      inventoryTypeGroup = Optional[7]
								      layers:
								        VdypSpecies (082L025       459(1964)-PRIMARY-F)
								          genus = "F"
								          genusIndex = 7
								          percentGenus = Optional[60.0]
								          fractionGenus = Optional[0.6]
								          sp64DistributionSet = [F1[1]:60.0, F2[2]:40.0]
								          site:
								            VdypSite (082L025       459(1964)-PRIMARY-F)
								              siteGenus = "F"
								              siteCurveNumber = Optional[96]
								              siteIndex = Optional[18.33]
								              ageTotal = Optional[70.0]
								              height = Optional[21.0]
								              yearsToBreastHeight = Optional[9.4]
								              yearsAtBreastHeight = Optional[61.1]
								          volumeGroup = Optional[33]
								          decayGroup = Optional[27]
								          breakageGroup = Optional[16]
								          baseAreaByUtilization = [-1:0.021361446, 0:14.962049, 1:2.1693978, 2:3.157458, 3:3.080265, 4:6.554928]
								          loreyHeightByUtilization = [-1:7.7875, 0:18.0594]
								          quadraticMeanDiameterByUtilization = [-1:5.902309, 0:17.314062, 1:10.060733, 2:15.029455, 3:20.035698, 4:30.987747]
								          treesPerHectareByUtilization = [-1:7.807229, 0:635.48193, 1:272.89157, 2:177.9759, 3:97.69879, 4:86.915665]
								          wholeStemVolumeByUtilization = [-1:0.07903615, 0:99.580246, 1:9.413856, 2:18.730844, 3:21.876747, 4:49.558796]
								          closeUtilizationVolumeByUtilization = [-1:0.0, 0:77.24904, 1:0.06674699, 2:12.073013, 3:18.811207, 4:46.298073]
								          closeUtilizationVolumeNetOfDecayByUtilization = [-1:0.0, 0:76.66205, 1:0.066385545, 2:12.01494, 3:18.71253, 4:45.868195]
								          closeUtilizationVolumeNetOfDecayAndWasteByUtilization = [-1:0.0, 0:76.47458, 1:0.06626506, 2:12.000121, 3:18.680603, 4:45.727592]
								          closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = [-1:0.0, 0:74.87422, 1:0.06493976, 2:11.758675, 3:18.304459, 4:44.746147]
								          cvVolume = Optional.empty
								          cvBasalArea = Optional.empty
								          cvQuadraticMeanDiameter = Optional.empty
								          cvPrimaryLayerSmall = Optional.empty
								        VdypSpecies (082L025       459(1964)-PRIMARY-L)
								          genus = "L"
								          genusIndex = 9
								          percentGenus = Optional[40.0]
								          fractionGenus = Optional[0.4]
								          sp64DistributionSet = [L1[1]:70.0, L2[2]:30.0]
								          site:
								            N/A
								          volumeGroup = Optional[46]
								          decayGroup = Optional[38]
								          breakageGroup = Optional[20]
								          baseAreaByUtilization = [-1:0.0060240966, 0:9.974687, 1:0.4012892, 2:1.764988, 3:2.8703375, 4:4.938072]
								          loreyHeightByUtilization = [-1:8.8518, 0:21.9554]
								          quadraticMeanDiameterByUtilization = [-1:6.603339, 0:19.775358, 1:10.304308, 2:15.016189, 3:19.724083, 4:27.516943]
								          treesPerHectareByUtilization = [-1:1.7590363, 0:324.75903, 1:48.120483, 2:99.66266, 3:93.939766, 4:83.03615]
								          wholeStemVolumeByUtilization = [-1:0.022771085, 0:81.6, 1:2.0796385, 2:12.241567, 3:23.434818, 4:43.843975]
								          closeUtilizationVolumeByUtilization = [-1:0.0, 0:69.58085, 1:0.027108436, 2:8.403012, 3:20.29711, 4:40.85362]
								          closeUtilizationVolumeNetOfDecayByUtilization = [-1:0.0, 0:66.28434, 1:0.026626507, 2:8.236748, 3:19.73964, 4:38.281326]
								          closeUtilizationVolumeNetOfDecayAndWasteByUtilization = [-1:0.0, 0:65.180244, 1:0.026506025, 2:8.190482, 3:19.583857, 4:37.3794]
								          closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = [-1:0.0, 0:63.060005, 1:0.025662651, 2:7.9383135, 3:18.974941, 4:36.121086]
								          cvVolume = Optional.empty
								          cvBasalArea = Optional.empty
								          cvQuadraticMeanDiameter = Optional.empty
								          cvPrimaryLayerSmall = Optional.empty
								      empiricalRelationshipParameterIndex = Optional[60]
								      primarySp0 = Optional[F]
								      baseAreaByUtilization = [-1:0.027385544, 0:24.936737, 1:2.570687, 2:4.922446, 3:5.950603, 4:11.493001]
								      loreyHeightByUtilization = [-1:8.0215, 0:19.6178]
								      quadraticMeanDiameterByUtilization = [-1:6.037325, 0:18.183805, 1:10.097809, 2:15.024368, 3:19.883558, 4:29.343294]
								      treesPerHectareByUtilization = [-1:9.566265, 0:960.24097, 1:321.0, 2:277.6506, 3:191.63857, 4:169.95181]
								      wholeStemVolumeByUtilization = [-1:0.10180723, 0:181.18024, 1:11.493495, 2:30.97241, 3:45.31157, 4:93.40277]
								      closeUtilizationVolumeByUtilization = [-1:0.0, 0:146.82988, 1:0.093855426, 2:20.476025, 3:39.108315, 4:87.15169]
								      closeUtilizationVolumeNetOfDecayByUtilization = [-1:0.0, 0:142.9465, 1:0.09301206, 2:20.251808, 3:38.45217, 4:84.14952]
								      closeUtilizationVolumeNetOfDecayAndWasteByUtilization = [-1:0.0, 0:141.65494, 1:0.09277109, 2:20.190603, 3:38.26458, 4:83.10699]
								      closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = [-1:0.0, 0:137.93422, 1:0.09060241, 2:19.696987, 3:37.2794, 4:80.867226]
								  targetYear = Optional[2123]
								"""
				)
		);

	}

}
