package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.LAYER_CSV_HEADER_LINE;
import static ca.bc.gov.nrs.vdyp.test.TestUtils.POLYGON_CSV_HEADER_LINE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.opencsv.bean.exceptionhandler.ExceptionHandlerQueue;
import com.opencsv.exceptions.CsvConstraintViolationException;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.NonVegetationTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;

class HcsvPolygonStreamTest {
	@Test
	void testDuplicateRankOneMessageIncludesPolygonAndLayer() throws Exception {
		String layers = layerFileHeader + defaultLayerPrefix
				+ "1,P,,1,,,,20,10,300,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n" + defaultLayerPrefix
				+ "2,P,,1,,,,20,10,300,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n";
		var polygon = new HcsvPolygonStream(
				context, new ByteArrayInputStream(hcsvPolygonFileContents.getBytes()),
				new ByteArrayInputStream(layers.getBytes())
		).getNextPolygon();
		var messages = polygon.getMessages().stream()
				.filter(m -> m.getKind() == PolygonMessageKind.POLYGON_ALREADY_HAS_RANK_ONE_LAYER).toList();
		assertThat(messages.size(), is(1));
		var message = messages.get(0);
		assertThat(message.getLayer().getLayerId(), is("2"));
		assertThat(message.getSeverity(), is(MessageSeverityCode.WARNING));
		assertThat(
				message.getReportMessageText(),
				is("WARN: Polygon 13,919,428: Rank '1' Layer was already encountered (Layer: '2')")
		);
	}

	Parameters p;
	ProjectionContext context;
	final String hcsvPolygonFileContents = POLYGON_CSV_HEADER_LINE + "\n"
			+ "13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,NA,,,,,,,,,TC,100,,,,";
	String layerFileHeader = LAYER_CSV_HEADER_LINE + "\n";
	String defaultLayerPrefix = "13919428,14321067,093C090,94833422,";

	// Standard header extended with the 13 new optional columns (positions 38-50)
	static final String EXTENDED_LAYER_CSV_HEADER_LINE = LAYER_CSV_HEADER_LINE
			+ ",EST_AGE_SPP3,EST_HEIGHT_SPP3,EST_AGE_SPP4,EST_HEIGHT_SPP4"
			+ ",EST_AGE_SPP5,EST_HEIGHT_SPP5,EST_AGE_SPP6,EST_HEIGHT_SPP6"
			+ ",EST_SITE_INDEX_SPP2,EST_SITE_INDEX_SPP3,EST_SITE_INDEX_SPP4,EST_SITE_INDEX_SPP5,EST_SITE_INDEX_SPP6";

	@BeforeEach
	void setup() throws AbstractProjectionRequestException {
		p = new Parameters().ageStart(0).ageEnd(100);
		context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", p, false);
	}

	static Stream<Arguments> duplicateTypes() {
		return Stream.of(Arguments.of("P"), Arguments.of("Y"), Arguments.of("R"), Arguments.of("V"), Arguments.of("D"));
	}

	@ParameterizedTest
	@MethodSource("duplicateTypes")
	void testPolygonStreamGracefulDuplicateProjectionType(String type) throws AbstractProjectionRequestException {
		String hcsvLayerFileContents = layerFileHeader + defaultLayerPrefix
				+ "1,P,,,,,,5,1.000050,150,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n" + defaultLayerPrefix + "2," + type
				+ ",,,,,,5,1.000050,150,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n" + defaultLayerPrefix + "3," + type
				+ ",,,,,,5,1.000050,150,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n";

		HcsvPolygonStream unit = new HcsvPolygonStream(
				context, new ByteArrayInputStream(hcsvPolygonFileContents.getBytes()),
				new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
		);

		Polygon poly = unit.getNextPolygon();

		assertThat(poly.getMessages().size(), is(0));
	}

