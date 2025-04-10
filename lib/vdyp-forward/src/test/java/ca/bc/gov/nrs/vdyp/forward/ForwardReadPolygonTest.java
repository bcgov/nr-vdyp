package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

@SuppressWarnings({ "unchecked" })
class ForwardReadPolygonTest {

	@Test
	void testReadPolygons() throws Exception {

		var parser = new ForwardControlParser();
		Map<String, Object> controlMap = parse(parser, "VDYP.CTR");

		try {
			var polygonDescriptionStreamFactory = controlMap.get(ControlKey.FORWARD_INPUT_GROWTO.name());
			var polygonDescriptionStream = ((StreamingParserFactory<PolygonIdentifier>) polygonDescriptionStreamFactory)
					.get();

			ForwardDataStreamReader reader = new ForwardDataStreamReader(controlMap);

			// Fetch the next polygon to process.
			List<VdypPolygon> polygons = new ArrayList<>();

			while (polygonDescriptionStream.hasNext()) {

				var polygon = reader.readNextPolygon();
				if (polygon.isPresent()) {
					polygons.add(polygon.get());
				} else {
					break;
				}
			}

			assertThat(polygons, Matchers.hasSize(10));

			var polygon = polygons.get(0);

			assertThat(polygon.getPolygonIdentifier().toStringCompact(), is("01002 S000001 00(1970)"));

			for (VdypLayer layer : polygon.getLayers().values()) {
				assertThat(layer.getPolygonIdentifier().getName(), is(polygon.getPolygonIdentifier().getName()));
				assertThat(layer.getPolygonIdentifier().getYear(), is(polygon.getPolygonIdentifier().getYear()));

				assertThat(UtilizationClass.values().length, is(layer.getBaseAreaByUtilization().size()));

				var speciesMap = layer.getSpecies();
				for (VdypSpecies species : speciesMap.values()) {
					assertThat(species, hasProperty("layerType", is(layer.getLayerType())));
					assertThat(species, hasProperty("polygonIdentifier", is(polygon.getPolygonIdentifier())));

					species.getSite().ifPresent(site -> {
						assertThat(
								site,
								allOf(
										hasProperty("polygonIdentifier", is(polygon.getPolygonIdentifier())),
										hasProperty("layerType", is(layer.getLayerType()))
								)
						);
					});
				}
			}
		} catch (ResourceParseException | IOException e) {
			throw new ProcessingException(e);
		}
	}

	static InputStream addToEnd(InputStream is, String... lines) {
		var appendix = new ByteArrayInputStream(String.join("\r\n", lines).getBytes(StandardCharsets.US_ASCII));
		return new SequenceInputStream(is, appendix);
	}

	static Map<String, ?> parseWithAppendix(ForwardControlParser parser, String... lines)
			throws IOException, ResourceParseException {

		Class<?> klazz = TestUtils.class;
		try (InputStream baseIs = klazz.getResourceAsStream("VDYP.CTR"); InputStream is = addToEnd(baseIs, lines);) {
			return parser.parse(is, TestUtils.fileResolver(klazz), new HashMap<>());
		}
	}

	Map<String, Object> parse(ForwardControlParser parser, String resourceName)
			throws IOException, ResourceParseException {

		Class<?> klazz = TestUtils.class;
		try (var is = klazz.getResourceAsStream(resourceName)) {

			return parser.parse(is, TestUtils.fileResolver(klazz), new HashMap<>());
		}
	}
}
