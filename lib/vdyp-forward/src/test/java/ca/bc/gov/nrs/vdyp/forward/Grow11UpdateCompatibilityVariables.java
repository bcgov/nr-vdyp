package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.ExecutionStep;
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.processing_state.LayerProcessingState;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class Grow11UpdateCompatibilityVariables {

	protected static final Logger logger = LoggerFactory.getLogger(Grow11UpdateCompatibilityVariables.class);

	protected static ProcessingControlParser parser;
	protected static Map<String, Object> controlMap;

	protected static StreamingParserFactory<PolygonIdentifier> polygonDescriptionStreamFactory;
	protected static StreamingParser<PolygonIdentifier> polygonDescriptionStream;

	protected static ForwardDataStreamReader forwardDataStreamReader;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void beforeTest() throws IOException, ResourceParseException, ProcessingException {
		parser = new ProcessingControlParser();
		controlMap = ForwardTestUtils.parse(parser, "VDYP.CTR");

		polygonDescriptionStreamFactory = (StreamingParserFactory<PolygonIdentifier>) controlMap
				.get(ControlKey.FORWARD_INPUT_GROWTO.name());
		polygonDescriptionStream = polygonDescriptionStreamFactory.get();

		forwardDataStreamReader = new ForwardDataStreamReader(controlMap);
	}

	@AfterEach
	void afterTest() throws ProcessingException {
		forwardDataStreamReader.close();
	}

	@Test
	void testStandardPath() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.processPolygon(polygon, ExecutionStep.GROW_10_COMPATIBILITY_VARS);

		// VDYP7 reports [], -9, -9, 35.473381, -9, -9)
		LayerProcessingState<ForwardLayerProcessingState> lps = fpe.fps.getPrimaryLayerProcessingState();
		assertThat(
				// VDYP7 reports BASAL_AREA = -2.13947629e-07, all others 0.0
				lps.getCvPrimaryLayerSmall()[1],
				allOf(
						hasEntry(is(UtilizationClassVariable.BASAL_AREA), closeTo(-2.1394816e-07f)),
						hasEntry(is(UtilizationClassVariable.QUAD_MEAN_DIAMETER), is(0.0f)),
						hasEntry(is(UtilizationClassVariable.LOREY_HEIGHT), is(0.0f)),
						hasEntry(is(UtilizationClassVariable.WHOLE_STEM_VOLUME), is(0.0f))
				)
		);
		assertThat(
				// VDYP7 reports BASAL_AREA = -4.49605286e-05, QUAD_MEAN_DIAMETER = 0.00236749649
				// LOREY_HEIGHT = 1.19209221e-06, WHOLE_STEM_VOLUME = 0.00102931913
				lps.getCvPrimaryLayerSmall()[2],
				allOf(
						hasEntry(is(UtilizationClassVariable.BASAL_AREA), closeTo(-4.406223e-5f)),
						hasEntry(is(UtilizationClassVariable.QUAD_MEAN_DIAMETER), closeTo(0.0023196794f)),
						hasEntry(is(UtilizationClassVariable.LOREY_HEIGHT), closeTo(1.2850753e-6f)),
						hasEntry(is(UtilizationClassVariable.WHOLE_STEM_VOLUME), closeTo(0.0010083826f))
				)
		);
		assertThat(
				// VDYP7 reports BASAL_AREA = 4.94660344e-6, QUAD_MEAN_DIAMETER = 0.0
				// LOREY_HEIGHT = -1.55569342e-5, WHOLE_STEM_VOLUME = 0.0
				lps.getCvPrimaryLayerSmall()[3],
				allOf(
						hasEntry(is(UtilizationClassVariable.BASAL_AREA), closeTo(4.8476713e-6f)),
						hasEntry(is(UtilizationClassVariable.QUAD_MEAN_DIAMETER), is(0.0f)),
						hasEntry(is(UtilizationClassVariable.LOREY_HEIGHT), closeTo(-1.5245796e-5f)),
						hasEntry(is(UtilizationClassVariable.WHOLE_STEM_VOLUME), is(0.0f))
				)
		);
		assertThat(
				// VDYP7 reports 0.0 for all
				lps.getCvPrimaryLayerSmall()[4],
				allOf(
						hasEntry(is(UtilizationClassVariable.BASAL_AREA), is(0.0f)),
						hasEntry(is(UtilizationClassVariable.QUAD_MEAN_DIAMETER), is(0.0f)),
						hasEntry(is(UtilizationClassVariable.LOREY_HEIGHT), is(0.0f)),
						hasEntry(is(UtilizationClassVariable.WHOLE_STEM_VOLUME), is(0.0f))
				)
		);
		assertThat(
				// VDYP7 reports BASAL_AREA = 3.42086423e-06, LOREY_HEIGHT = -5.7758567e-5, 0.0 for all others
				lps.getCvPrimaryLayerSmall()[5],
				allOf(
						hasEntry(is(UtilizationClassVariable.BASAL_AREA), closeTo(3.352447e-6f)),
						hasEntry(is(UtilizationClassVariable.QUAD_MEAN_DIAMETER), is(0.0f)),
						hasEntry(is(UtilizationClassVariable.LOREY_HEIGHT), closeTo(-5.6603396e-5f)),
						hasEntry(is(UtilizationClassVariable.WHOLE_STEM_VOLUME), is(0.0f))
				)
		);
	}
}