	@Test
	void testStockabilityHigherThanPolygon() throws PolygonValidationException {
		String hcsvLayerFileContents = layerFileHeader + defaultLayerPrefix
				+ "1,P,60.0,,,,,5,1.000050,150,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n"; // Stockability exceeds
																							// polygon (warning)

		HcsvPolygonStream unit = new HcsvPolygonStream(
				context, new ByteArrayInputStream(hcsvPolygonFileContents.getBytes()),
				new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
		);

		Polygon poly = unit.getNextPolygon();

		assertThat(poly.getMessages().size(), is(1));
		assertThat(poly.getMessages().get(0).toString().contains("stockability"), is(true));
	}

	@Test
	void testRejectedPolygonConsumesAllItsLayerErrors() {
		String layers = layerFileHeader + defaultLayerPrefix
				+ "1,P,,1,,,,20,10,300,,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n" + defaultLayerPrefix
				+ "2,P,,,,,,20,10,300,,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n";
		var stream = new HcsvPolygonStream(
				context,
				new ByteArrayInputStream(hcsvPolygonFileContents.replace(",UNK,V,UNK,", ",UNK,Z,UNK,").getBytes()),
				new ByteArrayInputStream(layers.getBytes())
		);
		var error = assertThrows(PolygonValidationException.class, stream::getNextPolygon);
		assertThat(error.getValidationMessages().size(), is(3));
		assertThat(error.getMessage().contains("Layer 1"), is(true));
		assertThat(error.getMessage().contains("Layer 2"), is(true));
		assertThat(stream.hasNextPolygon(), is(false));
		assertThat(stream.drainRemainingValidationMessages().isEmpty(), is(true));
	}

	@Test
	void testUnmatchedLayerErrorsAreDrainedOnlyOnce() {
		var stream = new HcsvPolygonStream(
				context, new ByteArrayInputStream( (POLYGON_CSV_HEADER_LINE + "\n").getBytes()),
				new ByteArrayInputStream( (layerFileHeader + "13919428\n").getBytes())
		);
		assertThat(stream.hasNextPolygon(), is(false));
		assertThat(stream.drainRemainingValidationMessages().size(), is(1));
		assertThat(stream.drainRemainingValidationMessages().isEmpty(), is(true));
	}

	@Test
	void testRejectedPolygonExceptionRetainsFeatureId() {
		var csv = HcsvPolygonRecordBean.createHcsvPolygonStream(
				new ExceptionHandlerQueue(),
				new ByteArrayInputStream(hcsvPolygonFileContents.replace(",UNK,V,UNK,", ",UNK,Z,UNK,").getBytes())
		);
		assertThat(csv.parse().isEmpty(), is(true));
		assertThat(csv.getCapturedExceptions().size(), is(1));
		var captured = (CsvConstraintViolationException) csv.getCapturedExceptions().get(0);
		var validation = (PolygonValidationException) captured.getSourceObject();
		assertThat(validation.getFeatureID(), is(13919428L));
		assertThat(validation.getContextPrefix(), is("Polygon 13919428"));
	}

	@Test
	void testToPolygonValidationExceptionIncludesFeatureIdWhenKnown() {
		var message = new ValidationMessage(ValidationMessageKind.GENERIC, "boom");
		var ex = HcsvPolygonStream.toPolygonValidationException(13919428L, message);
		assertThat(ex.getMessage(), allOf(startsWith("Polygon 13919428"), containsString("boom")));
	}

	@Test
	void testToPolygonValidationExceptionOmitsFeatureIdWhenUnknown() {
		var message = new ValidationMessage(ValidationMessageKind.GENERIC, "boom");
		var ex = HcsvPolygonStream.toPolygonValidationException((Long) null, message);
		assertThat(ex.getMessage(), allOf(containsString("boom")));
	}

