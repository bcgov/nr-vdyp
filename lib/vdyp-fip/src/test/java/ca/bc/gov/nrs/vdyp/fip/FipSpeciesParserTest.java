package ca.bc.gov.nrs.vdyp.fip;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.fip.model.FipSpecies;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class FipSpeciesParserTest {

	@Test
	void testParseEmpty() throws Exception {

		var parser = new FipSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FIP_INPUT_YIELD_LX_SP0.name(), "test.dat");
		TestUtils.populateControlMapBecReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext("test.dat", TestUtils.makeInputStream(/* empty */));

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FIP_INPUT_YIELD_LX_SP0.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<FipSpecies>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		assertEmpty(stream);
	}

	@Test
	void testParseOneGenus() throws Exception {

		var parser = new FipSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FIP_INPUT_YIELD_LX_SP0.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000001 00     1970 1 B  100.0B  100.0     0.0     0.0     0.0", //
						"01002 S000001 00     1970 Z      0.0     0.0     0.0     0.0     0.0" //
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FIP_INPUT_YIELD_LX_SP0.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<FipSpecies>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var genera = assertNext(stream);

		assertThat(
				genera, containsInAnyOrder(
						allOf(
								hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)), //
								hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("B")), //
								hasProperty("percentGenus", is(100.0f)), //
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														aMapWithSize(1),
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", is("B")),
																		hasProperty("percentage", is(100f))
																)
														)
												)
										)
								)
						)
				)
		);

		assertEmpty(stream);
	}

	@Test
	void testParseTwoGenera() throws Exception {

		var parser = new FipSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FIP_INPUT_YIELD_LX_SP0.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000001 00     1970 1 B   75.0B  100.0     0.0     0.0     0.0", //
						"01002 S000001 00     1970 1 C   25.0C  100.0     0.0     0.0     0.0", //
						"01002 S000001 00     1970 Z      0.0     0.0     0.0     0.0     0.0" //
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FIP_INPUT_YIELD_LX_SP0.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<FipSpecies>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var genera = assertNext(stream);

		assertThat(
				genera, containsInAnyOrder(
						allOf(
								hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)), //
								hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("B")), //
								hasProperty("percentGenus", is(75.0f)), //
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														aMapWithSize(1),
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", is("B")),
																		hasProperty("percentage", is(100f))
																)
														)
												)
										)
								)
						),
						allOf(
								hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)), //
								hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("C")), //
								hasProperty("percentGenus", is(25.0f)), //
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														aMapWithSize(1),
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", is("C")),
																		hasProperty("percentage", is(100f))
																)
														)
												)
										)
								)
						)
				)
		);

		assertEmpty(stream);

	}

	@Test
	void testParseTwoLayers() throws Exception {

		var parser = new FipSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FIP_INPUT_YIELD_LX_SP0.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000001 00     1970 1 B  100.0B  100.0     0.0     0.0     0.0", //
						"01002 S000001 00     1970 V B  100.0B  100.0     0.0     0.0     0.0", //
						"01002 S000001 00     1970 Z      0.0     0.0     0.0     0.0     0.0" //
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FIP_INPUT_YIELD_LX_SP0.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<FipSpecies>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var genera = assertNext(stream);

		assertThat(
				genera, containsInAnyOrder(
						allOf(
								hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)), //
								hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("B")), //
								hasProperty("percentGenus", is(100.0f)), //
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														aMapWithSize(1),
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", is("B")),
																		hasProperty("percentage", is(100f))
																)
														)
												)
										)
								)
						),
						allOf(
								hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)), //
								hasProperty("layerType", is(LayerType.VETERAN)), hasProperty("genus", is("B")), //
								hasProperty("percentGenus", is(100.0f)), //
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														aMapWithSize(1),
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", is("B")),
																		hasProperty("percentage", is(100f))
																)
														)
												)
										)
								)
						)
				)
		);

		assertEmpty(stream);
	}

	@Test
	void testParseTwoPolygons() throws Exception {

		var parser = new FipSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FIP_INPUT_YIELD_LX_SP0.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000001 00     1970 1 B  100.0B  100.0     0.0     0.0     0.0", //
						"01002 S000001 00     1970 Z      0.0     0.0     0.0     0.0     0.0", //
						"01002 S000002 00     1970 1 B  100.0B  100.0     0.0     0.0     0.0", //
						"01002 S000002 00     1970 Z      0.0     0.0     0.0     0.0     0.0" //
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FIP_INPUT_YIELD_LX_SP0.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<FipSpecies>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var genera = assertNext(stream);

		assertThat(
				genera, containsInAnyOrder(
						allOf(
								hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)), //
								hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("B")), //
								hasProperty("percentGenus", is(100.0f)), //
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														aMapWithSize(1),
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", is("B")),
																		hasProperty("percentage", is(100f))
																)
														)
												)
										)
								)
						)
				)
		);

		genera = assertNext(stream);

		assertThat(
				genera, containsInAnyOrder(
						allOf(
								hasProperty("polygonIdentifier", isPolyId("01002 S000002 00", 1970)), //
								hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("B")), //
								hasProperty("percentGenus", is(100.0f)), //
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														aMapWithSize(1),
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", is("B")),
																		hasProperty("percentage", is(100f))
																)
														)
												)
										)
								)
						)
				)
		);

		assertEmpty(stream);
	}

	@Test
	void testParseMultipleSpecies() throws Exception {

		var parser = new FipSpeciesParser();

		Map<String, Object> controlMap = new HashMap<>();

		controlMap.put(ControlKey.FIP_INPUT_YIELD_LX_SP0.name(), "test.dat");
		TestUtils.populateControlMapGenusReal(controlMap);

		var fileResolver = TestUtils.fileResolverContext(
				"test.dat",
				TestUtils.makeInputStream(
						"01002 S000001 00     1970 1 B  100.0B1  75.0B2  10.0B3   8.0B4   7.0", //
						"01002 S000001 00     1970 Z      0.0     0.0     0.0     0.0     0.0" //
				)
		);

		parser.modify(controlMap, fileResolver);

		var parserFactory = controlMap.get(ControlKey.FIP_INPUT_YIELD_LX_SP0.name());

		assertThat(parserFactory, instanceOf(StreamingParserFactory.class));

		@SuppressWarnings("unchecked")
		var stream = ((StreamingParserFactory<Collection<FipSpecies>>) parserFactory).get();

		assertThat(stream, instanceOf(StreamingParser.class));

		var genera = assertNext(stream);

		assertThat(
				genera, containsInAnyOrder(
						allOf(
								hasProperty("polygonIdentifier", isPolyId("01002 S000001 00", 1970)), //
								hasProperty("layerType", is(LayerType.PRIMARY)), hasProperty("genus", is("B")), //
								hasProperty("percentGenus", is(100.0f)), //
								hasProperty(
										"sp64DistributionSet",
										hasProperty(
												"sp64DistributionMap",
												allOf(
														aMapWithSize(4),
														hasEntry(
																is(1),
																allOf(
																		hasProperty("genusAlias", is("B1")),
																		hasProperty("percentage", is(75f))
																)
														),
														hasEntry(
																is(2),
																allOf(
																		hasProperty("genusAlias", is("B2")),
																		hasProperty("percentage", is(10f))
																)
														),
														hasEntry(
																is(3),
																allOf(
																		hasProperty("genusAlias", is("B3")),
																		hasProperty("percentage", is(8f))
																)
														),
														hasEntry(
																is(4),
																allOf(
																		hasProperty("genusAlias", is("B4")),
																		hasProperty("percentage", is(7f))
																)
														)
												)
										)
								)
						)
				)
		);

		assertEmpty(stream);
	}

}
