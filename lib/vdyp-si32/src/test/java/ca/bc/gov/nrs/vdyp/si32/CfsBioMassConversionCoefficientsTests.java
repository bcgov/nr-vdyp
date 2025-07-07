package ca.bc.gov.nrs.vdyp.si32;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsDead;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsDetails;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsForGenus;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsForSpecies;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedEcoZone;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedGenera;

class CfsBioMassConversionCoefficientsTests {

	@Test
	void test_dead() {
		CfsBiomassConversionCoefficientsDetails d1 = CfsBiomassConversionCoefficientsDead.get(1, 1);
		CfsBiomassConversionCoefficientsDetails d2 = CfsBiomassConversionCoefficientsDead.get(1, 2);

		assertTrue(d1.equals(d1));
		assertFalse(d1.equals(d2));
		assertThat(d1.toString(), Matchers.notNullValue());
		assertTrue(d1.hashCode() == d1.hashCode());
	}

	@Test
	void test_forGenus() {
		CfsBiomassConversionCoefficientsDetails d1 = CfsBiomassConversionCoefficientsForGenus.get(1, 1);
		CfsBiomassConversionCoefficientsDetails d2 = CfsBiomassConversionCoefficientsForGenus.get(1, 2);

		assertTrue(d1.equals(d1));
		assertFalse(d1.equals(d2));
		assertThat(d1.toString(), Matchers.notNullValue());
		assertTrue(d1.hashCode() == d1.hashCode());
	}

	@Test
	void test_forSpecies() {
		CfsBiomassConversionCoefficientsDetails d1 = CfsBiomassConversionCoefficientsForSpecies.get(1, 1);
		CfsBiomassConversionCoefficientsDetails d2 = CfsBiomassConversionCoefficientsForSpecies.get(1, 2);

		assertTrue(d1.equals(d1));
		assertFalse(d1.equals(d2));
		assertThat(d1.toString(), Matchers.notNullValue());
		assertTrue(d1.hashCode() == d1.hashCode());
	}

	static Stream<Arguments> cfsGeneraByEcoZoneAndSpecies() {
		return Stream.of(
				Arguments.of(
						CfsBiomassConversionSupportedEcoZone.UNKNOWN, "A", CfsBiomassConversionSupportedGenera.INVALID
				),
				Arguments.of(
						CfsBiomassConversionSupportedEcoZone.TAIGA_PLAINS, "BAD",
						CfsBiomassConversionSupportedGenera.INVALID
				),
				Arguments.of(
						CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA, "A",
						CfsBiomassConversionSupportedGenera.INVALID
				),
				Arguments.of(
						CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA, "A",
						CfsBiomassConversionSupportedGenera.INVALID
				),
				Arguments.of(
						CfsBiomassConversionSupportedEcoZone.MONTANE_CORDILLERA, "SXW",
						CfsBiomassConversionSupportedGenera.S
				),
				Arguments.of(
						CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA, "SWX",
						CfsBiomassConversionSupportedGenera.INVALID
				),
				Arguments.of(
						CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA, "B",
						CfsBiomassConversionSupportedGenera.B
				),
				Arguments.of(
						CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA, "C",
						CfsBiomassConversionSupportedGenera.C
				),
				Arguments.of(
						CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA, "D",
						CfsBiomassConversionSupportedGenera.D
				)
		);
	}

	@ParameterizedTest
	@MethodSource("cfsGeneraByEcoZoneAndSpecies")
	void testGetCfsBiomassConversionGenera(
			CfsBiomassConversionSupportedEcoZone ecoZone, String sp64, CfsBiomassConversionSupportedGenera expectedGenus
	) {

		assertThat(CfsBiomassConversionSupportedGenera.fromEcoZoneAndSpecies(ecoZone, sp64), is(expectedGenus));
	}

	static Stream<Arguments> cfsSupportedEcoZonesByCode() {
		return Stream.of(
				Arguments.of((short) 4, CfsBiomassConversionSupportedEcoZone.TAIGA_PLAINS),
				Arguments.of((short) 9, CfsBiomassConversionSupportedEcoZone.BOREAL_PLAINS),
				Arguments.of((short) 12, CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA),
				Arguments.of((short) 13, CfsBiomassConversionSupportedEcoZone.PACIFIC_MARITIME),
				Arguments.of((short) 14, CfsBiomassConversionSupportedEcoZone.MONTANE_CORDILLERA),
				Arguments.of((short) 1, CfsBiomassConversionSupportedEcoZone.UNKNOWN)
		);
	}

	@ParameterizedTest
	@MethodSource("cfsSupportedEcoZonesByCode")
	void testGetSupportedEcozoneByCode(short code, CfsBiomassConversionSupportedEcoZone expectedEcoZone) {
		assertThat(CfsBiomassConversionSupportedEcoZone.of(code), is(expectedEcoZone));
	}

	static Stream<Arguments> cfsSupportedEcoZonesByBecZoneDData() {
		return Stream.of(
				Arguments.of("BWBS", null, null, "", CfsBiomassConversionSupportedEcoZone.TAIGA_PLAINS),
				Arguments.of("BWBS", "DK", "1", "", CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA),
				Arguments.of("BWBS", "mw", "1", "", CfsBiomassConversionSupportedEcoZone.BOREAL_PLAINS),
				Arguments.of("BWBS", "wk", "X", "", CfsBiomassConversionSupportedEcoZone.BOREAL_PLAINS),
				Arguments.of("BWBS", null, null, "093", CfsBiomassConversionSupportedEcoZone.BOREAL_PLAINS),
				Arguments.of("BWBS", "BS", null, "104", CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA),
				Arguments.of("AT", null, null, "", CfsBiomassConversionSupportedEcoZone.MONTANE_CORDILLERA),
				Arguments.of("AT", null, null, "092", CfsBiomassConversionSupportedEcoZone.PACIFIC_MARITIME),
				Arguments.of("CWH", null, null, "", CfsBiomassConversionSupportedEcoZone.PACIFIC_MARITIME),
				Arguments.of("ICH", null, null, "", CfsBiomassConversionSupportedEcoZone.MONTANE_CORDILLERA),
				Arguments.of("ICH", "MC", null, "", CfsBiomassConversionSupportedEcoZone.PACIFIC_MARITIME),
				Arguments.of("MS", null, null, "", CfsBiomassConversionSupportedEcoZone.MONTANE_CORDILLERA),
				Arguments.of("ESSF", null, null, "", CfsBiomassConversionSupportedEcoZone.MONTANE_CORDILLERA),
				Arguments.of("SWB", null, null, "", CfsBiomassConversionSupportedEcoZone.BOREAL_CORDILLERA),
				Arguments.of("BADVALUE", null, null, "", CfsBiomassConversionSupportedEcoZone.UNKNOWN)
		);
	}

	@ParameterizedTest
	@MethodSource("cfsSupportedEcoZonesByBecZoneDData")
	void testGetSupportedEcozoneByBecZone(
			String becZone, String subBec, String variant, String mapSheet,
			CfsBiomassConversionSupportedEcoZone expectedEcoZone
	) {
		assertThat(
				CfsBiomassConversionSupportedEcoZone.fromBecZoneData(becZone, subBec, variant, mapSheet),
				is(expectedEcoZone)
		);
	}
}
