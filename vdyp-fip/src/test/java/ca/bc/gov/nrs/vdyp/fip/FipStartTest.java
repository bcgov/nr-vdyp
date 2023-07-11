package ca.bc.gov.nrs.vdyp.fip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.fip.model.FipLayer;
import ca.bc.gov.nrs.vdyp.fip.model.FipMode;
import ca.bc.gov.nrs.vdyp.fip.model.FipPolygon;
import ca.bc.gov.nrs.vdyp.fip.model.FipSpecies;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.Layer;

public class FipStartTest {

	@Test
	public void testProcessEmpty() throws Exception {

		var app = new FipStart();

		var controlMap = new HashMap<String, Object>();

		var control = EasyMock.createControl();

		StreamingParser<FipPolygon> polygonStream = mockStream(
				control, controlMap, FipPolygonParser.CONTROL_KEY, "polygonStream"
		);
		StreamingParser<Map<Layer, FipPolygon>> layerStream = mockStream(
				control, controlMap, FipLayerParser.CONTROL_KEY, "layerStream"
		);
		StreamingParser<Collection<FipSpecies>> speciesStream = mockStream(
				control, controlMap, FipSpeciesParser.CONTROL_KEY, "speciesStream"
		);

		expectAllEmpty(polygonStream, layerStream, speciesStream);

		expectAllClosed(polygonStream, layerStream, speciesStream);

		app.setControlMap(controlMap);

		control.replay();

		app.process();

		control.verify();
	}

	@Test
	public void testProcessSimple() throws Exception {

		var app = new FipStart();

		var controlMap = new HashMap<String, Object>();

		var control = EasyMock.createControl();

		StreamingParser<FipPolygon> polygonStream = mockStream(
				control, controlMap, FipPolygonParser.CONTROL_KEY, "polygonStream"
		);
		StreamingParser<Map<Layer, FipLayer>> layerStream = mockStream(
				control, controlMap, FipLayerParser.CONTROL_KEY, "layerStream"
		);
		StreamingParser<Collection<FipSpecies>> speciesStream = mockStream(
				control, controlMap, FipSpeciesParser.CONTROL_KEY, "speciesStream"
		);

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		// One polygon with one layer with one species entry

		mockWith(
				polygonStream,
				new FipPolygon(
						polygonId, "0", "BG", java.util.Optional.empty(), Optional.of(FipMode.FIPSTART),
						java.util.Optional.empty(), 1.0f
				)
		);
		mockWith(
				layerStream,
				Collections.singletonMap(
						layer,
						new FipLayer(
								polygonId, layer, 0, 0, 0, 0, polygonId, polygonId, 0, java.util.Optional.empty(),
								java.util.Optional.empty()
						)
				)
		);
		mockWith(
				speciesStream,
				Collections.singletonList(new FipSpecies(polygonId, layer, "B", 100.0f, Collections.emptyMap()))
		);

		expectAllClosed(polygonStream, layerStream, speciesStream);

		app.setControlMap(controlMap);

		control.replay();

		app.process();

		control.verify();
	}

	@Test
	public void testPolygonWithNoLayers() throws Exception {

		var app = new FipStart();

		var controlMap = new HashMap<String, Object>();

		var control = EasyMock.createControl();

		StreamingParser<FipPolygon> polygonStream = mockStream(
				control, controlMap, FipPolygonParser.CONTROL_KEY, "polygonStream"
		);
		StreamingParser<Map<Layer, FipLayer>> layerStream = mockStream(
				control, controlMap, FipLayerParser.CONTROL_KEY, "layerStream"
		);
		StreamingParser<Collection<FipSpecies>> speciesStream = mockStream(
				control, controlMap, FipSpeciesParser.CONTROL_KEY, "speciesStream"
		);

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		// One polygon with one layer with one species entry

		mockWith(
				polygonStream,
				new FipPolygon(
						polygonId, "0", "BG", java.util.Optional.empty(), Optional.of(FipMode.FIPSTART),
						java.util.Optional.empty(), 1.0f
				)
		);
		EasyMock.expect(layerStream.next()).andThrow(new IllegalStateException());

		expectAllClosed(polygonStream, layerStream, speciesStream);

		app.setControlMap(controlMap);

		control.replay();

		var ex = assertThrows(ProcessingException.class, () -> app.process());

		assertThat(ex, hasProperty("message", is("Layers file has fewer records than polygon file.")));

		control.verify();
	}

	private static <T> StreamingParser<T>
			mockStream(IMocksControl control, Map<String, Object> controlMap, String key, String name)
					throws IOException {
		StreamingParserFactory<T> streamFactory = control.mock(name + "Factory", StreamingParserFactory.class);
		StreamingParser<T> stream = control.mock(name, StreamingParser.class);

		EasyMock.expect(streamFactory.get()).andReturn(stream);

		controlMap.put(key, streamFactory);
		return stream;
	}

	private static void expectAllClosed(AutoCloseable... toClose) throws Exception {
		for (var x : toClose) {
			x.close();
			EasyMock.expectLastCall();
		}
	}

	private static void expectAllEmpty(StreamingParser<?>... toBeEmpty) throws Exception {
		for (var x : toBeEmpty) {
			EasyMock.expect(x.hasNext()).andStubReturn(false);
		}
	}

	private static <T> void mockWith(StreamingParser<T> stream, List<T> results)
			throws IOException, ResourceParseException {
		var it = results.iterator();
		EasyMock.expect(stream.hasNext()).andStubAnswer(it::hasNext);
		EasyMock.expect(stream.next()).andAnswer(it::next).times(results.size());
	}

	@SafeVarargs
	private static <T> void mockWith(StreamingParser<T> stream, T... results)
			throws IOException, ResourceParseException {
		mockWith(stream, Arrays.asList(results));
	}

	private String polygonId(String prefix, int year) {
		return String.format("%-23s%4d", prefix, year);
	}
}
