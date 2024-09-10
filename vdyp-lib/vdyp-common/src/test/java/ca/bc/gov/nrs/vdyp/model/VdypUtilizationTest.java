package ca.bc.gov.nrs.vdyp.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class VdypUtilizationTest {

	@Test
	void build() throws Exception {
		var polygonId = new PolygonIdentifier("Polygon1", 2024);
		var layerType = LayerType.PRIMARY;
		var genusIndex = Integer.valueOf(3);
		var genus = Optional.of("B");
		var ucIndex = UtilizationClass.ALL;
		var basalArea = 35.0f;
		var liveTreesPerHectare = 500.0f;
		var loreyHeight = 45.0f;
		var wholeStemVolume = 4.3f;
		var closeUtilizationVolume = 4.1f;
		var quadraticMeanDiameterAtBH = 0.95f;
		var cuVolumeMinusDecay = 3.9f;
		var cuVolumeMinusDecayWastage = 3.7f;
		var cuVolumeMinusDecayWastageBreakage = 3.5f;

		VdypUtilization u = new VdypUtilization(
				polygonId, layerType, genusIndex, genus, ucIndex, basalArea, liveTreesPerHectare, loreyHeight,
				wholeStemVolume, closeUtilizationVolume, cuVolumeMinusDecay, cuVolumeMinusDecayWastage,
				cuVolumeMinusDecayWastageBreakage, quadraticMeanDiameterAtBH
		);

		assertThat(u.getPolygonId(), is(new PolygonIdentifier("Polygon1", 2024)));
		assertThat(u.getLayerType(), is(LayerType.PRIMARY));
		assertThat(u.getGenusIndex(), is(Integer.valueOf(3)));
		assertThat(u.getGenus(), is(Optional.of("B")));
		assertThat(u.getUcIndex(), is(UtilizationClass.ALL));
		assertThat(u.getBasalArea(), is(35.0f));
		assertThat(u.getLiveTreesPerHectare(), is(500.0f));
		assertThat(u.getLoreyHeight(), is(45.0f));
		assertThat(u.getWholeStemVolume(), is(4.3f));
		assertThat(u.getCloseUtilizationVolume(), is(4.1f));
		assertThat(u.getQuadraticMeanDiameterAtBH(), is(0.95f));
		assertThat(u.getCuVolumeMinusDecay(), is(3.9f));
		assertThat(u.getCuVolumeMinusDecayWastage(), is(3.7f));
		assertThat(u.getCuVolumeMinusDecayWastageBreakage(), is(3.5f));
	}
}