	@Test
	void testToPolygonValidationExceptionFromRecordBeanIncludesFeatureId() {
		var records = HcsvPolygonRecordBean
				.createHcsvPolygonStream(new ByteArrayInputStream(hcsvPolygonFileContents.getBytes())).parse();
		var message = new ValidationMessage(ValidationMessageKind.GENERIC, "boom");

		var ex = HcsvPolygonStream.toPolygonValidationException(records.get(0), message);
		assertThat(ex.getMessage(), allOf(startsWith("Polygon 13919428"), containsString("boom")));

		var ex2 = HcsvPolygonStream.toPolygonValidationException((HcsvPolygonRecordBean) null, message);
		assertThat(ex2.getMessage(), containsString("boom"));
	}

	@Nested
	class TestValidationErrorsCaught {
		@Test
		void testPolygonStreamGracefulLayerValidationIssues() throws AbstractProjectionRequestException {
			String hcsvLayerFileContents = layerFileHeader + defaultLayerPrefix
					+ "1,P,,,,,,5,1.000050,150,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n" + // Second Veteran (warning)
					defaultLayerPrefix + "1,P,,,,,,5,1.000050,150,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n"; // Third
																												// Veteran
																												// and
																												// Invalid
																												// Species
																												// Code
																												// (2x
																												// warning)

			HcsvPolygonStream unit = new HcsvPolygonStream(
					context, new ByteArrayInputStream(hcsvPolygonFileContents.getBytes()),
					new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
			);

			assertThrows(PolygonValidationException.class, unit::getNextPolygon);
		}

		static Stream<String> invalidLayerData() {
			return Stream.of(
					",P,,1,,,,5,1.000050,150,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,", // No LayerID
					"1,P,,1,,,,5,1.000050,150,,20.00,,,,,,,,,,,60,9.00,,,,,,,,,,", // Missing Species Code
					"1,P,,1,,XXX,5.0,5,1.000050,150,PLI,20.00,,,,,,,,,,,60,9.00,,,,,,,,,,", // Site Species Invalid
					"1,P,,1,,,,5,1.000050,150,XXX,20.00,,,,,,,,,,,60,9.00,,,,,,,,,,", // Layer Species Code Invalid
					"1,P,,1,,,,5,1.000050,150,PLI,20.00,FD,80.0,,,,,,,,,60,9.00,,,,,,,,,," // non descending order
			);
		}

		@ParameterizedTest
		@MethodSource("invalidLayerData")
		void testInvalidLayerData(String invalidLayerData) throws AbstractProjectionRequestException {
			String hcsvLayerFileContents = layerFileHeader + defaultLayerPrefix + invalidLayerData + "\n";

			HcsvPolygonStream unit = new HcsvPolygonStream(
					context, new ByteArrayInputStream(hcsvPolygonFileContents.getBytes()),
					new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
			);

			assertThrows(PolygonValidationException.class, unit::getNextPolygon);
		}

		@Test
		void testNonVegCoverExceed100PercentError() throws AbstractProjectionRequestException {
			String myHcsvPolygonFileContents = // Non veg cover exceed 100
					POLYGON_CSV_HEADER_LINE + "\n"
							+ "13919428,093C090,94833422,DQU,UNK,UNK,I,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,LA,50,,BS,50,,,50,,TC,100,,,,";

			HcsvPolygonStream unit = new HcsvPolygonStream(
					context, new ByteArrayInputStream(myHcsvPolygonFileContents.getBytes()),
					new ByteArrayInputStream("".getBytes())
			);

			assertThat(unit.hasNextPolygon(), is(true));
			var error = assertThrows(PolygonValidationException.class, unit::getNextPolygon);
			assertThat(error.getMessage().contains("Polygon 13919428"), is(true));
			assertThat(unit.hasNextPolygon(), is(false));
		}

		@Test
		void testNonTreeCoverExceed100PercentError() throws AbstractProjectionRequestException {
			String myHcsvPolygonFileContents = // Non tree cover exceed 100
					POLYGON_CSV_HEADER_LINE + "\n"
							+ "13919428,093C090,94833422,DQU,UNK,UNK,I,UNK,0.6,35,3,HE,35,8,35,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,LA,50,,BS,50,,,50,,TC,100,,,,";

			HcsvPolygonStream unit = new HcsvPolygonStream(
					context, new ByteArrayInputStream(myHcsvPolygonFileContents.getBytes()),
					new ByteArrayInputStream("".getBytes())
			);

			assertThat(unit.hasNextPolygon(), is(true));
			assertThrows(PolygonValidationException.class, () -> unit.getNextPolygon());
			assertThat(unit.hasNextPolygon(), is(false));
		}

