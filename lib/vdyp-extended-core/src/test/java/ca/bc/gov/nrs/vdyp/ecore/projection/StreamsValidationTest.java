package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.api.helpers.TestHelper;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;

class StreamsValidationTest {

	@Test
	void testValidHcsvInputStreams() throws AbstractProjectionRequestException {

		Parameters p = TestHelper.buildValidParametersObject();
		ProjectionContext s = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);

		Map<String, InputStream> streams = new HashMap<>();
		streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

		AbstractPolygonStream.build(s, streams);
	}

	@Test
	void testMissingHcsvInputStreams() {

		ProjectionContext s = null;
		try {
			Parameters p = TestHelper.buildValidParametersObject();
			s = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
		} catch (AbstractProjectionRequestException e) {
			fail();
		}

		Map<String, InputStream> streams = new HashMap<>();
		streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

		try {
			AbstractPolygonStream.build(s, streams);
			fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper
					.verifyMessageSetIs(e.getValidationMessages(), ValidationMessageKind.EXPECTED_STREAMS_NOT_SUPPLIED);

			var message = e.getValidationMessages().get(0).getMessage();
			assertThat(message, Matchers.endsWith(ParameterNames.HCSV_LAYERS_INPUT_DATA));
		}
	}

	@Test
	void testTooManyHcsvInputStreams() {

		Parameters p = TestHelper.buildValidParametersObject();

		Map<String, InputStream> streams = new HashMap<>();
		streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.SCSV_HISTORY_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

		try {
			ProjectionContext s = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			AbstractPolygonStream.build(s, streams);
			fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(e.getValidationMessages(), ValidationMessageKind.UNEXPECTED_STREAMS_SUPPLIED);

			var message = e.getValidationMessages().get(0).getMessage();
			assertThat(message, Matchers.endsWith(ParameterNames.SCSV_HISTORY_INPUT_DATA));
		}
	}

	@Test
	void testDifferentHcsvInputStreams() {

		Parameters p = TestHelper.buildValidParametersObject();

		Map<String, InputStream> streams = new HashMap<>();
		streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.SCSV_HISTORY_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

		try {
			ProjectionContext s = new ProjectionContext(ProjectionRequestKind.HCSV, "id", p, false);
			AbstractPolygonStream.build(s, streams);
			fail();
		} catch (AbstractProjectionRequestException e) {
			TestHelper.verifyMessageSetIs(
					e.getValidationMessages(), ValidationMessageKind.UNEXPECTED_STREAMS_SUPPLIED,
					ValidationMessageKind.EXPECTED_STREAMS_NOT_SUPPLIED
			);
			assertThat(
					e.getValidationMessages(),
					Matchers.containsInAnyOrder(
							new ValidationMessage(
									ValidationMessageKind.EXPECTED_STREAMS_NOT_SUPPLIED,
									ParameterNames.HCSV_LAYERS_INPUT_DATA
							),
							new ValidationMessage(
									ValidationMessageKind.UNEXPECTED_STREAMS_SUPPLIED,
									ParameterNames.SCSV_HISTORY_INPUT_DATA
							)
					)
			);
		}
	}

	@Test
	void testValidDcsvInputStreams() {

		Parameters p = TestHelper.buildValidParametersObject();

		Map<String, InputStream> streams = new HashMap<>();
		streams.put(ParameterNames.DCSV_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

		try {
			ProjectionContext s = new ProjectionContext(ProjectionRequestKind.DCSV, "id", p, false);
			AbstractPolygonStream.build(s, streams);
			fail();
		} catch (AbstractProjectionRequestException e) {
			assertThat(e, Matchers.notNullValue());
		}
	}

	@Test
	void testValidScsvInputStreams() {

		Parameters p = TestHelper.buildValidParametersObject();

		Map<String, InputStream> streams = new HashMap<>();
		streams.put(ParameterNames.SCSV_POLYGON_ID_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.SCSV_HISTORY_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.SCSV_LAYERS_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.SCSV_NON_VEGETATION_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.SCSV_OTHER_VEGETATION_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.SCSV_POLYGON_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.SCSV_SPECIES_INPUT_DATA, new ByteArrayInputStream(new byte[0]));
		streams.put(ParameterNames.SCSV_VRI_ADJUST_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

		try {
			ProjectionContext s = new ProjectionContext(ProjectionRequestKind.SCSV, "id", p, false);
			AbstractPolygonStream.build(s, streams);
		} catch (AbstractProjectionRequestException e) {
			fail();
		}
	}

	@Test
	void testValidIcsvInputStreams() {

		Parameters p = TestHelper.buildValidParametersObject();

		Map<String, InputStream> streams = new HashMap<>();
		streams.put(ParameterNames.ICSV_INPUT_DATA, new ByteArrayInputStream(new byte[0]));

		try {
			ProjectionContext s = new ProjectionContext(ProjectionRequestKind.ICSV, "id", p, false);
			AbstractPolygonStream.build(s, streams);
		} catch (AbstractProjectionRequestException e) {
			fail();
		}
	}
}
