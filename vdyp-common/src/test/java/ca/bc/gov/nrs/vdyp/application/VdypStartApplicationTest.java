package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.causedBy;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.coe;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.BaseVdypLayer;
import ca.bc.gov.nrs.vdyp.model.BaseVdypPolygon;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

@SuppressWarnings({ "unchecked", "rawtypes" })
class VdypStartApplicationTest {

	private Map<String, Object> controlMap = new HashMap<>();

	@Test
	void testInitWithoutErrors() throws IOException, ResourceParseException {
		BaseControlParser controlParser = EasyMock.createMock(BaseControlParser.class);
		MockFileResolver resolver = dummyIo();

		InputStream inputStream = TestUtils.makeInputStream("");
		resolver.addStream("testControl", inputStream);

		EasyMock.expect(
				controlParser.parse(EasyMock.anyObject(List.class), EasyMock.same(resolver), EasyMock.anyObject())
		).andReturn(controlMap);

		var app = new VdypStartApplication() {

			@Override
			protected BaseControlParser getControlFileParser() {
				return controlParser;
			}

			@Override
			public void process() throws ProcessingException {
				// Do Nothing
			}

			@Override
			public VdypApplicationIdentifier getId() {
				return VdypApplicationIdentifier.FIP_START;
			}

			@Override
			protected BaseVdypSpecies copySpecies(BaseVdypSpecies toCopy, Consumer config) {
				return null;
			}
		};

		EasyMock.replay(controlParser);

		app.init(resolver, "testControl");
		assertThat(app.controlMap, is(controlMap));

		EasyMock.verify(controlParser);
		app.close();
	}