		@Test
		void testNoBECZoneError() throws AbstractProjectionRequestException {
			String myHcsvPolygonFileContents = // Non tree cover exceed 100
					POLYGON_CSV_HEADER_LINE + "\n"
							+ "13919428,093C090,94833422,DQU,UNK,UNK,I,UNK,0.6,10,3,HE,35,8,,,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,LA,50,,BS,50,,,50,,TC,100,,,,";

			HcsvPolygonStream unit = new HcsvPolygonStream(
					context, new ByteArrayInputStream(myHcsvPolygonFileContents.getBytes()),
					new ByteArrayInputStream("".getBytes())
			);

			assertThat(unit.hasNextPolygon(), is(true));
			assertThrows(PolygonValidationException.class, () -> unit.getNextPolygon());
			assertThat(unit.hasNextPolygon(), is(false));
		}

		@Test
		void testNegativeSiteIndexSpp2RaisesValidationError() {
			// EST_SITE_INDEX_SPP2=-1.0 is below the valid range [0, MAX_VALUE] and must cause an error
			// pos4-50 (47 fields): standard fields + 13 new; pos46=-1.0
			String hcsvLayerFileContents = EXTENDED_LAYER_CSV_HEADER_LINE + "\n" + defaultLayerPrefix
					+ "1,P,,,,,,5,1.0,150,PLI,60.0,FD,40.0,,,,,,,,,60,9.0,50,8.5,,,,,,,,,,,,,,,,,-1.0,,,,\n";

			HcsvPolygonStream unit = new HcsvPolygonStream(
					context, new ByteArrayInputStream(hcsvPolygonFileContents.getBytes()),
					new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
			);

			assertThrows(PolygonValidationException.class, () -> unit.getNextPolygon());
		}

		@Test
		void testDuplicateRank1Layers() throws PolygonValidationException {
			String hcsvLayerFileContents = layerFileHeader + defaultLayerPrefix
					+ "1,P,,1,,,,5,1.000050,150,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n" + defaultLayerPrefix
					+ "1,P,,1,,,,5,1.000050,150,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n";

			HcsvPolygonStream unit = new HcsvPolygonStream(
					context, new ByteArrayInputStream(hcsvPolygonFileContents.getBytes()),
					new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
			);

			// No Polygon because polygon was invalid
			assertThrows(PolygonValidationException.class, () -> unit.getNextPolygon());
		}

	}

	/**
	 * Verifies that each group of numbered polygon columns is read from its own CSV columns: BCLCS levels 1 to 5,
	 * non-vegetation cover type, percent and pattern 1 to 3, and land cover class code and percent 1 to 3. Each test
	 * takes the CSV values to place in the group's columns and the values that the bean getters must return. Every case
	 * uses different values per column, or fills a single column, so a getter that reads a neighbouring column fails.
	 */
	@Nested
	class TestNumberedPolygonColumns {
		// Columns up to and including NON_PRODUCTIVE_DESCRIPTOR_CD, followed by the five BCLCS columns.
		static final String POLYGON_PREFIX = "13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,";
		static final String[] DEFAULT_BCLCS = { "V", "T", "U", "TC", "SP" };

		static final int BCLCS_COUNT = 5;
		static final int NON_VEG_FIELD_COUNT = 9; // type, percent and pattern for each of three covers
		static final int LAND_COVER_FIELD_COUNT = 6; // class code and percent for each of three covers

