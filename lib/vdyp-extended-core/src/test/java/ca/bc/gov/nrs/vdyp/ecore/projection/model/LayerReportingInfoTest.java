package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class LayerReportingInfoTest {

	@ParameterizedTest
	@MethodSource("missingLayers")
	void compareOptionalReturnsZeroForMissingLayers(
			Optional<LayerReportingInfo> first, Optional<LayerReportingInfo> second
	) {
		assertEquals(0, LayerReportingInfo.compareOptional(first, second));
	}

	static Stream<Arguments> missingLayers() {
		var layer = Optional.of(reportingInfo("1"));
		var empty = Optional.<LayerReportingInfo>empty();
		return Stream.of(Arguments.of(empty, empty), Arguments.of(empty, layer), Arguments.of(layer, empty));
	}

	@ParameterizedTest
	@CsvSource({ ", 1", "1,", ",", ", D", "D," })
	void compareOptionalReturnsZeroForNullLayerIds(String firstId, String secondId) {
		LayerReportingInfo first = createMock(LayerReportingInfo.class);
		LayerReportingInfo second = createMock(LayerReportingInfo.class);
		expect(first.getLayerID()).andReturn(firstId).anyTimes();
		expect(second.getLayerID()).andReturn(secondId).anyTimes();
		replay(first, second);

		assertEquals(0, LayerReportingInfo.compareOptional(Optional.of(first), Optional.of(second)));
	}

	@ParameterizedTest
	@CsvSource(
		{ "1, 3, -1", "3, 1, 1", "1, 1, 0", "10, 2, -1", "2, 10, 1", "D, 1, 1", "1, D, -1", "D, Z, 1", "Z, D, -1" }
	)
	void compareOptionalOrdersLayerIdsWithDeadLayerLast(String firstId, String secondId, int expectedSign) {
		var first = Optional.of(reportingInfo(firstId));
		var second = Optional.of(reportingInfo(secondId));

		assertEquals(expectedSign, Integer.signum(LayerReportingInfo.compareOptional(first, second)));
	}

	private static LayerReportingInfo reportingInfo(String layerId) {
		var layer = new Layer.Builder().layerId(layerId).build();
		return new LayerReportingInfo.Builder().layer(layer).build();
	}
}
