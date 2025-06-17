package ca.bc.gov.nrs.vdyp.backend.projection.input;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.LAYER_CSV_HEADER_LINE;
import static ca.bc.gov.nrs.vdyp.test.TestUtils.POLYGON_CSV_HEADER_LINE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

class HcsvPolygonStreamTest {
	Parameters p;
	ProjectionContext context;
	final String hcsvPolygonFileContents = POLYGON_CSV_HEADER_LINE + "\n"
			+ "13919428,093C090,94833422,DQU,UNK,UNK,V,UNK,0.6,10,3,HE,35,8,,MS,14,50.0,1.000,,V,T,U,TC,SP,2013,2013,60.0,,,,,,,,,,TC,100,,,,";
	String layerFileHeader = LAYER_CSV_HEADER_LINE + "\n";
	String defaultLayerPrefix = "13919428,14321067,093C090,94833422,";

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

			assertThrows(PolygonValidationException.class, () -> unit.getNextPolygon());
		}

		static Stream<String> invalidLayerData() {
			return Stream.of(
					"1,P,,1,,,,5,1.000050,150,PLI,60.00,PLI,40.0,,,,,,,,,60,9.00,50,8.0,,,,,,,,", // Duplicate Species
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

			assertThrows(PolygonValidationException.class, () -> unit.getNextPolygon());
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

			// No Polygon because polygon was invalid
			assertThrows(IllegalStateException.class, () -> unit.getNextPolygon());
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

			// No Polygon because polygon was invalid
			assertThrows(IllegalStateException.class, () -> unit.getNextPolygon());
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

			// No Polygon because polygon was invalid
			assertThrows(IllegalStateException.class, () -> unit.getNextPolygon());
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

}