		/**
		 * Joins <code>values</code> into exactly <code>count</code> CSV fields, writing missing or null values blank.
		 */
		private static String csvFields(String[] values, int count) {
			String[] fields = Arrays.copyOf(values == null ? new String[0] : values, count);
			for (int i = 0; i < fields.length; i++) {
				if (fields[i] == null) {
					fields[i] = "";
				}
			}
			return String.join(",", fields);
		}

		/**
		 * Builds one polygon row from the three groups under test, parses it through the CSV bean (which also runs bean
		 * validation), and returns the bean.
		 *
		 * @param bclcs           BCLCS_LEVEL1_CODE to BCLCS_LEVEL5_CODE
		 * @param nonVegFields    NON_VEG_COVER_TYPE_1 to NON_VEG_COVER_PATTERN_3, in column order
		 * @param landCoverFields LAND_COVER_CLASS_CD_1 to LAND_COVER_PCT_3, in column order
		 */
		private HcsvPolygonRecordBean parse(String[] bclcs, String[] nonVegFields, String[] landCoverFields) {
			String row = POLYGON_PREFIX + "," + csvFields(bclcs, BCLCS_COUNT) + ",2013,2013,60.0,"
					+ csvFields(nonVegFields, NON_VEG_FIELD_COUNT) + ","
					+ csvFields(landCoverFields, LAND_COVER_FIELD_COUNT);
			var beans = HcsvPolygonRecordBean.createHcsvPolygonStream(
					new ByteArrayInputStream( (POLYGON_CSV_HEADER_LINE + "\n" + row).getBytes())
			).parse();
			assertThat(beans.size(), is(1));
			return beans.get(0);
		}

