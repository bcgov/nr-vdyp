package ca.bc.gov.nrs.vdyp.application;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ProcessorTest {

	@SuppressWarnings("unchecked")
	@Test
	void test() throws IOException, ResourceParseException, ProcessingException {

		Processor unit = EasyMock.partialMockBuilder(Processor.class).addMockedMethod("getControlFileParser")
				.addMockedMethod("process", Set.class, Map.class, Optional.class, Predicate.class).createStrictMock();

		BaseControlParser controlParser = EasyMock.createMock(BaseControlParser.class);

		var inputResolver = new MockFileResolver("input");
		var outputResolver = new MockFileResolver("output");

		var is = TestUtils.makeInputStream("TEST");

		inputResolver.addStream("test.ctr", is);

		var mockMap = new HashMap<String, Object>();

		EasyMock.expect(
				controlParser
						.parse(EasyMock.same(is), EasyMock.anyObject(FileResolver.class), EasyMock.anyObject(Map.class))
		).andStubReturn(mockMap);

		EasyMock.expect(unit.getControlFileParser()).andStubReturn(controlParser);

		unit.process(
				EasyMock.eq(EnumSet.allOf(Pass.class)), EasyMock.same(mockMap), EasyMock.anyObject(Optional.class),
				EasyMock.anyObject(Predicate.class)
		);
		EasyMock.expectLastCall();

		EasyMock.replay(unit, controlParser);

		unit.run(inputResolver, outputResolver, List.of("test.ctr"), EnumSet.allOf(Pass.class));

		EasyMock.verify(unit, controlParser);
	}
}
