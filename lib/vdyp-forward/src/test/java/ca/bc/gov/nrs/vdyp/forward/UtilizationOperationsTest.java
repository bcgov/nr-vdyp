package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSite;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class UtilizationOperationsTest {

	@Test
	void testUtilizationOperations() throws Exception {

		var polyId = "TestPolygon1         2024";
		var species1Id = "D";
		var species2Id = "B";
		var forestInventoryZone = "A";

		var coveringPercentage = 80.0f;
		var scalingFactor = 100.0f / coveringPercentage;

		var polygon = VdypPolygon.build(pb -> {
			pb.polygonIdentifier(polyId);
			pb.biogeoclimaticZone(new BecDefinition("CWH", Region.COASTAL, "Coastal Western Hemlock"));
			pb.forestInventoryZone(forestInventoryZone);
			pb.inventoryTypeGroup(37);
			pb.mode(PolygonMode.START);
			pb.percentAvailable(coveringPercentage);

			pb.addLayer(VdypLayer.build(lb -> {
				lb.polygonIdentifier(polyId);
				lb.layerType(LayerType.PRIMARY);
				lb.inventoryTypeGroup(1);

				lb.baseArea(0.01513f, 0.53100f, 1.27855f, 2.33020f, 40.79285f);
				lb.treesPerHectare(5.24f, 64.82f, 71.93f, 73.60f, 384.98f);
				lb.loreyHeight(7.0166f, 30.9724f);
				lb.wholeStemVolume(0.0630f, 2.5979f, 9.1057f, 22.4019f, 586.8720f);
				lb.quadMeanDiameter(6.1f, 31.0f, 10.2f, 15.0f, 20.1f, 36.7f);
				lb.closeUtilizationVolumeByUtilization(0.0630f, 2.5979f, 9.1057f, 22.4019f, 586.8720f);
				lb.closeUtilizationVolumeNetOfDecayByUtilization(0.0000f, 0.3794f, 6.8469f, 19.8884f, 553.0534f);
				lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
						0.0000f, 0.3788f, 6.8324f, 19.8375f, 550.5741f
				);
				lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
						0.0000f, 0.3623f, 6.5384f, 18.9555f, 523.1597f
				);

				lb.addSpecies(VdypSpecies.build(sb -> {
					sb.polygonIdentifier(polyId);
					sb.layerType(LayerType.PRIMARY);
					sb.percentGenus(100.0f);
					sb.genus(species1Id);
					sb.genus(5);
					sb.baseArea(0.00155f, 0.01412f, 0.05128f, 0.45736f, 28.77972f);
					sb.treesPerHectare(0.47f, 1.64f, 2.69f, 13.82f, 269.56f);
					sb.loreyHeight(10.6033f, 33.7440f);
					sb.wholeStemVolume(0.0078f, 0.1091f, 0.5602f, 6.0129f, 452.8412f);
					sb.quadMeanDiameter(6.5f, 36.0f, 10.5f, 15.6f, 20.5f, 36.9f);
					sb.closeUtilizationVolumeByUtilization(0.0078f, 0.1091f, 0.5602f, 6.0129f, 452.8412f);
					sb.closeUtilizationVolumeNetOfDecayByUtilization(0.0000f, 0.0571f, 0.5048f, 5.6414f, 437.8810f);
					sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(
							0.0000f, 0.0566f, 0.5007f, 5.5975f, 430.3732f
					);
					sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
							0.0000f, 0.0565f, 0.5005f, 5.5948f, 429.1300f
					);

					sb.addSite(VdypSite.build(tb -> {
						tb.siteIndex(35.0f);
						tb.height(35.3f);
						tb.ageTotal(55.0f);
						tb.yearsToBreastHeight(1.0f);
						tb.siteCurveNumber(13);
						tb.layerType(LayerType.PRIMARY);
						tb.polygonIdentifier(polyId);
						tb.genus(species1Id);
					}));
				}));

				lb.addSpecies(VdypSpecies.build(sb -> {
					sb.polygonIdentifier(polyId);
					sb.layerType(LayerType.PRIMARY);
					sb.percentGenus(100.0f);
					sb.genus(species2Id);
					sb.genus(3);
					sb.baseArea(
							0.00000f, 0.00502f, 0.01063f /* <-- too small; this will be replaced by the minimum */,
							0.02284f, 0.36143f
					);
					sb.treesPerHectare(0.00f, 0.76f, 0.93f, 0.88f, 2.60f);
					sb.wholeStemVolume(0.0001f /* <-- to be cleared */, 0.0185f, 0.0757f, 0.1748f, 5.9408f);

					// These aren't to be cleared.
					sb.loreyHeight(8.0272f, 36.7553f);
					sb.quadMeanDiameter(
							6.1f, 31.5f, 7.4999f /* <-- too small; this will be replaced by the minimum */, 13.7f,
							18.2f, 42.1f
					);
				}));
			}));
		});

		var layer = polygon.getLayers().get(LayerType.PRIMARY);

		VdypLayer originalLayer = VdypLayer.build(lb -> lb.copy(layer));

		var species1 = layer.getSpeciesBySp0(species1Id);

		VdypSpecies originalSpecies1 = VdypSpecies.build(sb -> sb.copy(species1));

		var species2 = layer.getSpeciesBySp0(species2Id);

		UtilizationOperations.doPostCreateAdjustments(polygon);

		// Scaling
		testScaling(layer, originalLayer, scalingFactor);
		testScaling(species1, originalSpecies1, scalingFactor);

		// (Partial) reset on missing basal area values
		assertThat(species2.getWholeStemVolumeByUtilization().get(UtilizationClass.SMALL), is(0.0f));
		assertThat(species2.getLoreyHeightByUtilization().get(UtilizationClass.SMALL), is(8.0272f));
		assertThat(species2.getQuadraticMeanDiameterByUtilization().get(UtilizationClass.SMALL), is(6.1f));

		// Adjust Basal Area to match trees per hectare
		assertThat(species2.getBaseAreaByUtilization().get(UtilizationClass.U125TO175), is(0.014277437f));
	}

	private void testScaling(VdypUtilizationHolder post, VdypUtilizationHolder pre, float scalingFactor) {
		testScalingWasPerformed(post.getBaseAreaByUtilization(), pre.getBaseAreaByUtilization(), scalingFactor);
		testScalingWasPerformed(
				post.getCloseUtilizationVolumeByUtilization(), pre.getCloseUtilizationVolumeByUtilization(),
				scalingFactor
		);
		testScalingWasPerformed(
				post.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(),
				pre.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(), scalingFactor
		);
		testScalingWasPerformed(
				post.getCloseUtilizationVolumeNetOfDecayByUtilization(),
				pre.getCloseUtilizationVolumeNetOfDecayByUtilization(), scalingFactor
		);
		testScalingWasPerformed(
				post.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(),
				pre.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(), scalingFactor
		);
		testScalingWasNotPerformed(
				post.getLoreyHeightByUtilization(), pre.getLoreyHeightByUtilization(), scalingFactor
		);
		testScalingWasNotPerformed(
				post.getQuadraticMeanDiameterByUtilization(), pre.getQuadraticMeanDiameterByUtilization(), scalingFactor
		);
		testScalingWasPerformed(
				post.getTreesPerHectareByUtilization(), pre.getTreesPerHectareByUtilization(), scalingFactor
		);
		testScalingWasPerformed(
				post.getWholeStemVolumeByUtilization(), pre.getWholeStemVolumeByUtilization(), scalingFactor
		);
	}

	private void testScalingWasPerformed(UtilizationVector post, UtilizationVector pre, float scalingFactor) {
		for (int i = UtilizationClass.SMALL.index, j = 0; j < post.size(); i++, j++) {
			UtilizationClass uc = UtilizationClass.getByIndex(i);
			assertThat(post.get(uc), is(pre.get(uc) * scalingFactor));
		}
	}

	private void testScalingWasNotPerformed(UtilizationVector post, UtilizationVector pre, float scalingFactor) {
		for (int i = UtilizationClass.SMALL.index, j = 0; j < post.size(); i++, j++) {
			UtilizationClass uc = UtilizationClass.getByIndex(i);
			assertThat(post.get(uc), not(pre.get(uc) * scalingFactor));
		}
	}

	Map<String, Object> parse(ForwardControlParser parser, String resourceName)
			throws IOException, ResourceParseException {

		Class<?> klazz = TestUtils.class;
		try (var is = klazz.getResourceAsStream(resourceName)) {

			return parser.parse(is, TestUtils.fileResolver(klazz), new HashMap<>());
		}
	}
}
