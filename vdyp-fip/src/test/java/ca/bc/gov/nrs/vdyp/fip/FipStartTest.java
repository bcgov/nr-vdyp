package ca.bc.gov.nrs.vdyp.fip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import ca.bc.gov.nrs.vdyp.io.parse.MockStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.Layer;

public class FipStartTest {

	@Test
	public void testProcessEmpty() throws Exception {

		testWith(Arrays.asList(), Arrays.asList(), Arrays.asList(), app -> {
			assertDoesNotThrow(app::process);
		});
	}

	@Test
	public void testProcessSimple() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.PRIMARY;

		// One polygon with one primary layer with one species entry

		testWith(
				Arrays.asList(
						new FipPolygon(
								polygonId, "0", "BG", java.util.Optional.empty(), Optional.of(FipMode.FIPSTART),
								java.util.Optional.empty(), 1.0f
						)
				),
				Arrays.asList(
						Collections.singletonMap(
								layer,
								new FipLayer(
										polygonId, layer, 0, 20f, 0.9f, 0, "B", "B", 0, java.util.Optional.empty(),
										java.util.Optional.empty()
								)
						)
				),
				Arrays.asList(
						Collections.singletonList(new FipSpecies(polygonId, layer, "B", 100.0f, Collections.emptyMap()))
				), app -> {
					assertDoesNotThrow(app::process);
				}
		);

	}

	@Test
	public void testPolygonWithNoLayersRecord() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);

		testWith(
				Arrays.asList(
						new FipPolygon(
								polygonId, "0", "BG", java.util.Optional.empty(), Optional.of(FipMode.FIPSTART),
								java.util.Optional.empty(), 1.0f
						)
				), Collections.emptyList(), Collections.emptyList(), app -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(ex, hasProperty("message", is("Layers file has fewer records than polygon file.")));

				}
		);
	}

	@Test
	public void testPolygonWithNoPrimaryLayer() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.VETERAN;

		// One polygon with one layer with one species entry, and type is VETERAN
		testWith(
				Arrays.asList(
						new FipPolygon(
								polygonId, "0", "BG", java.util.Optional.empty(), Optional.of(FipMode.FIPSTART),
								java.util.Optional.empty(), 1.0f
						)
				),
				Arrays.asList(
						Collections.singletonMap(
								layer,
								new FipLayer(
										polygonId, layer, 0, 20f, 0.9f, 0, "B", "B", 0, java.util.Optional.empty(),
										java.util.Optional.empty()
								)
						)
				),
				Arrays.asList(
						Collections.singletonList(new FipSpecies(polygonId, layer, "B", 100.0f, Collections.emptyMap()))
				), app -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId + " has no " + Layer.PRIMARY
													+ " layer, or that layer has non-positive height or crown closure."
									)
							)
					);

				}
		);

	}

	@Test
	public void testPrimaryLayerTotalAgeLessThanYearsToBreastHeight() throws Exception {

		var polygonId = polygonId("Test Polygon", 2023);
		var layer = Layer.VETERAN;

		// One polygon with one layer with one species entry, and type is VETERAN

		testWith(
				Arrays.asList(
						new FipPolygon(
								polygonId, "0", "BG", java.util.Optional.empty(), Optional.of(FipMode.FIPSTART),
								java.util.Optional.empty(), 1.0f
						)
				),
				Arrays.asList(
						Collections.singletonMap(
								layer,
								new FipLayer(
										polygonId, layer, 0, 20f, 0.9f, 5.0f, "B", "B", 0, java.util.Optional.empty(),
										java.util.Optional.empty()
								)
						)
				),
				Arrays.asList(
						Collections.singletonList(new FipSpecies(polygonId, layer, "B", 100.0f, Collections.emptyMap()))
				), app -> {
					var ex = assertThrows(ProcessingException.class, () -> app.process());

					assertThat(
							ex,
							hasProperty(
									"message",
									is(
											"Polygon " + polygonId + " has no " + Layer.PRIMARY
													+ " layer, or that layer has non-positive height or crown closure."
									)
							)
					);
				}
		);

	}

	private static <T> MockStreamingParser<T>
			mockStream(IMocksControl control, Map<String, Object> controlMap, String key, String name)
					throws IOException {
		StreamingParserFactory<T> streamFactory = control.mock(name + "Factory", StreamingParserFactory.class);
		MockStreamingParser<T> stream = new MockStreamingParser<>();

		EasyMock.expect(streamFactory.get()).andReturn(stream);

		controlMap.put(key, streamFactory);
		return stream;
	}

	private static void expectAllClosed(MockStreamingParser<?>... toClose) throws Exception {
		for (var x : toClose) {
			x.expectClosed();
		}
	}

	private static <T> void mockWith(MockStreamingParser<T> stream, List<T> results)
			throws IOException, ResourceParseException {
		stream.addValues(results);
	}

	@SuppressWarnings("unused")
	@SafeVarargs
	private static <T> void mockWith(MockStreamingParser<T> stream, T... results)
			throws IOException, ResourceParseException {
		stream.addValues(results);
	}

	private String polygonId(String prefix, int year) {
		return String.format("%-23s%4d", prefix, year);
	}

	private static final void testWith(
			List<FipPolygon> polygons, List<Map<Layer, FipLayer>> layers, List<Collection<FipSpecies>> species,
			TestConsumer<FipStart> test
	) throws Exception {

		var app = new FipStart();

		var controlMap = new HashMap<String, Object>();

		var control = EasyMock.createControl();

		MockStreamingParser<FipPolygon> polygonStream = mockStream(
				control, controlMap, FipPolygonParser.CONTROL_KEY, "polygonStream"
		);
		MockStreamingParser<Map<Layer, FipLayer>> layerStream = mockStream(
				control, controlMap, FipLayerParser.CONTROL_KEY, "layerStream"
		);
		MockStreamingParser<Collection<FipSpecies>> speciesStream = mockStream(
				control, controlMap, FipSpeciesParser.CONTROL_KEY, "speciesStream"
		);

		mockWith(polygonStream, polygons);
		mockWith(layerStream, layers);
		mockWith(speciesStream, species);

		app.setControlMap(controlMap);

		control.replay();

		test.accept(app);

		control.verify();

		expectAllClosed(polygonStream, layerStream, speciesStream);

	}

	@FunctionalInterface
	private static interface TestConsumer<T> {
		public void accept(T unit) throws Exception;
	}
}
