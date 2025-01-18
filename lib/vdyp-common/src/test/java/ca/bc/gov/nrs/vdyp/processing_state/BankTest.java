package ca.bc.gov.nrs.vdyp.processing_state;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.test.ProcessingTestUtils;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class BankTest {

	private Map<String, Object> controlMap;
	private VdypPolygon polygon;

	@BeforeEach
	void before() throws IOException, ResourceParseException {

		controlMap = TestUtils.loadControlMap();

		var bec = Utils.getBec("CDF", controlMap);

		polygon = VdypPolygon.build(pb -> {
			pb.polygonIdentifier("Test", 2024);

			pb.percentAvailable(99f);
			pb.biogeoclimaticZone(bec);
			pb.forestInventoryZone("A");

			pb.addLayer(lb -> {
				lb.layerType(LayerType.PRIMARY);

				lb.addSpecies(sb -> {
					sb.controlMap(controlMap);
					sb.genus("B");
					sb.baseArea(0.4f);
					sb.percentGenus(10);
				});
				lb.addSpecies(sb -> {
					sb.controlMap(controlMap);
					sb.genus("C");
					sb.baseArea(0.6f);
					sb.percentGenus(10);
				});
				lb.addSpecies(sb -> {
					sb.controlMap(controlMap);
					sb.genus("D");
					sb.baseArea(10f);
					sb.percentGenus(10);
				});
				lb.addSpecies(sb -> {
					sb.controlMap(controlMap);
					sb.genus("H");
					sb.baseArea(50f);
					sb.percentGenus(60);
					sb.addSite(ib -> {
						ib.ageTotal(100);
						ib.yearsToBreastHeight(5);
						ib.siteIndex(0.6f);
						ib.height(20f);
						ib.siteCurveNumber(10);
					});
				});
				lb.addSpecies(sb -> {
					sb.controlMap(controlMap);
					sb.genus("S");
					sb.baseArea(99.9f);
					sb.percentGenus(10);
					sb.addSite(ib -> {
						ib.ageTotal(100);
						ib.yearsToBreastHeight(5);
						ib.siteIndex(0.6f);
						ib.height(20f);
					});

					sb.quadMeanDiameter(25);
					sb.baseArea(26);
					sb.treesPerHectare(BaseAreaTreeDensityDiameter.treesPerHectare(26, 25));
					sb.loreyHeight(227);
					sb.closeUtilizationVolumeByUtilization(42);
					sb.closeUtilizationVolumeNetOfDecayByUtilization(41);
					sb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(40);
					sb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(39);
				});

				lb.quadMeanDiameter(21);
				lb.baseArea(22);
				lb.treesPerHectare(BaseAreaTreeDensityDiameter.treesPerHectare(22, 21));
				lb.loreyHeight(24);
				lb.closeUtilizationVolumeByUtilization(42);
				lb.closeUtilizationVolumeNetOfDecayByUtilization(41);
				lb.closeUtilizationVolumeNetOfDecayAndWasteByUtilization(40);
				lb.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(39);
			});

		});

	}

	@Test
	void testConstruction() throws IOException, ResourceParseException, ProcessingException {

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

		int nSpecies = pLayer.getSpecies().size();

		assertThat(bank, notNullValue());
		assertThat(bank.yearsAtBreastHeight.length, is(nSpecies + 1));
		assertThat(bank.ageTotals.length, is(nSpecies + 1));
		assertThat(bank.dominantHeights.length, is(nSpecies + 1));
		assertThat(bank.percentagesOfForestedLand.length, is(nSpecies + 1));
		assertThat(bank.siteIndices.length, is(nSpecies + 1));
		assertThat(bank.sp64Distributions.length, is(nSpecies + 1));
		assertThat(bank.speciesIndices.length, is(nSpecies + 1));
		assertThat(bank.speciesNames.length, is(nSpecies + 1));
		assertThat(bank.yearsToBreastHeight.length, is(nSpecies + 1));
		assertThat(bank.getNSpecies(), is(nSpecies));

		assertThat(bank.basalAreas.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.basalAreas[i].length, is(UtilizationClass.values().length));
		}
		assertThat(bank.closeUtilizationVolumes.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.closeUtilizationVolumes[i].length, is(UtilizationClass.values().length));
		}
		assertThat(bank.cuVolumesMinusDecay.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.cuVolumesMinusDecay[i].length, is(UtilizationClass.values().length));
		}
		assertThat(bank.cuVolumesMinusDecayAndWastage.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.cuVolumesMinusDecayAndWastage[i].length, is(UtilizationClass.values().length));
		}

		assertThat(bank.loreyHeights.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.loreyHeights[i].length, is(2));
		}
		assertThat(bank.quadMeanDiameters.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.quadMeanDiameters[i].length, is(UtilizationClass.values().length));
		}
		assertThat(bank.treesPerHectare.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.treesPerHectare[i].length, is(UtilizationClass.values().length));
		}
		assertThat(bank.wholeStemVolumes.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.wholeStemVolumes[i].length, is(UtilizationClass.values().length));
		}
	}

	@Test
	void testSetCopy() throws IOException, ResourceParseException, ProcessingException {

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

		pLayer = ProcessingTestUtils.normalizeLayer(pLayer);
		verifyBankMatchesLayer(bank, pLayer);

		Bank ppsCopy = bank.copy();

		verifyBankMatchesLayer(ppsCopy, pLayer);
	}

	@Test
	void testRemoveSmallLayers() throws IOException, ResourceParseException, ProcessingException {

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank1 = new Bank(
				pLayer, polygon.getBiogeoclimaticZone(),
				s -> s.getBaseAreaByUtilization().get(UtilizationClass.ALL) >= 0.5
		);

		// the filter should have removed genus B (index 3) since it's ALL basal area is below 0.5
		assertThat(bank1.getNSpecies(), is(pLayer.getSpecies().size() - 1));
		assertThat(bank1.speciesIndices, is(new int[] { 0, 4, 5, 8, 15 }));

		Bank bank2 = new Bank(
				pLayer, polygon.getBiogeoclimaticZone(),
				s -> s.getBaseAreaByUtilization().get(UtilizationClass.ALL) >= 100.0
		);

		// the filter should have removed all genera.
		assertThat(bank2.getNSpecies(), is(0));
		assertThat(bank2.speciesIndices, is(new int[] { 0 }));
	}

	@Test
	void testCopyConstructor() throws IOException, ResourceParseException, ProcessingException {

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

		Bank bankCopy = new Bank(bank);

		pLayer = ProcessingTestUtils.normalizeLayer(pLayer);
		verifyBankMatchesLayer(bankCopy, pLayer);
	}

	@Test
	void testLayerUpdate() throws IOException, ResourceParseException, ProcessingException {

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

		pLayer = ProcessingTestUtils.normalizeLayer(pLayer);

		verifyBankMatchesLayer(bank, pLayer);

		UtilizationVector uv = pLayer.getBaseAreaByUtilization();
		float newValue = uv.get(UtilizationClass.ALL) + 1.0f;
		uv.set(UtilizationClass.ALL, newValue);
		pLayer.setBaseAreaByUtilization(uv);

		bank.refreshBank(pLayer);

		verifyBankMatchesLayer(bank, pLayer);
	}

	@Test
	void testBuildLayerFromBank() throws IOException, ResourceParseException, ProcessingException {

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

		pLayer = ProcessingTestUtils.normalizeLayer(pLayer);

		verifyBankMatchesLayer(bank, pLayer);

		var bankPart2 = new float[][][] { bank.loreyHeights, bank.basalAreas, bank.quadMeanDiameters,
				bank.treesPerHectare, bank.wholeStemVolumes, bank.closeUtilizationVolumes, bank.cuVolumesMinusDecay,
				bank.cuVolumesMinusDecayAndWastage };

		var bankPart1 = new float[][] { bank.ageTotals, bank.dominantHeights,
				// bank.percentagesOfForestedLand,
				bank.siteIndices, bank.yearsAtBreastHeight
				// bank.yearsToBreastHeight //
		};

		for (int i = 0; i < bankPart1.length; i++) {
			for (int j = 0; j < bankPart1[i].length; j++) {
				bankPart1[i][j] += 1;
			}
		}
		for (int i = 0; i < bankPart2.length; i++) {
			for (int j = 0; j < bankPart2[i].length; j++) {
				for (int k = 0; k < bankPart2[i][j].length; k++) {
					bankPart2[i][j][k] += 1;
				}
			}
		}

		var result = bank.buildLayerFromBank();

		verifyBankMatchesLayer(bank, result);
	}

	private void verifyBankMatchesLayer(Bank lps, VdypLayer layer) {

		List<Integer> sortedSpIndices = layer.getSpecies().values().stream().map(s -> s.getGenusIndex()).sorted()
				.toList();

		int arrayIndex = 1;
		for (int i = 0; i < sortedSpIndices.size(); i++) {
			VdypSpecies genus = layer.getSpeciesByIndex(sortedSpIndices.get(i));

			verifyBankSpeciesMatchesSpecies(lps, arrayIndex, genus);

			verifyBankUtilizationsMatchesUtilizations(lps, arrayIndex, genus);

			arrayIndex += 1;
		}

		verifyBankUtilizationsMatchesUtilizations(lps, 0, layer);
	}

	private void verifyBankUtilizationsMatchesUtilizations(Bank lps, int spIndex, VdypUtilizationHolder u) {
		for (UtilizationClass uc : UtilizationClass.values()) {
			assertThat(lps.basalAreas[spIndex][uc.index + 1], is(u.getBaseAreaByUtilization().get(uc)));
			assertThat(
					lps.closeUtilizationVolumes[spIndex][uc.index + 1],
					is(u.getCloseUtilizationVolumeByUtilization().get(uc))
			);
			assertThat(
					lps.cuVolumesMinusDecay[spIndex][uc.index + 1],
					is(u.getCloseUtilizationVolumeNetOfDecayByUtilization().get(uc))
			);
			assertThat(
					lps.cuVolumesMinusDecayAndWastage[spIndex][uc.index + 1],
					is(u.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().get(uc))
			);
			if (uc.index <= 0) {
				assertThat(lps.loreyHeights[spIndex][uc.index + 1], is(u.getLoreyHeightByUtilization().get(uc)));
			}
			assertThat(
					lps.quadMeanDiameters[spIndex][uc.index + 1], is(u.getQuadraticMeanDiameterByUtilization().get(uc))
			);
			assertThat(lps.treesPerHectare[spIndex][uc.index + 1], is(u.getTreesPerHectareByUtilization().get(uc)));
			assertThat(lps.wholeStemVolumes[spIndex][uc.index + 1], is(u.getWholeStemVolumeByUtilization().get(uc)));
		}
	}

	private void verifyBankSpeciesMatchesSpecies(Bank bank, int index, VdypSpecies species) {
		assertThat(bank.sp64Distributions[index], is(species.getSp64DistributionSet()));
		assertThat(bank.speciesIndices[index], is(species.getGenusIndex()));
		assertThat(bank.speciesNames[index], is(species.getGenus()));

		species.getSite().ifPresentOrElse(site -> {
			assertThat(
					bank.yearsAtBreastHeight[index],
					is(site.getYearsAtBreastHeight().orElse(VdypEntity.MISSING_FLOAT_VALUE))
			);
			assertThat(bank.ageTotals[index], is(site.getAgeTotal().orElse(VdypEntity.MISSING_FLOAT_VALUE)));
			assertThat(bank.dominantHeights[index], is(site.getHeight().orElse(VdypEntity.MISSING_FLOAT_VALUE)));
			assertThat(bank.siteIndices[index], is(site.getSiteIndex().orElse(VdypEntity.MISSING_FLOAT_VALUE)));
			assertThat(
					bank.yearsToBreastHeight[index],
					is(site.getYearsToBreastHeight().orElse(VdypEntity.MISSING_FLOAT_VALUE))
			);
			site.getSiteCurveNumber().ifPresentOrElse(scn -> {
				assertThat(bank.siteCurveNumbers[index], is(scn));
			}, () -> {
				assertThat(bank.siteCurveNumbers[index], is(VdypEntity.MISSING_INTEGER_VALUE));
			});
			assertThat(bank.speciesNames[index], is(site.getSiteGenus()));
		}, () -> {
			assertThat(bank.yearsAtBreastHeight[index], is(VdypEntity.MISSING_FLOAT_VALUE));
			assertThat(bank.ageTotals[index], is(VdypEntity.MISSING_FLOAT_VALUE));
			assertThat(bank.dominantHeights[index], is(VdypEntity.MISSING_FLOAT_VALUE));
			assertThat(bank.siteIndices[index], is(VdypEntity.MISSING_FLOAT_VALUE));
			assertThat(bank.yearsToBreastHeight[index], is(VdypEntity.MISSING_FLOAT_VALUE));
			assertThat(bank.siteCurveNumbers[index], is(VdypEntity.MISSING_INTEGER_VALUE));
		});
	}

}