	private MockFileResolver dummyIo() {
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_POLYGON.name(), "DUMMY1");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SPECIES.name(), "DUMMY2");
		controlMap.put(ControlKey.VRI_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(), "DUMMY3");

		MockFileResolver resolver = new MockFileResolver("Test");
		resolver.addStream("DUMMY1", (OutputStream) new ByteArrayOutputStream());
		resolver.addStream("DUMMY2", (OutputStream) new ByteArrayOutputStream());
		resolver.addStream("DUMMY3", (OutputStream) new ByteArrayOutputStream());
		return resolver;
	}

	@Test
	void testInitNoControlFiles() throws IOException, ResourceParseException {
		BaseControlParser controlParser = EasyMock.createMock(BaseControlParser.class);
		MockFileResolver resolver = new MockFileResolver("Test");

		var app = new VdypStartApplication() {

			@Override
			protected BaseControlParser getControlFileParser() {
				return controlParser;
			}

			@Override
			public void process() throws ProcessingException {
				// Do Nothing
			}

			@Override
			public VdypApplicationIdentifier getId() {
				return VdypApplicationIdentifier.FIP_START;
			}

			@Override
			protected BaseVdypSpecies copySpecies(BaseVdypSpecies toCopy, Consumer config) {
				return null;
			}
		};

		EasyMock.replay(controlParser);

		var ex = assertThrows(IllegalArgumentException.class, () -> app.init(resolver));
		assertThat(ex, hasProperty("message", is("At least one control file must be specified.")));

		EasyMock.verify(controlParser);
		app.close();
	}

	@Test
	void testGetStreamingParser() throws IOException, ResourceParseException, ProcessingException {
		BaseControlParser controlParser = EasyMock.createMock(BaseControlParser.class);
		StreamingParserFactory streamingParserFactory = EasyMock.createMock(StreamingParserFactory.class);
		StreamingParser streamingParser = EasyMock.createMock(StreamingParser.class);

		MockFileResolver resolver = dummyIo();

		controlMap.put(ControlKey.FIP_INPUT_YIELD_LAYER.name(), streamingParserFactory);

		EasyMock.expect(streamingParserFactory.get()).andReturn(streamingParser);

		var app = new VdypStartApplication() {

			@Override
			protected BaseControlParser getControlFileParser() {
				return controlParser;
			}

			@Override
			public void process() throws ProcessingException {
				// Do Nothing
			}

			@Override
			public VdypApplicationIdentifier getId() {
				return VdypApplicationIdentifier.FIP_START;
			}

			@Override
			protected BaseVdypSpecies copySpecies(BaseVdypSpecies toCopy, Consumer config) {
				return null;
			}
		};

		EasyMock.replay(controlParser, streamingParserFactory, streamingParser);

		app.init(resolver, controlMap);

		var result = app.getStreamingParser(ControlKey.FIP_INPUT_YIELD_LAYER);

		assertThat(result, is(streamingParser));

		EasyMock.verify(controlParser, streamingParserFactory, streamingParser);

		app.close();
	}

	@Test
	void testGetStreamingParserMapEntryMissing() throws IOException, ResourceParseException, ProcessingException {
		BaseControlParser controlParser = EasyMock.createMock(BaseControlParser.class);

		MockFileResolver resolver = dummyIo();

		var app = new VdypStartApplication() {

			@Override
			protected BaseControlParser getControlFileParser() {
				return controlParser;
			}

			@Override
			public void process() throws ProcessingException {
				// Do Nothing
			}

			@Override
			public VdypApplicationIdentifier getId() {
				return VdypApplicationIdentifier.FIP_START;
			}

			@Override
			protected BaseVdypSpecies copySpecies(BaseVdypSpecies toCopy, Consumer config) {
				return null;
			}
		};

		EasyMock.replay(controlParser);

		app.init(resolver, controlMap);

		@SuppressWarnings("resource") // mock object can't leak anything
		var ex = assertThrows(
				ProcessingException.class, () -> app.getStreamingParser(ControlKey.FIP_INPUT_YIELD_LAYER)
		);
		assertThat(ex, hasProperty("message", is("Data file FIP_INPUT_YIELD_LAYER not specified in control map.")));

		EasyMock.verify(controlParser);

		app.close();
	}

	@Test
	void testGetStreamingParserErrorOpeningFile() throws IOException, ResourceParseException, ProcessingException {
		var mockControl = EasyMock.createControl();

		StreamingParserFactory streamingParserFactory = mockControl.createMock(StreamingParserFactory.class);

		MockFileResolver resolver = dummyIo();
		;
		controlMap.put(ControlKey.FIP_INPUT_YIELD_LAYER.name(), streamingParserFactory);

		IOException exception = new IOException("This is a Test");
		EasyMock.expect(streamingParserFactory.get()).andThrow(exception);

		var app = getTestUnit(mockControl);

		mockControl.replay();

		app.init(resolver, controlMap);

		@SuppressWarnings("resource") // mock object can't leak anything
		var ex = assertThrows(
				ProcessingException.class, () -> app.getStreamingParser(ControlKey.FIP_INPUT_YIELD_LAYER)
		);
		assertThat(ex, hasProperty("message", is("Error while opening data file.")));
		assertThat(ex, causedBy(is(exception)));

		mockControl.verify();

		app.close();
	}

	protected VdypStartApplication getTestUnit(IMocksControl control) throws IOException {

		VdypStartApplication mock = EasyMock.createMockBuilder(VdypStartApplication.class)//
				.addMockedMethods("getControlFileParser", "process", "getId", "copySpecies")//
				.createMock(control);

		return mock;
	}

	@Test
	void testFindPrimarySpeciesNoSpecies() throws Exception {
		var mockControl = EasyMock.createControl();

		try (VdypStartApplication app = getTestUnit(mockControl)) {
			mockControl.replay();
			var allSpecies = Collections.emptyList();
			assertThrows(IllegalArgumentException.class, () -> app.findPrimarySpecies(allSpecies));
		}
		mockControl.verify();
	}

	@Test
	void testFindPrimarySpeciesOneSpecies() throws Exception {
		var mockControl = EasyMock.createControl();

		BaseVdypSpecies spec = mockControl.createMock(BaseVdypSpecies.class);

		EasyMock.expect(spec.getGenus()).andStubReturn("B");
		EasyMock.expect(spec.getPercentGenus()).andStubReturn(100f);
		EasyMock.expect(spec.getFractionGenus()).andStubReturn(1f);

		try (VdypStartApplication app = getTestUnit(mockControl)) {
			EasyMock.expect(app.copySpecies(EasyMock.same(spec), EasyMock.anyObject())).andReturn(spec);
			mockControl.replay();
			var allSpecies = List.of(spec);
			List<BaseVdypSpecies> result = app.findPrimarySpecies(allSpecies);

			assertThat(result, hasSize(1));
			assertThat(result, contains(is(spec)));
		}
		mockControl.verify();
	}

	@Test
	void testFindPrimaryCombinePAIntoPL() throws Exception {
		var mockControl = EasyMock.createControl();

		BaseVdypSpecies spec1 = mockSpecies(mockControl, "PA", 25f);
		BaseVdypSpecies spec2 = mockSpecies(mockControl, "PL", 75f);
		BaseVdypSpecies specCombined = mockSpecies(mockControl, "PL", 100f);
		BaseVdypSpecies.Builder specBuilder = mockControl.createMock(BaseVdypSpecies.Builder.class);
		BaseVdypSpecies.Builder copyBuilder = mockControl.createMock(BaseVdypSpecies.Builder.class); // Should not have
																										// any methods
																										// called.

		Capture<Consumer<BaseVdypSpecies.Builder>> configCapture = Capture.newInstance();
		Capture<Consumer<BaseVdypSpecies.Builder>> copyCapture = Capture.newInstance();

		EasyMock.expect(specBuilder.percentGenus(100f)).andReturn(specBuilder);

		try (VdypStartApplication app = getTestUnit(mockControl)) {
			EasyMock.expect(app.copySpecies(EasyMock.same(spec1), EasyMock.capture(copyCapture))).andReturn(spec1);
			EasyMock.expect(app.copySpecies(EasyMock.same(spec2), EasyMock.capture(copyCapture))).andReturn(spec2);
			EasyMock.expect(app.copySpecies(EasyMock.same(spec2), EasyMock.capture(configCapture)))
					.andReturn(specCombined);
			mockControl.replay();

			var allSpecies = List.of(spec1, spec2);

			List<BaseVdypSpecies> result = app.findPrimarySpecies(allSpecies);

			assertThat(result, hasSize(1));
			assertThat(result, contains(is(specCombined)));

			configCapture.getValue().accept(specBuilder);
			for (var config : copyCapture.getValues()) {
				config.accept(copyBuilder);
			}
		}
		mockControl.verify();
	}

	@Test
	void testFindPrimaryCombinePLIntoPA() throws Exception {

		var mockControl = EasyMock.createControl();

		BaseVdypSpecies spec1 = mockSpecies(mockControl, "PA", 75f);
		BaseVdypSpecies spec2 = mockSpecies(mockControl, "PL", 25f);
		BaseVdypSpecies specCombined = mockSpecies(mockControl, "PA", 100f);
		BaseVdypSpecies.Builder specBuilder = mockControl.createMock(BaseVdypSpecies.Builder.class);
		BaseVdypSpecies.Builder copyBuilder = mockControl.createMock(BaseVdypSpecies.Builder.class); // Should not have
																										// any methods
																										// called.

		Capture<Consumer<BaseVdypSpecies.Builder>> configCapture = Capture.newInstance();
		Capture<Consumer<BaseVdypSpecies.Builder>> copyCapture = Capture.newInstance();

		EasyMock.expect(specBuilder.percentGenus(100f)).andReturn(specBuilder);

		try (VdypStartApplication app = getTestUnit(mockControl)) {
			EasyMock.expect(app.copySpecies(EasyMock.same(spec1), EasyMock.capture(copyCapture))).andReturn(spec1);
			EasyMock.expect(app.copySpecies(EasyMock.same(spec2), EasyMock.capture(copyCapture))).andReturn(spec2);
			EasyMock.expect(app.copySpecies(EasyMock.same(spec1), EasyMock.capture(configCapture)))
					.andReturn(specCombined);
			mockControl.replay();

			var allSpecies = List.of(spec1, spec2);

			List<BaseVdypSpecies> result = app.findPrimarySpecies(allSpecies);

			assertThat(result, hasSize(1));
			assertThat(result, contains(is(specCombined)));

			configCapture.getValue().accept(specBuilder);
			for (var config : copyCapture.getValues()) {
				config.accept(copyBuilder);
			}
		}
		mockControl.verify();
	}

	@Test
	void testFindPrimaryCombineCIntoY() throws Exception {

		var mockControl = EasyMock.createControl();

		BaseVdypSpecies spec1 = mockSpecies(mockControl, "C", 25f);
		BaseVdypSpecies spec2 = mockSpecies(mockControl, "Y", 75f);
		BaseVdypSpecies specCombined = mockSpecies(mockControl, "Y", 100f);

		BaseVdypSpecies.Builder specBuilder = mockControl.createMock(BaseVdypSpecies.Builder.class);
		BaseVdypSpecies.Builder copyBuilder = mockControl.createMock(BaseVdypSpecies.Builder.class); // Should not have
																										// any methods
																										// called.

		Capture<Consumer<BaseVdypSpecies.Builder>> configCapture = Capture.newInstance();
		Capture<Consumer<BaseVdypSpecies.Builder>> copyCapture = Capture.newInstance();

		EasyMock.expect(specBuilder.percentGenus(100f)).andReturn(specBuilder);

		try (VdypStartApplication app = getTestUnit(mockControl)) {
			EasyMock.expect(app.copySpecies(EasyMock.same(spec1), EasyMock.capture(copyCapture))).andReturn(spec1);
			EasyMock.expect(app.copySpecies(EasyMock.same(spec2), EasyMock.capture(copyCapture))).andReturn(spec2);
			EasyMock.expect(app.copySpecies(EasyMock.same(spec2), EasyMock.capture(configCapture)))
					.andReturn(specCombined);
			mockControl.replay();

			var allSpecies = List.of(spec1, spec2);

			List<BaseVdypSpecies> result = app.findPrimarySpecies(allSpecies);

			assertThat(result, hasSize(1));
			assertThat(result, contains(is(specCombined)));

			configCapture.getValue().accept(specBuilder);
			for (var config : copyCapture.getValues()) {
				config.accept(copyBuilder);
			}
		}
		mockControl.verify();
	}

	@Test
	void testFindPrimaryCombineYIntoC() throws Exception {

		var mockControl = EasyMock.createControl();

		BaseVdypSpecies spec1 = mockSpecies(mockControl, "C", 75f);
		BaseVdypSpecies spec2 = mockSpecies(mockControl, "Y", 25f);
		BaseVdypSpecies specCombined = mockSpecies(mockControl, "C", 100f);

		BaseVdypSpecies.Builder specBuilder = mockControl.createMock(BaseVdypSpecies.Builder.class);
		BaseVdypSpecies.Builder copyBuilder = mockControl.createMock(BaseVdypSpecies.Builder.class); // Should not have
																										// any methods
																										// called.

		Capture<Consumer<BaseVdypSpecies.Builder>> configCapture = Capture.newInstance();
		Capture<Consumer<BaseVdypSpecies.Builder>> copyCapture = Capture.newInstance();

		EasyMock.expect(specBuilder.percentGenus(100f)).andReturn(specBuilder);

		try (VdypStartApplication app = getTestUnit(mockControl)) {
			EasyMock.expect(app.copySpecies(EasyMock.same(spec1), EasyMock.capture(copyCapture))).andReturn(spec1);
			EasyMock.expect(app.copySpecies(EasyMock.same(spec2), EasyMock.capture(copyCapture))).andReturn(spec2);
			EasyMock.expect(app.copySpecies(EasyMock.same(spec1), EasyMock.capture(configCapture)))
					.andReturn(specCombined);
			mockControl.replay();

			var allSpecies = List.of(spec1, spec2);

			List<BaseVdypSpecies> result = app.findPrimarySpecies(allSpecies);

			assertThat(result, hasSize(1));
			assertThat(result, contains(is(specCombined)));

			configCapture.getValue().accept(specBuilder);
			for (var config : copyCapture.getValues()) {
				config.accept(copyBuilder);
			}
		}
		mockControl.verify();
	}

	BaseVdypSpecies mockSpecies(IMocksControl mockControl, String id, float percent) {
		BaseVdypSpecies spec = mockControl.createMock("spec" + id, BaseVdypSpecies.class);

		EasyMock.expect(spec.getGenus()).andStubReturn(id);
		EasyMock.expect(spec.getPercentGenus()).andStubReturn(percent);
		EasyMock.expect(spec.getFractionGenus()).andStubReturn(percent / 100f);

		return spec;
	}

	@Test
	void testFindPrimarySort() throws Exception {

		var mockControl = EasyMock.createControl();

		BaseVdypSpecies spec1 = mockSpecies(mockControl, "B", 20f);
		BaseVdypSpecies spec2 = mockSpecies(mockControl, "H", 70f);
		BaseVdypSpecies spec3 = mockSpecies(mockControl, "MB", 10f);
		BaseVdypSpecies.Builder copyBuilder = mockControl.createMock(BaseVdypSpecies.Builder.class); // Should not have
																										// any methods
																										// called.

		Capture<Consumer<BaseVdypSpecies.Builder>> copyCapture = Capture.newInstance();

		try (VdypStartApplication app = getTestUnit(mockControl)) {
			EasyMock.expect(app.copySpecies(EasyMock.same(spec1), EasyMock.capture(copyCapture))).andReturn(spec1);
			EasyMock.expect(app.copySpecies(EasyMock.same(spec2), EasyMock.capture(copyCapture))).andReturn(spec2);
			EasyMock.expect(app.copySpecies(EasyMock.same(spec3), EasyMock.capture(copyCapture))).andReturn(spec3);
			mockControl.replay();

			var allSpecies = List.of(spec1, spec2, spec3);

			List<BaseVdypSpecies> result = app.findPrimarySpecies(allSpecies);

			assertThat(result, hasSize(2));
			assertThat(result, contains(is(spec2), is(spec1)));

			for (var config : copyCapture.getValues()) {
				config.accept(copyBuilder);
			}
		}
		mockControl.verify();

	}

	@Test
	void testFindItg80PercentPure() throws Exception {
		var mockControl = EasyMock.createControl();

		BaseVdypSpecies spec1 = mockSpecies(mockControl, "F", 80f);
		BaseVdypSpecies spec2 = mockSpecies(mockControl, "C", 20f);

		try (VdypStartApplication app = getTestUnit(mockControl)) {
			mockControl.replay();

			Map<String, BaseVdypSpecies> allSpecies = new HashMap<>();
			allSpecies.put(spec1.getGenus(), spec1);
			allSpecies.put(spec2.getGenus(), spec2);

			List<BaseVdypSpecies> primarySpecies = List.of(spec1, spec2);

			var result = app.findItg(primarySpecies);

			assertEquals(1, result);

		}
		mockControl.verify();
	}

	@Test
	void testFindItgNoSecondary() throws Exception {

		var mockControl = EasyMock.createControl();

		BaseVdypSpecies spec1 = mockSpecies(mockControl, "F", 100f);

		try (VdypStartApplication app = getTestUnit(mockControl)) {
			mockControl.replay();

			Map<String, BaseVdypSpecies> allSpecies = new HashMap<>();
			allSpecies.put(spec1.getGenus(), spec1);

			List<BaseVdypSpecies> primarySpecies = List.of(spec1);

			var result = app.findItg(primarySpecies);

			assertEquals(1, result);

		}
		mockControl.verify();
	}

	List<BaseVdypSpecies> primarySecondarySpecies(String primary, String secondary) {
		var mockControl = EasyMock.createControl();

		var spec1 = this.mockSpecies(mockControl, primary, 70f);
		var spec2 = this.mockSpecies(mockControl, secondary, 70f);

		mockControl.replay();

		return List.of(spec1, spec2);
	}

	void assertItgMixed(VdypStartApplication app, int expected, String primary, String... secondary)
			throws ProcessingException {
		for (var sec : secondary) {
			var result = app.findItg(primarySecondarySpecies(primary, sec));
			assertThat(
					result, describedAs("ITG for " + primary + " and " + sec + " should be " + expected, is(expected))
			);
		}
	}

	void assertItgMixed(VdypStartApplication app, int expected, String primary, Collection<String> secondary)
			throws ProcessingException {
		for (var sec : secondary) {
			var mocks = primarySecondarySpecies(primary, sec);
			var result = app.findItg(mocks);
			assertThat(
					result, describedAs("ITG for " + primary + " and " + sec + " should be " + expected, is(expected))
			);
		}
	}

	@Test
	void testFindItgMixed() throws Exception {
		var mockControl = EasyMock.createControl();
		try (VdypStartApplication app = getTestUnit(mockControl)) {
			mockControl.replay();

			assertItgMixed(app, 2, "F", /*  */ "Y", "C");
			assertItgMixed(app, 3, "F", /*  */ "B", "H");
			assertItgMixed(app, 3, "F", /*  */ "H");
			assertItgMixed(app, 4, "F", /*  */ "S");
			assertItgMixed(app, 5, "F", /*  */ "PL", "PA");
			assertItgMixed(app, 6, "F", /*  */ "PY");
			assertItgMixed(app, 7, "F", /*  */ "L", "PW");
			assertItgMixed(app, 8, "F", /*  */ VdypStartApplication.HARDWOODS);

			assertItgMixed(app, 10, "C", /* */ "Y");
			assertItgMixed(app, 11, "C", /* */ "B", "H", "S");
			assertItgMixed(app, 10, "C", /* */ "PL", "PA", "PY", "L", "PW");
			assertItgMixed(app, 10, "C", /* */ VdypStartApplication.HARDWOODS);

			assertItgMixed(app, 10, "Y", /* */ "C");
			assertItgMixed(app, 11, "Y", /* */ "B", "H", "S");
			assertItgMixed(app, 10, "Y", /* */ "PL", "PA", "PY", "L", "PW");
			assertItgMixed(app, 10, "Y", /* */ VdypStartApplication.HARDWOODS);

			assertItgMixed(app, 14, "H", /* */ "C", "Y");
			assertItgMixed(app, 15, "H", /* */ "B");
			assertItgMixed(app, 16, "H", /* */ "S");
			assertItgMixed(app, 17, "H", /* */ VdypStartApplication.HARDWOODS);
			assertItgMixed(app, 13, "H", /* */ "F", "L", "PA", "PL", "PY");

			assertItgMixed(app, 19, "B", /* */ "C", "Y", "H");
			assertItgMixed(app, 20, "B", /* */ "S", "PL", "PA", "PY", "L", "PW");
			assertItgMixed(app, 20, "B", /* */ VdypStartApplication.HARDWOODS);

			assertItgMixed(app, 22, "S", /* */ "F", "L", "PA", "PW", "PY");
			assertItgMixed(app, 23, "S", /* */ "C", "Y", "H");
			assertItgMixed(app, 24, "S", /* */ "B");
			assertItgMixed(app, 25, "S", /* */ "PL");
			assertItgMixed(app, 26, "S", /* */ VdypStartApplication.HARDWOODS);

			assertItgMixed(app, 27, "PW", /**/ "B", "C", "F", "H", "L", "PA", "PL", "PY", "S", "Y");
			assertItgMixed(app, 27, "PW", /**/ VdypStartApplication.HARDWOODS);

			assertItgMixed(app, 28, "PL", /**/ "PA");
			assertItgMixed(app, 30, "PL", /**/ "B", "C", "H", "S", "Y");
			assertItgMixed(app, 29, "PL", /**/ "F", "PW", "L", "PY");
			assertItgMixed(app, 31, "PL", /**/ VdypStartApplication.HARDWOODS);

			assertItgMixed(app, 28, "PA", /**/ "PL");
			assertItgMixed(app, 30, "PA", /**/ "B", "C", "H", "S", "Y");
			assertItgMixed(app, 29, "PA", /**/ "F", "PW", "L", "PY");
			assertItgMixed(app, 31, "PA", /**/ VdypStartApplication.HARDWOODS);

			assertItgMixed(app, 32, "PY", /**/ "B", "C", "F", "H", "L", "PA", "PL", "PW", "S", "Y");
			assertItgMixed(app, 32, "PY", /**/ VdypStartApplication.HARDWOODS);

			assertItgMixed(app, 33, "L", /* */ "F");
			assertItgMixed(app, 34, "L", /* */ "B", "C", "H", "PA", "PL", "PW", "PY", "S", "Y");
			assertItgMixed(app, 34, "L", /* */ VdypStartApplication.HARDWOODS);

			assertItgMixed(app, 35, "AC", /**/ "B", "C", "F", "H", "L", "PA", "PL", "PW", "PY", "S", "Y");
			assertItgMixed(app, 36, "AC", /**/ "AT", "D", "E", "MB");

			assertItgMixed(app, 37, "D", /* */ "B", "C", "F", "H", "L", "PA", "PL", "PW", "PY", "S", "Y");
			assertItgMixed(app, 38, "D", /* */ "AC", "AT", "E", "MB");

			assertItgMixed(app, 39, "MB", /**/ "B", "C", "F", "H", "L", "PA", "PL", "PW", "PY", "S", "Y");
			assertItgMixed(app, 39, "MB", /**/ "AC", "AT", "D", "E");

			assertItgMixed(app, 40, "E", /* */ "B", "C", "F", "H", "L", "PA", "PL", "PW", "PY", "S", "Y");
			assertItgMixed(app, 40, "E", /* */ "AC", "AT", "D", "MB");

			assertItgMixed(app, 41, "AT", /**/ "B", "C", "F", "H", "L", "PA", "PL", "PW", "PY", "S", "Y");
			assertItgMixed(app, 42, "AT", /**/ "AC", "D", "E", "MB");

		}
	}

	@Test
	void testFindEmpericalRelationshipParameterIndexModified() throws Exception {
		var mockControl = EasyMock.createControl();

		controlMap.put(
				ControlKey.DEFAULT_EQ_NUM.name(), new MatrixMap2Impl(
						Collections.singletonList("D"), Collections.singletonList("CDF"), (x, y) -> 42
				)
		);
		controlMap.put(
				ControlKey.EQN_MODIFIERS.name(), new MatrixMap2Impl(
						Collections.singletonList(42), Collections.singletonList(37), (x, y) -> Optional.of(64)
				)
		);

		try (VdypStartApplication app = getTestUnit(mockControl)) {

			MockFileResolver resolver = dummyIo();

			mockControl.replay();

			app.init(resolver, controlMap);

			var bec = new BecDefinition("CDF", Region.COASTAL, "Coastal Douglas Fir");

			int result = app.findEmpiricalRelationshipParameterIndex("D", bec, 37);

			assertThat(result, is(64));
		}
	}

	@Test
	void testFindEmpericalRelationshipParameterIndexUnmodified() throws Exception {
		var mockControl = EasyMock.createControl();

		controlMap.put(
				ControlKey.DEFAULT_EQ_NUM.name(), new MatrixMap2Impl(
						Collections.singletonList("D"), Collections.singletonList("CDF"), (x, y) -> 42
				)
		);
		controlMap.put(
				ControlKey.EQN_MODIFIERS.name(), new MatrixMap2Impl(
						Collections.singletonList(42), Collections.singletonList(37), (x, y) -> Optional.empty()
				)
		);

		try (VdypStartApplication app = getTestUnit(mockControl)) {

			MockFileResolver resolver = dummyIo();

			mockControl.replay();

			app.init(resolver, controlMap);

			var bec = new BecDefinition("CDF", Region.COASTAL, "Coastal Douglas Fir");

			int result = app.findEmpiricalRelationshipParameterIndex("D", bec, 37);

			assertThat(result, is(42));
		}
	}

	@Test
	void testWeightedCoefficientSum() {
		var result = VdypStartApplication
				.weightedCoefficientSum(List.of(2, 4, 6), 6, 1, List.of("A", "B", "C", "D"), s -> {
					switch (s) {
					case "A":
						return 0.2f;
					case "B":
						return 0.4f;
					case "C":
						return 0.1f;
					default:
						return 0.3f;
					}
				}, s -> {
					switch (s) {
					case "A":
						return new Coefficients(new float[] { 2.0f, 10.0f, 7.0f, 0.0f, 9.0f, 0.0f }, 1);
					case "B":
						return new Coefficients(new float[] { 2.0f, 0.0f, 7.0f, 6.0f, 9.0f, 4.0f }, 1);
					case "C":
						return new Coefficients(new float[] { 2.0f, 3.0f, 7.0f, -3.0f, 9.0f, 3.0f }, 1);
					default:
						return new Coefficients(new float[] { 2.0f, 1.0f, 7.0f, 1.0f, 9.0f, 1.0f }, 1);
					}
				});

		assertThat(
				result, coe(
						1, 2f, 2f + 0.3f + 0.3f, 7f, 6f * 0.4f - 3f * 0.1f + 0.3f * 1f, 9f, 4f * 0.4f + 3f * 0.1f
								+ 0.3f * 1f
				)
		);
	}

	@Test
	void testRequireLayer() throws ProcessingException, IOException {
		var mockControl = EasyMock.createControl();
		BaseVdypPolygon poly = mockControl.createMock(BaseVdypPolygon.class);
		BaseVdypLayer layer = mockControl.createMock(BaseVdypLayer.class);
		EasyMock.expect(poly.getLayers()).andStubReturn(Collections.singletonMap(LayerType.PRIMARY, layer));
		EasyMock.expect(poly.getPolygonIdentifier()).andStubReturn("TestPoly");

		try (VdypStartApplication app = getTestUnit(mockControl)) {
			mockControl.replay();

			var result = app.requireLayer(poly, LayerType.PRIMARY);
			assertThat(result, is(layer));
			var ex = assertThrows(StandProcessingException.class, () -> app.requireLayer(poly, LayerType.VETERAN));
			assertThat(
					ex, hasProperty(
							"message", is(
									"Polygon TestPoly has no VETERAN layer, or that layer has non-positive height or crown closure."
							)
					)
			);

		}

	}

}
