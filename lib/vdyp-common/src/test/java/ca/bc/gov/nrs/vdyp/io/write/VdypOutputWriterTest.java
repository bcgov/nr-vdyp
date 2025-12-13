package ca.bc.gov.nrs.vdyp.io.write;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.TestUtils.MockOutputStream;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class VdypOutputWriterTest {

	@TempDir
	Path tempDir;

	MockOutputStream polyStream;
	MockOutputStream specStream;
	MockOutputStream utilStream;
	MockOutputStream cvarsStream;

	MockFileResolver fileResolver;

	Map<String, Object> controlMap;

	@BeforeEach
	void initStreams() {
		controlMap = new HashMap<String, Object>();

		TestUtils.populateControlMapBecReal(controlMap);
		TestUtils.populateControlMapGenusReal(controlMap);

		polyStream = new TestUtils.MockOutputStream("polygons");
		specStream = new TestUtils.MockOutputStream("species");
		utilStream = new TestUtils.MockOutputStream("utilization");
		cvarsStream = new TestUtils.MockOutputStream("cvars");

		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name(), "testPolygonFile");
		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES.name(), "testSpeciesFile");
		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), "testUtilizationFile");
		controlMap.put(ControlKey.VDYP_OUTPUT_COMPATIBILITY_VARIABLES.name(), "testCompatibilityVariables");

		fileResolver = new MockFileResolver("TEST");
		fileResolver.addStream("testPolygonFile", polyStream);
		fileResolver.addStream("testSpeciesFile", specStream);
		fileResolver.addStream("testUtilizationFile", utilStream);
		fileResolver.addStream("testCompatibilityVariables", cvarsStream);
	}

	@Test
	void testClosesGivenStreams() throws IOException {

		var unit = new VdypOutputWriter(controlMap, polyStream, specStream, utilStream, Optional.of(cvarsStream));

		unit.close();

		polyStream.assertClosed();
		specStream.assertClosed();
		utilStream.assertClosed();
		cvarsStream.assertClosed();

		polyStream.assertContent(emptyString());
		specStream.assertContent(emptyString());
		utilStream.assertContent(emptyString());
		cvarsStream.assertContent(emptyString());
	}

	@Test
	void testClosesOpenedStreams() throws IOException {

		var unit = new VdypOutputWriter(controlMap, fileResolver);

		unit.close();

		polyStream.assertClosed();
		specStream.assertClosed();
		utilStream.assertClosed();
		cvarsStream.assertClosed();

		polyStream.assertContent(emptyString());
		specStream.assertContent(emptyString());
		utilStream.assertContent(emptyString());
		cvarsStream.assertContent(emptyString());
	}

	@Test
	void testCreateFromControlMap() throws IOException {

		var fileResolver = new FileSystemFileResolver(tempDir);

		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name(), fileResolver.toPath("testPolygonFile"));
		controlMap.put(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES.name(), fileResolver.toPath("testSpeciesFile"));
		controlMap.put(
				ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), fileResolver.toPath("testUtilizationFile")
		);
		controlMap.put(
				ControlKey.VDYP_OUTPUT_COMPATIBILITY_VARIABLES.name(), fileResolver.toPath("testCompatibilityVariables")
		);

		try (var unit = new VdypOutputWriter(controlMap)) {
			var polygon = buildTestPolygonAndChildren();

			unit.writePolygonWithSpeciesAndUtilization(polygon);
		}

		try (var polygonReader = Files.newBufferedReader(fileResolver.toPath("testPolygonFile"))) {
			assertTrue(polygonReader.read() >= 0);
		}

		try (var speciesReader = Files.newBufferedReader(fileResolver.toPath("testSpeciesFile"))) {
			assertTrue(speciesReader.read() >= 0);
		}

		try (var utilizationReader = Files.newBufferedReader(fileResolver.toPath("testUtilizationFile"))) {
			assertTrue(utilizationReader.read() >= 0);
		}

		try (var cVarsReader = Files.newBufferedReader(fileResolver.toPath("testCompatibilityVariables"))) {
			assertTrue(cVarsReader.read() == -1);
		}
	}

	@Test
	void testWritePolygon() throws IOException {
		try (var unit = new VdypOutputWriter(controlMap, fileResolver);) {

			VdypPolygon polygon = VdypPolygon.build(pb -> {

				pb.polygonIdentifier("082E004    615       1988");
				pb.percentAvailable(90f);
				pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				pb.forestInventoryZone("D");
				pb.mode(PolygonMode.START);
			});
			var layer = VdypLayer.build(polygon, lb -> {
				lb.polygonIdentifier("082E004    615       1988");
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(100);
					sb.volumeGroup(1);
					sb.decayGroup(2);
					sb.breakageGroup(3);

					sb.addSite(ib -> {
						ib.height(15f);
						ib.siteIndex(14.7f);
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.yearsAtBreastHeightAuto();
						ib.siteCurveNumber(0);
					});
				});
			});

			// FIXME Add to builder
			layer.setEmpiricalRelationshipParameterIndex(Optional.of(119));
			layer.setInventoryTypeGroup(Optional.of(28));

			unit.writePolygon(polygon);
		}

		polyStream.assertContent(is("082E004    615       1988 IDF  D    90 28119  1\n"));
		specStream.assertContent(emptyString());
		utilStream.assertContent(emptyString());
	}

	@Test
	void testWriteSpecies() throws IOException {
		try (var unit = new VdypOutputWriter(controlMap, fileResolver);) {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("082E004    615       1988");
				lb.layerType(LayerType.PRIMARY);

				lb.primaryGenus("PL");

				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(100);
					sb.volumeGroup(0);
					sb.decayGroup(0);
					sb.breakageGroup(0);
					sb.addSp64Distribution("PL", 100);

					sb.addSite(ib -> {
						ib.height(15f);
						ib.siteIndex(14.7f);
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.yearsAtBreastHeightAuto();
						ib.siteCurveNumber(0);
					});
				});
			});

			unit.writeSpecies(layer, layer.getSpecies().get("PL"));
		}
		specStream.assertContent(
				is(
						"082E004    615       1988 P 12 PL PL 100.0     0.0     0.0     0.0 14.70 15.00  60.0  51.5   8.5 1  0\n"
				)
		);
		polyStream.assertContent(emptyString());
		utilStream.assertContent(emptyString());
	}

	@Test
	void testWriteUtilizationForLayer() throws IOException {
		try (var unit = new VdypOutputWriter(controlMap, fileResolver);) {

			var polygon = VdypPolygon.build(builder -> {
				builder.polygonIdentifier("082E004    615       1988");

				builder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				builder.forestInventoryZone("D");

				builder.percentAvailable(100f);
			});
			var layer = VdypLayer.build(polygon, lb -> {
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(100);
					sb.volumeGroup(1);
					sb.decayGroup(2);
					sb.breakageGroup(3);

					sb.addSite(ib -> {
						ib.height(15f);
						ib.siteIndex(14.7f);
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.yearsAtBreastHeightAuto();
						ib.siteCurveNumber(0);
					});
				});

			});

			@SuppressWarnings("unused")
			var species = VdypSpecies.build(layer, builder -> {
				builder.genus("PL", controlMap);
				builder.addSp64Distribution("PL", 100f);

				builder.percentGenus(100f);
				builder.volumeGroup(0);
				builder.decayGroup(0);
				builder.breakageGroup(0);
			});

			layer.setBaseAreaByUtilization(
					Utils.utilizationVector(0.02865f, 19.97867f, 6.79731f, 8.54690f, 3.63577f, 0.99869f)
			);
			layer.setTreesPerHectareByUtilization(
					Utils.utilizationVector(9.29f, 1485.82f, 834.25f, 509.09f, 123.56f, 18.92f)
			);
			layer.setLoreyHeightByUtilization(Utils.heightVector(7.8377f, 13.0660f));

			layer.setWholeStemVolumeByUtilization(
					Utils.utilizationVector(0.1077f, 117.9938f, 33.3680f, 52.4308f, 25.2296f, 6.9654f)
			);
			layer.setCloseUtilizationVolumeByUtilization(
					Utils.utilizationVector(0f, 67.7539f, 2.4174f, 36.8751f, 22.0156f, 6.4459f)
			);
			layer.setCloseUtilizationVolumeNetOfDecayByUtilization(
					Utils.utilizationVector(0f, 67.0665f, 2.3990f, 36.5664f, 21.7930f, 6.3080f)
			);
			layer.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					Utils.utilizationVector(0f, 66.8413f, 2.3951f, 36.4803f, 21.7218f, 6.2442f)
			);
			layer.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(0f, 65.4214f, 2.3464f, 35.7128f, 21.2592f, 6.1030f)
			);

			// Should be ignored and computed from BA and TPH
			layer.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(4f, 4f, 4f, 4f, 4f, 4f));

			unit.writeUtilization(polygon, layer, layer);
		}
		utilStream.assertContent(
				VdypMatchers.hasLines(
						"082E004    615       1988 P  0    -1  0.02865     9.29   7.8377   0.1077   0.0000   0.0000   0.0000   0.0000   6.3", //
						"082E004    615       1988 P  0     0 19.97867  1485.82  13.0660 117.9938  67.7539  67.0665  66.8413  65.4214  13.1", //
						"082E004    615       1988 P  0     1  6.79731   834.25  -9.0000  33.3680   2.4174   2.3990   2.3951   2.3464  10.2", //
						"082E004    615       1988 P  0     2  8.54690   509.09  -9.0000  52.4308  36.8751  36.5664  36.4803  35.7128  14.6", //
						"082E004    615       1988 P  0     3  3.63577   123.56  -9.0000  25.2296  22.0156  21.7930  21.7218  21.2592  19.4", //
						"082E004    615       1988 P  0     4  0.99869    18.92  -9.0000   6.9654   6.4459   6.3080   6.2442   6.1030  25.9" //
				)
		);
		polyStream.assertContent(emptyString());
		specStream.assertContent(emptyString());
	}

	@Test
	void testWriteUtilizationZeroBaseArea() throws IOException {
		try (var unit = new VdypOutputWriter(controlMap, fileResolver);) {

			var polygon = VdypPolygon.build(builder -> {
				builder.polygonIdentifier("082E004    615       1988");

				builder.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
				builder.forestInventoryZone("D");

				builder.percentAvailable(100f);
			});
			var layer = VdypLayer.build(polygon, lb -> {
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.genus("PL", controlMap);
					sb.percentGenus(100);
					sb.volumeGroup(1);
					sb.decayGroup(2);
					sb.breakageGroup(3);

					sb.addSite(ib -> {
						ib.height(15f);
						ib.siteIndex(14.7f);
						ib.ageTotal(60f);
						ib.yearsToBreastHeight(8.5f);
						ib.yearsAtBreastHeightAuto();
						ib.siteCurveNumber(0);
					});
				});
			});

			var species = VdypSpecies.build(layer, builder -> {
				builder.genus("PL", controlMap);
				builder.addSp64Distribution("PL", 100f);

				builder.percentGenus(100f);
				builder.volumeGroup(0);
				builder.decayGroup(0);
				builder.breakageGroup(0);
			});

			species.setBaseAreaByUtilization(
					Utils.utilizationVector(0.02865f, 19.97867f, 6.79731f, 8.54690f, 3.63577f, 0f)
			);
			species.setTreesPerHectareByUtilization(
					Utils.utilizationVector(9.29f, 1485.82f, 834.25f, 509.09f, 123.56f, 18.92f)
			);
			species.setLoreyHeightByUtilization(Utils.heightVector(7.8377f, 13.0660f));

			species.setWholeStemVolumeByUtilization(
					Utils.utilizationVector(0.1077f, 117.9938f, 33.3680f, 52.4308f, 25.2296f, 6.9654f)
			);
			species.setCloseUtilizationVolumeByUtilization(
					Utils.utilizationVector(0f, 67.7539f, 2.4174f, 36.8751f, 22.0156f, 6.4459f)
			);
			species.setCloseUtilizationVolumeNetOfDecayByUtilization(
					Utils.utilizationVector(0f, 67.0665f, 2.3990f, 36.5664f, 21.7930f, 6.3080f)
			);
			species.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
					Utils.utilizationVector(0f, 66.8413f, 2.3951f, 36.4803f, 21.7218f, 6.2442f)
			);
			species.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
					Utils.utilizationVector(0f, 65.4214f, 2.3464f, 35.7128f, 21.2592f, 6.1030f)
			);

			// Should be ignored and computed from BA and TPH
			species.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(4f, 4f, 4f, 4f, 4f, 4f));

			unit.writeUtilization(polygon, layer, species);
		}
		utilStream.assertContent(
				VdypMatchers.hasLines(
						"082E004    615       1988 P 12 PL -1  0.02865     9.29   7.8377   0.1077   0.0000   0.0000   0.0000   0.0000   6.3", //
						"082E004    615       1988 P 12 PL  0 19.97867  1485.82  13.0660 117.9938  67.7539  67.0665  66.8413  65.4214  13.1", //
						"082E004    615       1988 P 12 PL  1  6.79731   834.25  -9.0000  33.3680   2.4174   2.3990   2.3951   2.3464  10.2", //
						"082E004    615       1988 P 12 PL  2  8.54690   509.09  -9.0000  52.4308  36.8751  36.5664  36.4803  35.7128  14.6", //
						"082E004    615       1988 P 12 PL  3  3.63577   123.56  -9.0000  25.2296  22.0156  21.7930  21.7218  21.2592  19.4", //
						"082E004    615       1988 P 12 PL  4  0.00000    18.92  -9.0000   6.9654   6.4459   6.3080   6.2442   6.1030  -9.0" //
						/* DQ should be -9 */
				)
		);
		polyStream.assertContent(emptyString());
		specStream.assertContent(emptyString());
	}

	@Test
	void testWritePolygonWithChildren() throws IOException {
		try (var unit = new VdypOutputWriter(controlMap, fileResolver)) {

			VdypPolygon polygon = buildTestPolygonAndChildren();
			unit.writePolygonWithSpeciesAndUtilization(polygon);
		}

		polyStream.assertContent(is("082E004    615       1988 IDF  D   100 28119  1\n"));
		utilStream.assertContent(
				VdypMatchers.hasLines(
						"082E004    615       1988 P  0    -1  0.02865     9.29   7.8377   0.1077   0.0000   0.0000   0.0000   0.0000   6.3", //
						"082E004    615       1988 P  0     0 19.97867  1485.82  13.0660 117.9938  67.7539  67.0665  66.8413  65.4214  13.1", //
						"082E004    615       1988 P  0     1  6.79731   834.25  -9.0000  33.3680   2.4174   2.3990   2.3951   2.3464  10.2", //
						"082E004    615       1988 P  0     2  8.54690   509.09  -9.0000  52.4308  36.8751  36.5664  36.4803  35.7128  14.6", //
						"082E004    615       1988 P  0     3  3.63577   123.56  -9.0000  25.2296  22.0156  21.7930  21.7218  21.2592  19.4", //
						"082E004    615       1988 P  0     4  0.99869    18.92  -9.0000   6.9654   6.4459   6.3080   6.2442   6.1030  25.9", //
						"082E004    615       1988 P 12 PL -1  0.02865     9.29   7.8377   0.1077   0.0000   0.0000   0.0000   0.0000   6.3", //
						"082E004    615       1988 P 12 PL  0 19.97867  1485.82  13.0660 117.9938  67.7539  67.0665  66.8413  65.4214  13.1", //
						"082E004    615       1988 P 12 PL  1  6.79731   834.25  -9.0000  33.3680   2.4174   2.3990   2.3951   2.3464  10.2", //
						"082E004    615       1988 P 12 PL  2  8.54690   509.09  -9.0000  52.4308  36.8751  36.5664  36.4803  35.7128  14.6", //
						"082E004    615       1988 P 12 PL  3  3.63577   123.56  -9.0000  25.2296  22.0156  21.7930  21.7218  21.2592  19.4", //
						"082E004    615       1988 P 12 PL  4  0.00000    18.92  -9.0000   6.9654   6.4459   6.3080   6.2442   6.1030  -9.0", //
						"082E004    615       1988  "
				)
		);
		specStream.assertContent(
				VdypMatchers.hasLines(
						"082E004    615       1988 P 12 PL PL 100.0     0.0     0.0     0.0 14.70 15.00  60.0  51.5   8.5 1  0", //
						"082E004    615       1988  "
				)
		);
	}

	@Test
	void testWritePolygonWithChildrenForYear() throws IOException {
		TestUtils.populateControlMapEquationGroups(controlMap, (s, b) -> new int[] { 1, 1, 1 });
		TestUtils
				.populateControlMapNetBreakage(controlMap, bgrp -> new Coefficients(new float[] { 0f, 0f, 0f, 0f }, 1));

		try (var unit = new VdypOutputWriter(controlMap, fileResolver)) {

			VdypPolygon polygon = buildTestPolygonAndChildren();
			unit.writePolygonWithSpeciesAndUtilizationForYear(polygon, 1989);
		}

		polyStream.assertContent(is("082E004    615       1989 IDF  D   100 28119  1\n"));
		utilStream.assertContent(
				VdypMatchers.hasLines(
						"082E004    615       1989 P  0    -1  0.02865     9.29   7.8377   0.1077   0.0000   0.0000   0.0000   0.0000   6.3",
						"082E004    615       1989 P  0     0 19.97867  1485.82  13.0660 117.9938  67.7539  67.0665  66.8413      NaN  13.1",
						"082E004    615       1989 P  0     1  6.79731   834.25  -9.0000  33.3680   2.4174   2.3990   2.3951   2.3951  10.2",
						"082E004    615       1989 P  0     2  8.54690   509.09  -9.0000  52.4308  36.8751  36.5664  36.4803  36.4803  14.6",
						"082E004    615       1989 P  0     3  3.63577   123.56  -9.0000  25.2296  22.0156  21.7930  21.7218  21.7218  19.4",
						"082E004    615       1989 P  0     4  0.99869    18.92  -9.0000   6.9654   6.4459   6.3080   6.2442      NaN  25.9",
						"082E004    615       1989 P 12 PL -1  0.02865     9.29   7.8377   0.1077   0.0000   0.0000   0.0000   0.0000   6.3",
						"082E004    615       1989 P 12 PL  0 19.97867  1485.82  13.0660 117.9938  67.7539  67.0665  66.8413      NaN  13.1",
						"082E004    615       1989 P 12 PL  1  6.79731   834.25  -9.0000  33.3680   2.4174   2.3990   2.3951   2.3951  10.2",
						"082E004    615       1989 P 12 PL  2  8.54690   509.09  -9.0000  52.4308  36.8751  36.5664  36.4803  36.4803  14.6",
						"082E004    615       1989 P 12 PL  3  3.63577   123.56  -9.0000  25.2296  22.0156  21.7930  21.7218  21.7218  19.4",
						"082E004    615       1989 P 12 PL  4  0.00000    18.92  -9.0000   6.9654   6.4459   6.3080   6.2442      NaN  -9.0",
						"082E004    615       1989  "
				)
		);
		specStream.assertContent(
				VdypMatchers.hasLines(
						"082E004    615       1989 P 12 PL PL 100.0     0.0     0.0     0.0 14.70 15.00  60.0  51.5   8.5 1  0", //
						"082E004    615       1989  "
				)
		);
	}

	private VdypPolygon buildTestPolygonAndChildren() {

		VdypPolygon polygon = VdypPolygon.build(pb -> {

			pb.polygonIdentifier("082E004    615       1988");
			pb.percentAvailable(100f);
			pb.biogeoclimaticZone(Utils.getBec("IDF", controlMap));
			pb.forestInventoryZone("D");
			pb.mode(PolygonMode.START);

		});

		var layer = VdypLayer.build(polygon, lb -> {
			lb.layerType(LayerType.PRIMARY);

			lb.primaryGenus("PL");

			lb.addSpecies(sb -> {
				sb.genus("PL", controlMap);
				sb.percentGenus(100);
				sb.volumeGroup(0);
				sb.decayGroup(0);
				sb.breakageGroup(0);
				sb.addSp64Distribution("PL", 100);

				sb.addSite(ib -> {
					ib.height(15f);
					ib.siteIndex(14.7f);
					ib.ageTotal(60f);
					ib.yearsToBreastHeight(8.5f);
					ib.yearsAtBreastHeightAuto();
					ib.siteCurveNumber(0);
				});
			});
		});

		var species = layer.getSpecies().get("PL");

		// fixme add to builder
		layer.setEmpiricalRelationshipParameterIndex(Optional.of(119));
		layer.setInventoryTypeGroup(Optional.of(28));

		layer.setBaseAreaByUtilization(
				Utils.utilizationVector(0.02865f, 19.97867f, 6.79731f, 8.54690f, 3.63577f, 0.99869f)
		);
		layer.setTreesPerHectareByUtilization(
				Utils.utilizationVector(9.29f, 1485.82f, 834.25f, 509.09f, 123.56f, 18.92f)
		);
		layer.setLoreyHeightByUtilization(Utils.heightVector(7.8377f, 13.0660f));

		layer.setWholeStemVolumeByUtilization(
				Utils.utilizationVector(0.1077f, 117.9938f, 33.3680f, 52.4308f, 25.2296f, 6.9654f)
		);
		layer.setCloseUtilizationVolumeByUtilization(
				Utils.utilizationVector(0f, 67.7539f, 2.4174f, 36.8751f, 22.0156f, 6.4459f)
		);
		layer.setCloseUtilizationVolumeNetOfDecayByUtilization(
				Utils.utilizationVector(0f, 67.0665f, 2.3990f, 36.5664f, 21.7930f, 6.3080f)
		);
		layer.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
				Utils.utilizationVector(0f, 66.8413f, 2.3951f, 36.4803f, 21.7218f, 6.2442f)
		);
		layer.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
				Utils.utilizationVector(0f, 65.4214f, 2.3464f, 35.7128f, 21.2592f, 6.1030f)
		);

		// Should be ignored and computed from BA and TPH.
		layer.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(4f, 4f, 4f, 4f, 4f, 4f));

		species.setBaseAreaByUtilization(
				Utils.utilizationVector(0.02865f, 19.97867f, 6.79731f, 8.54690f, 3.63577f, 0f)
		);
		species.setTreesPerHectareByUtilization(
				Utils.utilizationVector(9.29f, 1485.82f, 834.25f, 509.09f, 123.56f, 18.92f)
		);
		species.setLoreyHeightByUtilization(Utils.heightVector(7.8377f, 13.0660f));

		species.setWholeStemVolumeByUtilization(
				Utils.utilizationVector(0.1077f, 117.9938f, 33.3680f, 52.4308f, 25.2296f, 6.9654f)
		);
		species.setCloseUtilizationVolumeByUtilization(
				Utils.utilizationVector(0f, 67.7539f, 2.4174f, 36.8751f, 22.0156f, 6.4459f)
		);
		species.setCloseUtilizationVolumeNetOfDecayByUtilization(
				Utils.utilizationVector(0f, 67.0665f, 2.3990f, 36.5664f, 21.7930f, 6.3080f)
		);
		species.setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
				Utils.utilizationVector(0f, 66.8413f, 2.3951f, 36.4803f, 21.7218f, 6.2442f)
		);
		species.setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
				Utils.utilizationVector(0f, 65.4214f, 2.3464f, 35.7128f, 21.2592f, 6.1030f)
		);

		// Should be ignored and computed from BA and TPH
		species.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(4f, 4f, 4f, 4f, 4f, 4f));

		return polygon;
	}
}
