package ca.bc.gov.nrs.vdyp.io.parse.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;

class VdypUtilizationParserTest {

	@Test
	void testEmpty() throws IOException, ResourceParseException {
		var controlMap = TestUtils.loadControlMap();
		var resolver = new MockFileResolver("testResolver");

		try (var is = TestUtils.makeInputStream("")) {

			resolver.addStream("test.dat", is);

			var parser = new VdypUtilizationParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(!stream.hasNext(), "stream is not empty");

			assertThrows(NoSuchElementException.class, () -> stream.next());
		}
	}

	@Test
	void testOnePoly() throws IOException, ResourceParseException {
		var controlMap = TestUtils.loadControlMap();
		var resolver = new MockFileResolver("testResolver");

		try (
				var is = TestUtils.makeInputStream(
						"01002 S000001 00     1970 P  0    -1  0.01513     5.24   7.0166   0.0630   0.0000   0.0000   0.0000   0.0000   6.1",
						"01002 S000001 00     1970 P  0     0 44.93259   595.32  30.9724 620.9775 592.2023 580.1681 577.6229 549.0159  31.0",
						"01002 S000001 00     1970 P  0     1  0.53100    64.82  -9.0000   2.5979   0.3834   0.3794   0.3788   0.3623  10.2",
						"01002 S000001 00     1970 P  0     2  1.27855    71.93  -9.0000   9.1057   6.9245   6.8469   6.8324   6.5384  15.0",
						"01002 S000001 00     1970 P  0     3  2.33020    73.60  -9.0000  22.4019  20.1244  19.8884  19.8375  18.9555  20.1",
						"01002 S000001 00     1970 P  0     4 40.79285   384.98  -9.0000 586.8720 564.7699 553.0534 550.5741 523.1597  36.7",
						"01002 S000001 00     1970 P  3 B  -1  0.00000     0.00   8.0272   0.0000   0.0000   0.0000   0.0000   0.0000   6.1",
						"01002 S000001 00     1970 P  3 B   0  0.40292     5.16  36.7553   6.2098   5.9592   5.8465   5.8163   5.5177  31.5",
						"01002 S000001 00     1970 P  3 B   1  0.00502     0.76  -9.0000   0.0185   0.0009   0.0009   0.0009   0.0009   9.2",
						"01002 S000001 00     1970 P  3 B   2  0.01363     0.93  -9.0000   0.0757   0.0498   0.0497   0.0496   0.0475  13.7",
						"01002 S000001 00     1970 P  3 B   3  0.02284     0.88  -9.0000   0.1748   0.1521   0.1514   0.1512   0.1445  18.2",
						"01002 S000001 00     1970 P  3 B   4  0.36143     2.60  -9.0000   5.9408   5.7564   5.6446   5.6146   5.3249  42.1",
						"01002 S000001 00     1970 P  4 C  -1  0.01243     4.40   6.4602   0.0507   0.0000   0.0000   0.0000   0.0000   6.0",
						"01002 S000001 00     1970 P  4 C   0  5.04597    83.46  22.9584  43.4686  39.4400  36.2634  35.2930  32.9144  27.7",
						"01002 S000001 00     1970 P  4 C   1  0.12822    16.12  -9.0000   0.6027   0.1116   0.1094   0.1090   0.1035  10.1",
						"01002 S000001 00     1970 P  4 C   2  0.31003    17.87  -9.0000   1.9237   1.4103   1.3710   1.3628   1.2915  14.9",
						"01002 S000001 00     1970 P  4 C   3  0.51339    16.55  -9.0000   3.8230   3.3162   3.2127   3.1859   3.0076  19.9",
						"01002 S000001 00     1970 P  4 C   4  4.09434    32.92  -9.0000  37.1192  34.6019  31.5703  30.6352  28.5119  39.8",
						"01002 S000001 00     1970 P  5 D  -1  0.00155     0.47  10.6033   0.0078   0.0000   0.0000   0.0000   0.0000   6.5",
						"01002 S000001 00     1970 P  5 D   0 29.30249   287.70  33.7440 459.5233 444.0844 436.5280 435.2818 413.6949  36.0",
						"01002 S000001 00     1970 P  5 D   1  0.01412     1.64  -9.0000   0.1091   0.0571   0.0566   0.0565   0.0541  10.5",
						"01002 S000001 00     1970 P  5 D   2  0.05128     2.69  -9.0000   0.5602   0.5048   0.5007   0.5005   0.4783  15.6",
						"01002 S000001 00     1970 P  5 D   3  0.45736    13.82  -9.0000   6.0129   5.6414   5.5975   5.5948   5.3383  20.5",
						"01002 S000001 00     1970 P  5 D   4 28.77972   269.56  -9.0000 452.8412 437.8810 430.3732 429.1300 407.8242  36.9",
						"01002 S000001 00     1970 P  8 H  -1  0.00000     0.00   7.5464   0.0000   0.0000   0.0000   0.0000   0.0000  -9.0",
						"01002 S000001 00     1970 P  8 H   0  5.81006   167.90  22.7704  55.8878  49.8291  49.0742  48.8550  46.6828  21.0",
						"01002 S000001 00     1970 P  8 H   1  0.36138    43.57  -9.0000   1.7385   0.1925   0.1913   0.1911   0.1834  10.3",
						"01002 S000001 00     1970 P  8 H   2  0.82449    45.99  -9.0000   5.8666   4.4155   4.3846   4.3789   4.2023  15.1",
						"01002 S000001 00     1970 P  8 H   3  1.07566    33.93  -9.0000   9.6521   8.5752   8.5019   8.4827   8.1397  20.1",
						"01002 S000001 00     1970 P  8 H   4  3.54853    44.42  -9.0000  38.6306  36.6459  35.9963  35.8023  34.1574  31.9",
						"01002 S000001 00     1970 P 15 S  -1  0.00115     0.36   8.2003   0.0045   0.0000   0.0000   0.0000   0.0000   6.3",
						"01002 S000001 00     1970 P 15 S   0  4.37115    51.10  32.0125  55.8879  52.8895  52.4561  52.3768  50.2060  33.0",
						"01002 S000001 00     1970 P 15 S   1  0.02225     2.73  -9.0000   0.1291   0.0213   0.0212   0.0212   0.0204  10.2",
						"01002 S000001 00     1970 P 15 S   2  0.07911     4.46  -9.0000   0.6795   0.5440   0.5410   0.5406   0.5189  15.0",
						"01002 S000001 00     1970 P 15 S   3  0.26095     8.43  -9.0000   2.7391   2.4396   2.4250   2.4229   2.3254  19.9",
						"01002 S000001 00     1970 P 15 S   4  4.00883    35.49  -9.0000  52.3402  49.8846  49.4689  49.3920  47.3414  37.9",
						"01002 S000001 00     1970  "
				)
		) {

			resolver.addStream("test.dat", is);

			var parser = new VdypUtilizationParser().map("test.dat", resolver, controlMap);

			var stream = parser.get();

			assertTrue(stream.hasNext(), "stream is empty");

			var result = new ArrayList<>(stream.next()); // Array list makes it indexable so we can easily sample
															// entries to test. Checking everything would be excessive.

			assertThat(result, hasSize(6 * UtilizationClass.values().length));

			assertThat(
					result.get(1),
					allOf(
							hasProperty("genus", notPresent()), hasProperty("genusIndex", is(0)),
							hasProperty("ucIndex", is(UtilizationClass.ALL)),
							hasProperty("basalArea", closeTo(44.93259f)),
							hasProperty("liveTreesPerHectare", closeTo(595.32f)),
							hasProperty("loreyHeight", closeTo(30.9724f)),
							hasProperty("wholeStemVolume", closeTo(620.9775f)),
							hasProperty("closeUtilizationVolume", closeTo(592.2023f)),
							hasProperty("cuVolumeMinusDecay", closeTo(580.1681f)),
							hasProperty("cuVolumeMinusDecayWastage", closeTo(577.6229f)),
							hasProperty("cuVolumeMinusDecayWastageBreakage", closeTo(549.0159f))
					)
			);

			assertThat(
					result.get(32),
					allOf(
							hasProperty("genus", present(is("S"))), hasProperty("genusIndex", is(15)),
							hasProperty("ucIndex", is(UtilizationClass.U75TO125)),
							hasProperty("basalArea", closeTo(0.02225f)),
							hasProperty("liveTreesPerHectare", closeTo(2.73f)),
							hasProperty("loreyHeight", is(Float.NaN)), hasProperty("wholeStemVolume", closeTo(0.1291f)),
							hasProperty("closeUtilizationVolume", closeTo(0.0213f)),
							hasProperty("cuVolumeMinusDecay", closeTo(0.0212f)),
							hasProperty("cuVolumeMinusDecayWastage", closeTo(0.0212f)),
							hasProperty("cuVolumeMinusDecayWastageBreakage", closeTo(0.0204f))
					)
			);
			assertTrue(!stream.hasNext(), "stream is not empty");

			assertThrows(NoSuchElementException.class, () -> stream.next());

		}
	}

}