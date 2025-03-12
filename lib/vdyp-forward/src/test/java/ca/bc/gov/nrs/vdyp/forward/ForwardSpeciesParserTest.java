package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypSpeciesParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class ForwardSpeciesParserTest {

	@Test
	void testParseEmpty() throws Exception {

		var parser = new VdypSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext("test.dat", TestUtils.makeInputStream(/* empty */));

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<VdypSpecies>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		assertEmpty(stream);
	}

	@Test
	void testParseOneGenus() throws Exception {

		var parser = new VdypSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9",
						"01002 S000001 00     1970"
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<VdypSpecies>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var genera = assertNext(stream);

		assertThat(
				genera,
				hasItem(
						allOf(
								hasProperty("polygonIdentifier", hasProperty("base", is("01002 S000001 00"))),
								hasProperty("polygonIdentifier", hasProperty("year", is(1970))),
								hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genusIndex", is(15)),
								hasProperty("genus", is("S")),
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												hasEntry(
														is(1),
														allOf(
																hasProperty("index", is(1)),
																hasProperty("genusAlias", is("S")),
																hasProperty("percentage", is(100.0f))
														)
												)
										)
								), hasProperty("site", VdypMatchers.notPresent())
						)
				)
		);

		assertEmpty(stream);
	}

	@Test
	void testParseTwoPairsOfGenera() throws Exception {

		var parser = new VdypSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000002 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
						"01002 S000002 00     1970 V  3 B  B   50.0 S  50.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0", //
						"01002 S000002 00     1970", //
						"01002 S000003 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
						"01002 S000003 00     1970 V  3 B  B   50.0 S  50.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0", //
						"01002 S000003 00     1970" //
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<VdypSpecies>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var genera1 = assertNext(stream);
		assertThat(genera1, hasSize(2));

		var genera2 = assertNext(stream);
		assertThat(genera2, hasSize(2));

		assertEmpty(stream);
	}

	@Test
	void testParseComputesAgesWhenMissing() throws Exception {

		var parser = new VdypSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000002 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  20.0  12.0  -9.0 0 -9", //
						"01002 S000002 00     1970 V  3 B  B   50.0 S  50.0     0.0     0.0 -9.00 -9.00  -9.0   8.0   4.0", //
						"01002 S000002 00     1970 V  5 L  L   50.0 S  25.0 AC 15.0 B  10.0 -9.00 -9.00  14.0  -9.0   6.0 1", //
						"01002 S000002 00     1970" //
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<VdypSpecies>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var genera = assertNext(stream);
		assertThat(genera, hasSize(3));

		assertThat(
				genera,
				hasItems(
						allOf(hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("site", notPresent())),
						allOf(
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", is("B")),
																		hasProperty("percentage", is(50.0f))
																)
														),
														hasEntry(
																is(2),
																allOf(
																		hasProperty("genusAlias", is("S")),
																		hasProperty("percentage", is(50.0f))
																)
														)
												)
										)
								), hasProperty("layerType", is(LayerType.VETERAN)), hasProperty("site", notPresent())
						),
						allOf(
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", is("L")),
																		hasProperty("percentage", is(50.0f))
																)
														),
														hasEntry(
																is(2),
																allOf(
																		hasProperty("genusAlias", is("S")),
																		hasProperty("percentage", is(25.0f))
																)
														),
														hasEntry(
																is(3),
																allOf(
																		hasProperty("genusAlias", is("AC")),
																		hasProperty("percentage", is(15.0f))
																)
														),
														hasEntry(
																is(4),
																allOf(
																		hasProperty("genusAlias", is("B")),
																		hasProperty("percentage", is(10.0f))
																)
														)
												)
										)
								), hasProperty("layerType", is(LayerType.VETERAN)),
								hasProperty(
										"site", present(
												allOf(
														hasProperty("ageTotal", present(is(14.0f))),
														// hasProperty("yearsAtBreastHeight", present(is(8.0f))),
														hasProperty("yearsToBreastHeight", present(is(6.0f)))
												)
										)
								)
						)
				)
		);

		assertThat(
				genera.stream().filter(s -> s.getSite().isPresent())
						.map(s -> s.getSite().get().getYearsAtBreastHeight()).toList(),
				hasItems(present(is(8.0f)))
		);

		assertEmpty(stream);
	}

	@Test
	void testParseTwoGenera() throws Exception {

		var parser = new VdypSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000002 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 14.50 -9.00  -9.0  -9.0  -9.0 1 -9", //
						"01002 S000002 00     1970 V  3 B  B   50.0 S  50.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0", //
						"01002 S000002 00     1970" //
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<VdypSpecies>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var genera = assertNext(stream);

		assertThat(
				genera,
				hasItems(
						allOf(
								hasProperty("polygonIdentifier", hasProperty("base", is("01002 S000002 00"))),
								hasProperty("polygonIdentifier", hasProperty("year", is(1970))),
								hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genusIndex", is(15)),
								hasProperty("genus", is("S")),
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												hasEntry(
														is(1),
														allOf(
																hasProperty("index", is(1)),
																hasProperty("genusAlias", is("S")),
																hasProperty("percentage", is(100.0f))
														)
												)
										)
								),
								hasProperty(
										"site",
										present(
												allOf(
														hasProperty("siteIndex", present(is(14.50f))),
														hasProperty("siteGenus", is("S")),
														hasProperty("height", present(is(Float.NaN))),
														hasProperty("ageTotal", present(is(Float.NaN))),
														hasProperty("yearsToBreastHeight", present(is(Float.NaN))),
														hasProperty("layerType", is(LayerType.PRIMARY)),
														hasProperty(
																"siteCurveNumber",
																present(is(VdypEntity.MISSING_INTEGER_VALUE))
														)
												)
										)
								)
						),
						allOf(
								hasProperty("polygonIdentifier", hasProperty("base", is("01002 S000002 00"))),
								hasProperty("polygonIdentifier", hasProperty("year", is(1970))),
								hasProperty("layerType", is(LayerType.VETERAN)), hasProperty("genusIndex", is(3)),
								hasProperty("genus", is("B")),
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														hasEntry(
																is(1),
																allOf(
																		hasProperty("index", is(1)),
																		hasProperty("genusAlias", is("B")),
																		hasProperty("percentage", is(50.0f))
																)
														),
														hasEntry(
																is(2),
																allOf(
																		hasProperty("index", is(2)),
																		hasProperty("genusAlias", is("S")),
																		hasProperty("percentage", is(50.0f))
																)
														)
												)
										)
								), hasProperty("site", notPresent())
						)
				)
		);

		assertEmpty(stream);
	}
}
