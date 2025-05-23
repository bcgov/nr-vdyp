package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.ExecutionStep;

class ProcessPolygonBasicTest extends AbstractForwardProcessingEngineTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ProcessPolygonBasicTest.class);

	@Test
	void testOnePolygon() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		assertThat(fpe.fps.fcm.getBecLookup(), notNullValue());
		assertThat(fpe.fps.fcm.getGenusDefinitionMap(), notNullValue());
		assertThat(fpe.fps.fcm.getSiteCurveMap(), notNullValue());

		int nPolygonsProcessed = 0;
		var polygon = forwardDataStreamReader.readNextPolygon();

		if (polygon.isPresent()) {
			fpe.processPolygon(polygon.get(), ExecutionStep.GROW);
			nPolygonsProcessed += 1;
		}

		assertEquals(1, nPolygonsProcessed);
	}
}
