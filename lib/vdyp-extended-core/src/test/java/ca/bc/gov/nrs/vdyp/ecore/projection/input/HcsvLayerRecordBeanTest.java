package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.LAYER_CSV_HEADER_LINE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

class HcsvLayerRecordBeanTest {

	private static final String EXTENDED_HEADER = LAYER_CSV_HEADER_LINE
			+ ",EST_AGE_SPP3,EST_HEIGHT_SPP3,EST_AGE_SPP4,EST_HEIGHT_SPP4"
			+ ",EST_AGE_SPP5,EST_HEIGHT_SPP5,EST_AGE_SPP6,EST_HEIGHT_SPP6"
			+ ",EST_SITE_INDEX_SPP2,EST_SITE_INDEX_SPP3,EST_SITE_INDEX_SPP4,EST_SITE_INDEX_SPP5,EST_SITE_INDEX_SPP6";

	// pos0-3: FEATURE_ID, TREE_COVER_LAYER_ESTIMATED_ID, MAP_ID, POLYGON_NUMBER
	private static final String PREFIX = "13919428,14321067,093C090,94833422,";

	private HcsvLayerRecordBean parseFirstBean(String header, String dataRow) {
		String csv = header + "\n" + PREFIX + dataRow;
		var stream = HcsvLayerRecordBean.createHcsvLayerStream(new ByteArrayInputStream(csv.getBytes()));
		return stream.parse().get(0);
	}

	@Test
	void testSiteIndexNullForAllSpeciesWhenOldFormatCsvUsed() {
		// Old-format CSV (no per-species site index columns): estimatedSiteIndex is null for all species
		// pos4-37 (34 fields): layer-level values, 2 species (PLI 60%, FD 40%), age/height for spp1+spp2
		String row = "1,P,,,,,,5,1.0,150,PLI,60.0,FD,40.0,,,,,,,,,60,9.0,50,8.5,,,,,,,,";
		List<HcsvLayerRecordBean.SpeciesDetails> details = parseFirstBean(LAYER_CSV_HEADER_LINE, row).getSpeciesDetails();

		assertThat(details.get(0).estimatedSiteIndex(), is(nullValue()));
		assertThat(details.get(1).estimatedSiteIndex(), is(nullValue()));
	}

	@Test
	void testSiteIndexSpp2IsPopulatedFromEstSiteIndexSpp2Column() {
		// EST_SITE_INDEX_SPP2 = 22.5 for FD (species 2 in this row)
		// pos4-50 (47 fields): standard fields + 13 new optional fields; pos46=22.5
		String row = "1,P,,,,,,5,1.0,150,PLI,60.0,FD,40.0,,,,,,,,,60,9.0,50,8.5,,,,,,,,,,,,,,,,,22.5,,,,";
		List<HcsvLayerRecordBean.SpeciesDetails> details = parseFirstBean(EXTENDED_HEADER, row).getSpeciesDetails();

		assertThat(details.get(0).estimatedSiteIndex(), is(nullValue())); // SPP1 has no per-species SI column
		assertThat(details.get(1).estimatedSiteIndex(), is(22.5));
	}

	@Test
	void testAgeHeightAndSiteIndexSpp3ArePopulatedFromNewColumns() {
		// 3 species: PLI 50%, FD 30%, CW 20%
		// EST_AGE_SPP3=55 (pos38), EST_HEIGHT_SPP3=12.5 (pos39), EST_SITE_INDEX_SPP3=18.0 (pos47)
		String row = "1,P,,,,,,5,1.0,150,PLI,50.0,FD,30.0,CW,20.0,,,,,,,60,9.0,50,8.5,,,,,,,,,55,12.5,,,,,,,,18.0,,,";
		List<HcsvLayerRecordBean.SpeciesDetails> details = parseFirstBean(EXTENDED_HEADER, row).getSpeciesDetails();

		HcsvLayerRecordBean.SpeciesDetails spp3 = details.get(2); // CW is third species (index 2)
		assertThat(spp3.estimatedAge(), is((short) 55));
		assertThat(spp3.estimatedHeight(), is(12.5));
		assertThat(spp3.estimatedSiteIndex(), is(18.0));
	}
}
