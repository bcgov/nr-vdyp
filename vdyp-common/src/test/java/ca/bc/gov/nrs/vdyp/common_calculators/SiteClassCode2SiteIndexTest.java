package ca.bc.gov.nrs.vdyp.common_calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.ClassErrorException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.ForestInventoryZoneException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.SpeciesErrorException;

class SiteClassCode2SiteIndexTest {
	// Taken from sindex.h
	/* define species and equation indices */
	private static final short SI_SPEC_ACT = 5;
	private static final short SI_SPEC_AT = 8;
	private static final short SI_SPEC_BA = 11;
	private static final short SI_SPEC_BL = 16;
	private static final short SI_SPEC_CWC = 23;
	private static final short SI_SPEC_CWI = 24;
	private static final short SI_SPEC_DR = 29;
	private static final short SI_SPEC_EA = 31;
	private static final short SI_SPEC_EP = 34;
	private static final short SI_SPEC_FDC = 39;
	private static final short SI_SPEC_FDI = 40;
	private static final short SI_SPEC_HWC = 47;
	private static final short SI_SPEC_HWI = 48;
	private static final short SI_SPEC_LA = 57;
	private static final short SI_SPEC_LT = 59;
	private static final short SI_SPEC_LW = 60;
	private static final short SI_SPEC_MB = 62;
	private static final short SI_SPEC_PA = 76;
	private static final short SI_SPEC_PF = 77;
	private static final short SI_SPEC_PLI = 81;
	private static final short SI_SPEC_PW = 85;
	private static final short SI_SPEC_PY = 87;
	private static final short SI_SPEC_SB = 95;
	private static final short SI_SPEC_SE = 96;
	private static final short SI_SPEC_SS = 99;
	private static final short SI_SPEC_SW = 100;
	private static final short SI_SPEC_YC = 130;

	private static final char[] validSiteCl = { 'G', 'M', 'P', 'L' };

	@Test
	void testClassErrorExcepion() {
		assertThrows(ClassErrorException.class, () -> SiteClassCode2SiteIndex.classToIndex(SI_SPEC_ACT, 'A', 'A'));
	}

	@Test
	void testSI_SPEC_ActAndMb() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_MB, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 26);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_ACT, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 18);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_ACT, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 9);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_ACT, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 3);
	}

	@Test
	void testSI_SPEC_AtEaAndEp() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_AT, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 27);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_EA, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 20);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_EP, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 12);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_EP, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 4);
	}

	@Test
	void testSI_SPEC_BA() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_BA, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 29);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_BA, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 23);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_BA, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 14);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_BA, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 5);
	}

	@Test
	void testSI_SPEC_BL() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_BL, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 18);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_BL, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 15);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_BL, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 11);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_BL, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 5);
	}

	@Test
	void testSI_SPEC_CwcAndYc() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_CWC, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 29);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_YC, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 23);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_YC, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 15);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_YC, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 6);
	}

	@Test
	void testSI_SPEC_Cwi() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_CWI, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 22);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_CWI, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 19);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_CWI, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 13);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_CWI, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 5);
	}

	@Test
	void testSI_SPEC_Dr() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_DR, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 33);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_DR, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 23);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_DR, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 13);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_DR, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 6);
	}

	@Test
	void testSI_SPEC_Fdc() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_FDC, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 32);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_FDC, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 27);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_FDC, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 18);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_FDC, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 7);
	}

	@Test
	void testSI_SPEC_Fdi() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_FDI, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 20);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_FDI, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 17);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_FDI, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 12);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_FDI, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 5);
	}

	@Test
	void testSI_SPEC_Hwc() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWC, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 28);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWC, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 22);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWC, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 14);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWC, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 5);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWC, validSiteCl[0], 'D');
		assertEquals(actualResult, (double) 21);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWC, validSiteCl[1], 'D');
		assertEquals(actualResult, (double) 18);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWC, validSiteCl[2], 'D');
		assertEquals(actualResult, (double) 12);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWC, validSiteCl[3], 'D');
		assertEquals(actualResult, (double) 4);

		assertThrows(
				ForestInventoryZoneException.class,
				() -> SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWC, validSiteCl[3], 'X')
		);
	}

	@Test
	void testSI_SPEC_Hwi() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWI, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 21);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWI, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 18);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWI, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 12);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_HWI, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 4);
	}

	@Test
	void testSI_SPEC_LaLtAndLw() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_LW, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 20);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_LA, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 16);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_LT, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 10);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_LW, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 3);
	}

	@Test
	void testSI_SPEC_PliPaAndPf() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PLI, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 20);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PA, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 16);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PF, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 11);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PF, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 4);
	}

	@Test
	void testSI_SPEC_Py() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PY, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 17);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PY, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 14);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PY, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 10);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PY, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 4);
	}

	@Test
	void testSI_SPEC_Pw() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PW, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 28);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PW, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 22);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PW, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 12);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_PW, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 4);
	}

	@Test
	void testSI_SPEC_Ss() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_SS, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 28);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_SS, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 21);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_SS, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 11);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_SS, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 4);
	}

	@Test
	void testSI_SPEC_SbSwAndSe() {
		double actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_SB, validSiteCl[0], 'A');
		assertEquals(actualResult, (double) 19);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_SW, validSiteCl[1], 'A');
		assertEquals(actualResult, (double) 15);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_SE, validSiteCl[2], 'A');
		assertEquals(actualResult, (double) 10);

		actualResult = SiteClassCode2SiteIndex.classToIndex(SI_SPEC_SE, validSiteCl[3], 'A');
		assertEquals(actualResult, (double) 5);
	}

	@Test
	void testSpeciesErrorException() {
		assertThrows(
				SpeciesErrorException.class,
				() -> SiteClassCode2SiteIndex.classToIndex((short) 150, validSiteCl[3], 'X')
		);
	}
}