		static Stream<Arguments> bclcsCases() {
			return Stream.of(
					Arguments.of(
							"all five levels supplied", new String[] { "N", "A", "B", "TM", "OP" },
							new String[] { "N", "A", "B", "TM", "OP" }
					),
					Arguments.of(
							"all five levels blank", new String[] { "", "", "", "", "" },
							new String[] { null, null, null, null, null }
					),
					Arguments.of(
							"only level 1", new String[] { "N", "", "", "", "" },
							new String[] { "N", null, null, null, null }
					),
					Arguments.of(
							"only level 2", new String[] { "", "A", "", "", "" },
							new String[] { null, "A", null, null, null }
					),
					Arguments.of(
							"only level 3", new String[] { "", "", "B", "", "" },
							new String[] { null, null, "B", null, null }
					),
					Arguments.of(
							"only level 4", new String[] { "", "", "", "TM", "" },
							new String[] { null, null, null, "TM", null }
					),
					Arguments.of(
							"only level 5", new String[] { "", "", "", "", "OP" },
							new String[] { null, null, null, null, "OP" }
					)
			);
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("bclcsCases")
		void testBclcsLevels(String description, String[] bclcsInput, String[] expectedCodes) {
			var bean = parse(bclcsInput, null, null);

			assertThat(
					Arrays.asList(
							bean.getBclcsLevel1Code(), bean.getBclcsLevel2Code(), bean.getBclcsLevel3Code(),
							bean.getBclcsLevel4Code(), bean.getBclcsLevel5Code()
					), is(Arrays.asList(expectedCodes))
			);
		}

		static Stream<Arguments> nonVegCoverCases() {
			// Arguments: description, CSV fields (type1, pct1, pattern1, type2, pct2, pattern2, type3, pct3, pattern3),
			// expected types, expected percents, expected patterns.
			return Stream.of(
					Arguments.of(
							"all three covers supplied",
							new String[] { "LA", "10", "PA", "ES", "20", "PB", "BU", "30", "PC" },
							new String[] { "LA", "ES", "BU" }, new Integer[] { 10, 20, 30 },
							new String[] { "PA", "PB", "PC" }
					),
					Arguments.of(
							"all blank", new String[] {}, new String[] { null, null, null },
							new Integer[] { null, null, null }, new String[] { null, null, null }
					),
					Arguments.of(
							"only percent 1", new String[] { null, "40" }, new String[] { null, null, null },
							new Integer[] { 40, null, null }, new String[] { null, null, null }
					),
					// Regression: getNonVegCoverPercent2() once returned the value of percent 1.
					Arguments.of(
							"only percent 2", new String[] { null, null, null, null, "40" },
							new String[] { null, null, null }, new Integer[] { null, 40, null },
							new String[] { null, null, null }
					),
					Arguments.of(
							"only percent 3", new String[] { null, null, null, null, null, null, null, "40" },
							new String[] { null, null, null }, new Integer[] { null, null, 40 },
							new String[] { null, null, null }
					),
					Arguments.of(
							"percent 1 supplied and percent 2 blank",
							new String[] { "LA", "10", "PA", "ES", null, "PB" }, new String[] { "LA", "ES", null },
							new Integer[] { 10, null, null }, new String[] { "PA", "PB", null }
					),
					Arguments.of(
							"percent 2 supplied and percent 1 blank",
							new String[] { "LA", null, "PA", "ES", "20", "PB" }, new String[] { "LA", "ES", null },
							new Integer[] { null, 20, null }, new String[] { "PA", "PB", null }
					),
					Arguments.of(
							"first cover omitted",
							new String[] { null, null, null, "ES", "20", "PB", "BU", "30", "PC" },
							new String[] { null, "ES", "BU" }, new Integer[] { null, 20, 30 },
							new String[] { null, "PB", "PC" }
					),
					Arguments.of(
							"patterns only", new String[] { null, null, "PA", null, null, "PB", null, null, "PC" },
							new String[] { null, null, null }, new Integer[] { null, null, null },
							new String[] { "PA", "PB", "PC" }
					)
			);
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("nonVegCoverCases")
		void testNonVegCover(
				String description, String[] nonVegInput, String[] expectedTypes, Integer[] expectedPercents,
				String[] expectedPatterns
		) {
			var bean = parse(DEFAULT_BCLCS, nonVegInput, null);

			assertThat(
					Arrays.asList(bean.getNonVegCoverType1(), bean.getNonVegCoverType2(), bean.getNonVegCoverType3()),
					is(Arrays.asList(expectedTypes))
			);
			assertThat(
					Arrays.asList(
							bean.getNonVegCoverPercent1(), bean.getNonVegCoverPercent2(), bean.getNonVegCoverPercent3()
					), is(Arrays.asList(expectedPercents))
			);
			assertThat(
					Arrays.asList(
							bean.getNonVegCoverPattern1(), bean.getNonVegCoverPattern2(), bean.getNonVegCoverPattern3()
					), is(Arrays.asList(expectedPatterns))
			);

			// The combined details must pair each supplied type with its own percent and pattern, and omit the rest.
			var details = bean.getNonVegCoverDetails();
			int suppliedTypes = 0;
			for (int i = 0; i < expectedTypes.length; i++) {
				if (expectedTypes[i] == null) {
					continue;
				}
				suppliedTypes++;
				var detail = details.get(NonVegetationTypeCode.fromCode(expectedTypes[i]));
				assertThat("details for cover " + (i + 1), detail.nonVegCoverPercent(), is(expectedPercents[i]));
				assertThat("details for cover " + (i + 1), detail.getNonVegCoverPattern(), is(expectedPatterns[i]));
			}
			assertThat(details.size(), is(suppliedTypes));
		}

		static Stream<Arguments> landCoverCases() {
			// Arguments: description, CSV fields (code1, pct1, code2, pct2, code3, pct3), expected codes, expected
			// percents.
			return Stream.of(
					Arguments.of(
							"all three land covers supplied", new String[] { "TC", "11", "BL", "22", "HE", "33" },
							new String[] { "TC", "BL", "HE" }, new Integer[] { 11, 22, 33 }
					),
					Arguments.of(
							"all blank", new String[] {}, new String[] { null, null, null },
							new Integer[] { null, null, null }
					),
					Arguments.of(
							"only percent 1", new String[] { null, "40" }, new String[] { null, null, null },
							new Integer[] { 40, null, null }
					),
					Arguments.of(
							"only percent 2", new String[] { null, null, null, "40" },
							new String[] { null, null, null }, new Integer[] { null, 40, null }
					),
					Arguments.of(
							"only percent 3", new String[] { null, null, null, null, null, "40" },
							new String[] { null, null, null }, new Integer[] { null, null, 40 }
					),
					Arguments.of(
							"only class code 2", new String[] { null, null, "BL" }, new String[] { null, "BL", null },
							new Integer[] { null, null, null }
					),
					Arguments.of(
							"second land cover omitted", new String[] { "TC", "11", null, null, "HE", "33" },
							new String[] { "TC", null, "HE" }, new Integer[] { 11, null, 33 }
					)
			);
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("landCoverCases")
		void testLandCover(
				String description, String[] landCoverInput, String[] expectedCodes, Integer[] expectedPercents
		) {
			var bean = parse(DEFAULT_BCLCS, null, landCoverInput);

			assertThat(
					Arrays.asList(
							bean.getLandCoverClassCode1(), bean.getLandCoverClassCode2(), bean.getLandCoverClassCode3()
					), is(Arrays.asList(expectedCodes))
			);
			assertThat(
					Arrays.asList(
							bean.getLandCoverPercent1(), bean.getLandCoverPercent2(), bean.getLandCoverPercent3()
					), is(Arrays.asList(expectedPercents))
			);
		}
	}

	@Test
	void testNoFeatureIdGracefulSkip() throws AbstractProjectionRequestException {
		String hcsvLayerFileContents = layerFileHeader + defaultLayerPrefix.replaceAll("13919428", "") + // NO feature
																											// ID
				"1,P,,1,,,,5,1.000050,150,PLI,60.00,PLI,40.0,,,,,,,,,60,9.00,,,,,,,,,,\n" + defaultLayerPrefix
				+ "1,P,,1,,,,5,1.000050,150,PLI,100.00,,,,,,,,,,,60,9.00,,,,,,,,,,\n"; // Same record Feature ID

		HcsvPolygonStream unit = new HcsvPolygonStream(
				context, new ByteArrayInputStream(hcsvPolygonFileContents.getBytes()),
				new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
		);

		Polygon poly = unit.getNextPolygon();
		assertThat(poly.getLayers().size(), is(1));
	}

	@Test
	void testSixSpeciesStream() throws AbstractProjectionRequestException {
		String hcsvLayerFileContents = layerFileHeader + defaultLayerPrefix
				+ "1,P,,1,,,,5,1.000050,150,PLI,40.00,FD,28.0,CW,12.0,EA,10.0,H,5.0,AC,5.0,60,9.00,,,,,,,,,,\n"; // 5
																													// species
																													// defined

		HcsvPolygonStream unit = new HcsvPolygonStream(
				context, new ByteArrayInputStream(hcsvPolygonFileContents.getBytes()),
				new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
		);

		Polygon poly = unit.getNextPolygon();

		assertThat(poly.getMessages().size(), is(0));
		assertThat(poly.getRank1Layer().getSiteSpecies().size(), is(6));
	}

	static Stream<Arguments> nonForestDescriptorValues() {
		return Stream.of(
				Arguments.of("I", "NSR", true), Arguments.of("F", "NC", true), Arguments.of("F", "", false),
				Arguments.of("V", "NSR", true), Arguments.of("V", "NC", false), Arguments.of("V", "", false)
		);

	}

	@ParameterizedTest
	@MethodSource("nonForestDescriptorValues")
	void testNonForestDescriptorSetsSuppressHAYFields(
			String inventoryStandard, String nonForestDescriptor, boolean suppress
	) throws PolygonValidationException {
		String myHcsvPolygonFileContents = // Inventory standard = Silviculture
				POLYGON_CSV_HEADER_LINE + "\n" + "13919428,093C090,94833422,DQU,UNK,UNK," + inventoryStandard
						+ ",UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,";
		String hcsvLayerFileContents = layerFileHeader + defaultLayerPrefix + "1,P,,," + nonForestDescriptor
				+ ",,,5,1.000050,150,,,,,,,,,,,,,,,,,,,,,,,,\n"; // Has non forest descriptor

		HcsvPolygonStream unit = new HcsvPolygonStream(
				context, new ByteArrayInputStream(myHcsvPolygonFileContents.getBytes()),
				new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
		);

		Polygon poly = unit.getNextPolygon();
		if ("I".equals(inventoryStandard) || "F".equals(inventoryStandard)) {
			assertThat(poly.getInventoryStandard(), is(InventoryStandard.FIP));
		}
		assertThat(poly.getLayers().get("1").getDoSuppressPerHAYields(), is(suppress));

	}

	@Test
	void testLayerInferVDYPTypeFromLayerID() throws PolygonValidationException {
		String myHcsvPolygonFileContents = // Inventory standard = FIP
				POLYGON_CSV_HEADER_LINE + "\n"
						+ "13919428,093C090,94833422,DQU,UNK,UNK,F,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,";
		String hcsvLayerFileContents = layerFileHeader + defaultLayerPrefix
				+ "P,,,,,,,5,1.000050,150,PLI,100.0,,,,,,,,,,,,,,,,,,,,,,\n"; // Layer Id is a VDYP type Code

		HcsvPolygonStream unit = new HcsvPolygonStream(
				context, new ByteArrayInputStream(myHcsvPolygonFileContents.getBytes()),
				new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
		);

		Polygon poly = unit.getNextPolygon();
		assertThat(poly.getLayers().get("P").getVdyp7LayerCode(), is(ProjectionTypeCode.PRIMARY));
	}

	static Stream<Arguments> BecZoneReplacements() {
		return Stream.of(
				Arguments.of("BAFA", "AT"), Arguments.of("CMA", "AT"), Arguments.of("IMA", "AT"),
				Arguments.of("MS", "MS")
		);
	}

	@ParameterizedTest
	@MethodSource("BecZoneReplacements")
	void testSubstituteATForUnusedBECZones(String input, String output) throws PolygonValidationException {
		String myHcsvPolygonFileContents = POLYGON_CSV_HEADER_LINE + "\n"
				+ "13919428,093C090,94833422,DQU,UNK,UNK,F,UNK,0.6,10,3,HE,35,8,," + input
				+ ",14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,";
		String hcsvLayerFileContents = layerFileHeader + defaultLayerPrefix
				+ "1,P,,,,,,5,1.000050,150,PLI,100.0,,,,,,,,,,,,,,,,,,,,,,\n"; // normal

		HcsvPolygonStream unit = new HcsvPolygonStream(
				context, new ByteArrayInputStream(myHcsvPolygonFileContents.getBytes()),
				new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
		);

		Polygon poly = unit.getNextPolygon();
		assertThat(poly.getBecZone(), is(output));
	}

	@Test
	void testExtendedColumnsWithValidSiteIndexSpp2ParsedWithoutErrors() throws PolygonValidationException {
		// Provide EST_SITE_INDEX_SPP2=22.5 for FD (species 2) via the extended header columns.
		// pos4-50 (47 fields): standard fields + 13 new optional fields; pos46=22.5
		String hcsvLayerFileContents = EXTENDED_LAYER_CSV_HEADER_LINE + "\n" + defaultLayerPrefix
				+ "1,P,,,,,,5,1.0,150,PLI,60.0,FD,40.0,,,,,,,,,60,9.0,50,8.5,,,,,,,,,,,,,,,,,22.5,,,,\n";

		HcsvPolygonStream unit = new HcsvPolygonStream(
				context, new ByteArrayInputStream(hcsvPolygonFileContents.getBytes()),
				new ByteArrayInputStream(hcsvLayerFileContents.getBytes())
		);

		Polygon poly = unit.getNextPolygon();
		assertThat(poly.getMessages().size(), is(0));
	}

}
