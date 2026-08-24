package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.assumeThat;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.utilizationAllOnly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.easymock.EasyMock;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.application.test.TestDebugSettings;
import ca.bc.gov.nrs.vdyp.application.test.TestStartApplication;
import ca.bc.gov.nrs.vdyp.application.VdypApplication.Interval;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.CouldNotFindBracketingIntervalException;
import ca.bc.gov.nrs.vdyp.exceptions.FatalProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.model.DebugSettings.UpperBoundsMode;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.NonFipDebugSettings;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class VdypApplicationTest {

	@Nested
	class GetControlMapFileNames {
		@Nested
		class ByCommandLine {
			@Test
			void testInputOne() throws IOException {
				InputStream input = null;
				PrintStream output = null;
				var result = VdypApplication.getControlMapFileNames(
						new String[] { "input.ctl" }, "default.ctl", VdypApplicationIdentifier.FIP_START, output, input
				);
				assertThat(result, contains("input.ctl"));
			}

			@Test
			void testInputTwo() throws IOException {
				InputStream input = null;
				PrintStream output = null;
				var result = VdypApplication.getControlMapFileNames(
						new String[] { "input1.ctl", "input2.ctl" }, "default.ctl", VdypApplicationIdentifier.FIP_START,
						output, input
				);
				assertThat(result, contains("input1.ctl", "input2.ctl"));
			}

			@Test
			void testInputDefault() throws IOException {
				InputStream input = null;
				PrintStream output = null;
				var result = VdypApplication.getControlMapFileNames(
						new String[] { "*" }, "default.ctl", VdypApplicationIdentifier.FIP_START, output, input
				);
				assertThat(result, contains("default.ctl"));
			}

			@Test
			void testInputDefaultPlusOne() throws IOException {
				InputStream input = null;
				PrintStream output = null;
				var result = VdypApplication.getControlMapFileNames(
						new String[] { "*input.ctl" }, "default.ctl", VdypApplicationIdentifier.FIP_START, output, input
				);
				assertThat(result, contains("default.ctl", "input.ctl"));
			}

		}

		@Nested
		class ByInput {
			@Test
			void testInputOne() throws IOException {
				var input = TestUtils.makeInputStream("input.ctl", "");
				var output = new PrintStream(new ByteArrayOutputStream());
				var result = VdypApplication.getControlMapFileNames(
						new String[] {}, "default.ctl", VdypApplicationIdentifier.FIP_START, output, input
				);
				assertThat(result, contains("input.ctl"));
			}

			@Test
			void testInputTwo() throws IOException {
				var input = TestUtils.makeInputStream("input1.ctl input2.ctl", "");
				var output = new PrintStream(new ByteArrayOutputStream());
				var result = VdypApplication.getControlMapFileNames(
						new String[] {}, "default.ctl", VdypApplicationIdentifier.FIP_START, output, input
				);
				assertThat(result, contains("input1.ctl", "input2.ctl"));
			}

			@Test
			void testInputOnePlusDefault() throws IOException {
				var input = TestUtils.makeInputStream("*input.ctl", "");
				var output = new PrintStream(new ByteArrayOutputStream());
				var result = VdypApplication.getControlMapFileNames(
						new String[] {}, "default.ctl", VdypApplicationIdentifier.FIP_START, output, input
				);
				assertThat(result, contains("default.ctl", "input.ctl"));
			}

			@Test
			void testInputJustDefaultExplicit() throws IOException {
				var input = TestUtils.makeInputStream("*", "");
				var output = new PrintStream(new ByteArrayOutputStream());
				var result = VdypApplication.getControlMapFileNames(
						new String[] {}, "default.ctl", VdypApplicationIdentifier.FIP_START, output, input
				);
				assertThat(result, contains("default.ctl"));
			}

			@Test
			void testInputJustDefaultImplicit() throws IOException {
				var input = TestUtils.makeInputStream("", "");
				var output = new PrintStream(new ByteArrayOutputStream());
				var result = VdypApplication.getControlMapFileNames(
						new String[] {}, "default.ctl", VdypApplicationIdentifier.FIP_START, output, input
				);
				assertThat(result, contains("default.ctl"));
			}
		}
	}

	@Nested
	class RunApp {
		@Test
		void testSuccess() throws Exception {
			VdypApplication<?> app = EasyMock.createMock(VdypApplication.class);

			app.logVersionInformation();
			EasyMock.expectLastCall().once();
			app.doMain("test.ctl");
			EasyMock.expectLastCall().once();
			app.close();
			EasyMock.expectLastCall().once();

			EasyMock.replay(app);

			var result = VdypApplication.doRunApp(() -> app, "test.ctl");

			assertThat("exit code", result, is(0));

			EasyMock.verify(app);
		}

		@Test
		void testInitializationFailure() throws Exception {
			VdypApplication<?> app = EasyMock.createMock(VdypApplication.class);

			app.logVersionInformation();
			EasyMock.expectLastCall().once();
			app.doMain("test.ctl");
			var cause = new VdypApplicationInitializationException(null);
			EasyMock.expectLastCall().andThrow(cause).once();
			app.close();
			EasyMock.expectLastCall().once();

			EasyMock.replay(app);

			var result = VdypApplication.doRunApp(() -> app, "test.ctl");

			assertThat("exit code", result, is(1));

			EasyMock.verify(app);
		}

		@Test
		void testProccessingFailure() throws Exception {
			VdypApplication<?> app = EasyMock.createMock(VdypApplication.class);

			app.logVersionInformation();
			EasyMock.expectLastCall().once();
			app.doMain("test.ctl");
			var cause = new VdypApplicationProcessingException(null);
			EasyMock.expectLastCall().andThrow(cause).once();
			app.close();
			EasyMock.expectLastCall().once();

			EasyMock.replay(app);

			var result = VdypApplication.doRunApp(() -> app, "test.ctl");

			assertThat("exit code", result, is(2));

			EasyMock.verify(app);
		}

		@Test
		void testCloseFailure() throws Exception {
			VdypApplication<?> app = EasyMock.createMock(VdypApplication.class);

			app.logVersionInformation();
			EasyMock.expectLastCall().once();

			app.doMain("test.ctl");
			var cause = new IOException();
			EasyMock.expectLastCall().once();
			app.close();
			EasyMock.expectLastCall().andThrow(cause).once();

			EasyMock.replay(app);

			var result = VdypApplication.doRunApp(() -> app, "test.ctl");

			assertThat("exit code", result, is(-1));

			EasyMock.verify(app);
		}

		@Test
		void testCloseFailureAfterInitializationFailure() throws Exception {
			VdypApplication<?> app = EasyMock.createMock(VdypApplication.class);

			var cause1 = new VdypApplicationInitializationException(null);
			var cause2 = new IOException();

			app.logVersionInformation();
			EasyMock.expectLastCall().once();
			app.doMain("test.ctl");
			EasyMock.expectLastCall().andThrow(cause1).once();
			app.close();
			EasyMock.expectLastCall().andThrow(cause2).once();

			EasyMock.replay(app);

			var result = VdypApplication.doRunApp(() -> app, "test.ctl");

			assertThat("exit code", result, is(1));

			EasyMock.verify(app);
		}

		@Test
		void testCloseFailureAfterProcessingFailure() throws Exception {
			VdypApplication<?> app = EasyMock.createMock(VdypApplication.class);

			var cause1 = new VdypApplicationProcessingException(null);
			var cause2 = new IOException();

			app.logVersionInformation();
			EasyMock.expectLastCall().once();
			app.doMain("test.ctl");
			EasyMock.expectLastCall().andThrow(cause1).once();
			app.close();
			EasyMock.expectLastCall().andThrow(cause2).once();

			EasyMock.replay(app);

			var result = VdypApplication.doRunApp(() -> app, "test.ctl");

			assertThat("exit code", result, is(2));

			EasyMock.verify(app);
		}
	}

	@Nested
	class Init {

		@TempDir
		Path directory;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Test
		void testCallsSetControlMap() throws IOException, ResourceParseException {

			var fileResolver = TestUtils.initConfigDir(directory, TestUtils.class, "VRISTART.CTR");

			var em = EasyMock.createControl();
			VdypApplication unit = EasyMock.partialMockBuilder(VdypApplication.class).addMockedMethod("setControlMap")
					.createMock(em);

			Map<String, Object> map = new HashMap<>();

			unit.setControlMap(EasyMock.same(map)); // The thing we're testing.
			BaseControlParser parser = em.createMock(BaseControlParser.class);
			EasyMock.expectLastCall().once();
			EasyMock.expect(unit.getDefaultControlFileName()).andStubReturn("VRISTART.CTR");
			EasyMock.expect(unit.getId()).andStubReturn(VdypApplicationIdentifier.VRI_START);
			EasyMock.expect(unit.getControlFileParser()).andStubReturn(parser);
			EasyMock.expect(
					parser.parseByName(
							EasyMock.eq(List.of("VRISTART.CTR")), EasyMock.same(fileResolver),
							EasyMock.anyObject(Map.class)
					)
			).andStubReturn(map);

			em.replay();

			unit.init(fileResolver, (PrintStream) null, (InputStream) null, new String[] { "VRISTART.CTR" });
			em.verify();
		}
	}

	@Nested
	class QuadMeanDiameterRootFinding {

		VdypApplication<TestDebugSettings> app;
		Processor processor;

		Optional<? extends IOException> initIoError = Optional.empty();
		Optional<? extends ResourceParseException> initParseError = Optional.empty();

		Map<String, Object> controlMap;

		@BeforeEach
		void init() {
			processor = EasyMock.createMock(Processor.class);

			controlMap = TestUtils.loadControlMap();

			TestDebugSettings debug = EasyMock.createMock(TestDebugSettings.class);

			EasyMock.expect(debug.getExpandDiameterForTPHRecovery()).andStubReturn(Optional.of(0.5f));

			EasyMock.replay(debug);

			controlMap.put(ControlKey.DEBUG_SWITCHES.name(), debug);

			app = new TestStartApplication(controlMap, false);

		}

		@Nested
		class ErrorFunction {
			@Test
			void testCompute() {

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x = 0.161783934f;
				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				float result = app
						.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);

				assertThat(result, closeTo(0.00525851687f));
				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(12.8846836f)), //
								hasEntry(is("C"), closeTo(8.87247944f)), //
								hasEntry(is("F"), closeTo(12.5603895f)), //
								hasEntry(is("H"), closeTo(9.33975124f)), //
								hasEntry(is("S"), closeTo(10.9634094f))
						)
				);
			}

			@Test
			void testComputeXClamppedHigh() {

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x = 12;
				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				float result = app
						.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);

				assertThat(result, closeTo(-0.45818153f));
				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(13.8423338f)), //
								hasEntry(is("C"), closeTo(16.6669998f)), //
								hasEntry(is("F"), closeTo(15.5116472f)), //
								hasEntry(is("H"), closeTo(12.5369997f)), //
								hasEntry(is("S"), closeTo(12.6630001f))
						)
				);
			}

			@Test
			void testComputeXClamppedLow() {

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x = -12;
				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				float result = app
						.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);

				assertThat(result, closeTo(0.868255138f));
				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(7.6f)), //
								hasEntry(is("C"), closeTo(7.6f)), //
								hasEntry(is("F"), closeTo(7.6f)), //
								hasEntry(is("H"), closeTo(7.6f)), //
								hasEntry(is("S"), closeTo(7.6f))
						)
				);
			}

			@Test
			void testComputeInitial() {

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x = -10;
				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				float result = app
						.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);

				assertThat(result, closeTo(0.868255138f));
				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(7.6f)), //
								hasEntry(is("C"), closeTo(7.6f)), //
								hasEntry(is("F"), closeTo(7.6f)), //
								hasEntry(is("H"), closeTo(7.6f)), //
								hasEntry(is("S"), closeTo(7.6f))
						)
				);
			}

		}

		@Nested
		class ExpandIntervalOfRootFinder {
			@Test
			void testNoChange() throws Exception {

				UnivariateFunction errorFunc = x -> x;

				var xInterval = new Interval(-1, 1);

				var result = app.findInterval(xInterval, errorFunc, (i, x) -> false);

				app.close();

				assertThat(result, equalTo(xInterval));

			}

			@Test
			void testSimpleChange() throws Exception {

				UnivariateFunction errorFunc = x -> x;

				var xInterval = new Interval(-2, -1);

				var result = app.findInterval(xInterval, errorFunc, (i, x) -> false);

				app.close();

				var evaluated = result.evaluate(errorFunc);
				assertTrue(
						evaluated.start() * evaluated.end() <= 0,
						() -> "F(" + result + ") should have mixed signs but was " + evaluated
				);

			}

			@ParameterizedTest
			@CsvSource({ "1, 1", "-1, 1", "1, -1", "-1, -1" })
			void testDifficultChange(float a, float b) throws Exception {

				UnivariateFunction errorFunc = x -> a * (Math.exp(b * x) - 0.000001);

				var xInterval = new Interval(-1, 1);

				app.close();

				var result = app.findInterval(xInterval, errorFunc, (i, x) -> false);

				var evaluated = result.evaluate(errorFunc);
				assertTrue(
						evaluated.start() * evaluated.end() <= 0,
						() -> "F(" + result + ") should have mixed signs but was " + evaluated
				);

			}

			@ParameterizedTest
			@CsvSource(
				{ "1, 0", "-1, 0", "20, 0", "-20, 0", "1, 0.25", "-1, 0.25", "20, 0.25", "-20, 0.25", "1, -0.25",
						"-1, -0.25", "20, -0.25", "-20, -0.25", "1, 10", "-1, 10", "20, 10", "-20, 10", "1, -10",
						"-1, -10", "20, -10", "-20, -10" }
			)
			void testTwoRoots(float a, float b) throws Exception {

				assumeThat(
						"Fixing VDYP-942 broke the case where the starting interval exactly stradles two roots on a symetric function which shouldn't be relevant to VDYP",
						b, not(0f)
				);

				UnivariateFunction errorFunc = x -> a * ( (x + b) * (x + b) - 0.5);

				var xInterval = new Interval(-1, 1);

				app.close();

				Interval result = assertDoesNotThrow(() -> app.findInterval(xInterval, errorFunc, (i, x) -> false));

				var evaluated = result.evaluate(errorFunc);
				assertTrue(
						evaluated.start() * evaluated.end() <= 0,
						() -> "F(" + result + ") should have mixed signs but was " + evaluated
				);

			}

			@ParameterizedTest
			@CsvSource({ "1, 1", "-1, 1", "1, -1", "-1, -1" })
			void testImpossible(float a, float b) throws Exception {

				UnivariateFunction errorFunc = x -> a * (Math.exp(b * x) + 1);

				var xInterval = new Interval(-1, 1);

				app.close();

				var ex = assertThrows(
						CouldNotFindBracketingIntervalException.class,
						() -> app.findInterval(xInterval, errorFunc, (i, x) -> false)
				);

				assertThat(ex, hasProperty("exitEarly", is(false)));
			}

			@Test
			void testFailEarly() throws Exception {

				// Should find the root, but it will take a a few tries.
				UnivariateFunction errorFunc = x -> (Math.exp(x) - 0.0001);

				var xInterval = new Interval(-1, 1);

				app.close();

				var ex = assertThrows(
						CouldNotFindBracketingIntervalException.class,
						() -> app.findInterval(
								xInterval, errorFunc, (i, x) -> i > 3 // Exit on the third iteration
						)
				);

				assertThat(ex, hasProperty("exitEarly", is(true)));
			}

		}

		@Nested
		class FindRootOfErrorFunction {

			@Test
			void testSuccess() throws ProcessingException {

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;
				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				float result = app.findRootForQuadMeanDiameterFractionalError(
						x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
				);

				assertThat(result, closeTo(0.172141284f));

				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(12.9407434f)), //
								hasEntry(is("C"), closeTo(8.88676834f)), //
								hasEntry(is("F"), closeTo(12.6130743f)), //
								hasEntry(is("H"), closeTo(9.35890579f)), //
								hasEntry(is("S"), closeTo(10.9994669f))
						)
				);

			}

			@Test
			void testNoIntervalGuess() throws ProcessingException {

				app = new TestStartApplication(controlMap, true) {

					@Override
					float quadMeanDiameterFractionalError(
							double x, Map<String, Float> finalDiameters, Map<String, Float> initial,
							Map<String, Float> baseArea, Map<String, Float> min, Map<String, Float> max,
							float totalTreeDensity
					) {
						// Force this to be something with no root. Finding a set of inputs that have no real root or
						// which the interval fixer can't handle would be better

						var f = Math.exp(x) + 1;

						initial.forEach((k, v) -> finalDiameters.put(k, (float) (v * x)));

						return (float) f;
					}

				};

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;

				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				app.setDebugModes(new TestDebugSettings());

				var result = app.findRootForQuadMeanDiameterFractionalError(
						x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
				);

				assertThat(result, closeTo(-38.164387f));

				// Complete nonsense numbers, but they test if the function is doing the right thing based on the
				// nonsense function used in the mock
				assertThat(
						resultPerSpecies, allOf(
								hasEntry(is("B"), closeTo(12.0803461f * (-38.164387f))), //
								hasEntry(is("C"), closeTo(8.66746521f * (-38.164387f))), //
								hasEntry(is("F"), closeTo(11.8044939f * (-38.164387f))), //
								hasEntry(is("H"), closeTo(9.06493855f * (-38.164387f))), //
								hasEntry(is("S"), closeTo(10.4460621f * (-38.164387f)))
						)
				);

			}

			@Test
			void testTooManyEvaluationsStrictThrow() {

				float expectedX = 0.172142f;

				app = new TestStartApplication(controlMap, false) {

					@Override
					double doSolve(float min, float max, UnivariateFunction errorFunc) {
						errorFunc.value(0.1);
						errorFunc.value(expectedX);
						throw new TooManyEvaluationsException(100);
					}

				};

				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;

				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				app.setDebugModes(new TestDebugSettings() {

					@Override
					public boolean getMode1ErrorsFatal() {
						return true;
					}

				});

				assertThrows(
						FatalProcessingException.class,
						() -> app.findRootForQuadMeanDiameterFractionalError(
								x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
						)
				);
			}

			@Test
			void testTooManyEvaluationsGuess() throws ProcessingException {

				float expectedX = 0.172142f;
				app = new TestStartApplication(controlMap, false) {

					@Override
					double doSolve(float min, float max, UnivariateFunction errorFunc) {
						errorFunc.value(0.1);
						errorFunc.value(expectedX);
						throw new TooManyEvaluationsException(100);
					}

				};

				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;

				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				app.setDebugModes(TestUtils.debugSettingsSingle(TestDebugSettings.class, 1, 0));

				var result = app.findRootForQuadMeanDiameterFractionalError(
						x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
				);

				assertThat(result, closeTo((float) expectedX));
				assertThat(
						resultPerSpecies,
						allOf(
								appliedX("B", expectedX, app, initialDqs, minDq, maxDq),
								appliedX("C", expectedX, app, initialDqs, minDq, maxDq),
								appliedX("F", expectedX, app, initialDqs, minDq, maxDq),
								appliedX("H", expectedX, app, initialDqs, minDq, maxDq),
								appliedX("S", expectedX, app, initialDqs, minDq, maxDq)
						)
				);

			}

			@Test
			void testTooManyEvaluationsDiscontinuity() {

				float expectedX = -0.2f;

				app = new TestStartApplication(controlMap, false) {

					@Override
					double doSolve(float min, float max, UnivariateFunction errorFunc) {
						errorFunc.value(0.1);
						errorFunc.value(expectedX);
						throw new TooManyEvaluationsException(100);
					}

				};

				ApplicationTestUtils.setControlMap(app, controlMap);

				Map<String, Float> initialDqs = Utils.constMap(map -> {
					map.put("B", 12.0803461f);
					map.put("C", 8.66746521f);
					map.put("F", 11.8044939f);
					map.put("H", 9.06493855f);
					map.put("S", 10.4460621f);
				});
				Map<String, Float> baseAreas = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);

				});
				Map<String, Float> minDq = Utils.constMap(map -> {
					map.put("B", 7.6f);
					map.put("C", 7.6f);
					map.put("F", 7.6f);
					map.put("H", 7.6f);
					map.put("S", 7.6f);
				});
				Map<String, Float> maxDq = Utils.constMap(map -> {
					map.put("B", 13.8423338f);
					map.put("C", 16.6669998f);
					map.put("F", 15.5116472f);
					map.put("H", 12.5369997f);
					map.put("S", 12.6630001f);
				});

				float x1 = -0.6f;
				float x2 = 0.5f;

				float tph = 748.402222f;

				var resultPerSpecies = new HashMap<String, Float>();

				app.setDebugModes(new TestDebugSettings());

				assertThrows(
						FatalProcessingException.class,
						() -> app.findRootForQuadMeanDiameterFractionalError(
								x1, x2, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph
						)
				);
			}

			static Matcher<Map<? extends String, ? extends Float>> appliedX(
					String species, float expectedX, VdypApplication<?> app, Map<String, Float> initialDqs,
					Map<String, Float> minDq, Map<String, Float> maxDq
			) {
				return hasEntry(
						is(species),
						closeTo(
								app.quadMeanDiameterSpeciesAdjust(
										expectedX, initialDqs.get(species), minDq.get(species), maxDq.get(species)
								)
						)
				);
			}

		}

		@Nested
		class BestOf {

			static double func(double x) {
				return 7 * Math.sin(x / 7) + 2 * Math.sin(x / 2);
			}

			@ParameterizedTest
			@CsvSource(
				{ //
						"-1, 0, 1, 0", // Increasing middle
						"-23, -21, -19, -21", // Decreasing middle
						"4, 10, 20, 20", // Last from above
						"-9, -5, -1, -1", // Last from below
						"2, 8, 14, 2", // First from above
						"22, 30, 34, 22" // First from below
				}
			)
			void testThreePoints(double x1, double x2, double x3, double expect) {

				var result = VdypApplication.bestOf(BestOf::func, x1, x2, x3);

				assertThat(result, is(expect));
			}

			@ParameterizedTest
			@ValueSource(doubles = { 0, -1, 1, -23, -21, -19, -9, -5, 4, 10, 20, 2, 8, 14, 22, 30, 34 })
			void testOnePoint(double x) {

				var result = VdypApplication.bestOf(BestOf::func, x);

				assertThat(result, is(x));
			}

			@Test
			void noPoints() {

				assertThrows(IllegalArgumentException.class, () -> VdypApplication.bestOf(BestOf::func));
			}
		}

		@Nested
		class InitialMaps {
			@Test
			void testCompute() throws ProcessingException {

				VdypLayer layer = VdypLayer.build(lb -> {
					lb.polygonIdentifier("Test", 2024);
					lb.layerType(LayerType.PRIMARY);
					lb.controlMap(controlMap);

					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.percentGenus(10);
						sb.volumeGroup(15);
						sb.decayGroup(11);
						sb.breakageGroup(4);
						sb.loreyHeight(8.98269558f);
						sb.baseArea(0.634290636f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("C");
						sb.percentGenus(20);
						sb.volumeGroup(23);
						sb.decayGroup(15);
						sb.breakageGroup(10);
						sb.loreyHeight(5.06450224f);
						sb.baseArea(1.26858127f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("F");
						sb.percentGenus(30);
						sb.volumeGroup(33);
						sb.decayGroup(27);
						sb.breakageGroup(16);
						sb.loreyHeight(7.1979804f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("H");
						sb.percentGenus(30);
						sb.volumeGroup(40);
						sb.decayGroup(33);
						sb.breakageGroup(19);
						sb.loreyHeight(6.18095589f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.percentGenus(10);
						sb.volumeGroup(69);
						sb.decayGroup(59);
						sb.breakageGroup(30);
						sb.loreyHeight(6.89051533f);
						sb.baseArea(0.634290636f);
					});
				});

				Region region = Region.INTERIOR;

				float quadMeanDiameterTotal = 10.3879938f;
				float baseAreaTotal = 6.34290648f;
				float treeDensityTotal = 748.402222f;
				float loreyHeightTotal = 6.61390257f;

				Map<String, Float> initialDqs = new HashMap<>(5);
				Map<String, Float> baseAreaPerSpecies = new HashMap<>(5);
				Map<String, Float> minPerSpecies = new HashMap<>(5);
				Map<String, Float> maxPerSpecies = new HashMap<>(5);

				app.getDqBySpeciesInitial(
						layer, region, quadMeanDiameterTotal, baseAreaTotal, treeDensityTotal, loreyHeightTotal,
						initialDqs, baseAreaPerSpecies, minPerSpecies, maxPerSpecies
				);

				assertThat(
						initialDqs, allOf(
								hasEntry(is("B"), closeTo(12.0803461f)), //
								hasEntry(is("C"), closeTo(8.66746521f)), //
								hasEntry(is("F"), closeTo(11.8044939f)), //
								hasEntry(is("H"), closeTo(9.06493855f)), //
								hasEntry(is("S"), closeTo(10.4460621f))
						)
				);
				assertThat(
						baseAreaPerSpecies, allOf(
								hasEntry(is("B"), closeTo(0.634290636f)), //
								hasEntry(is("C"), closeTo(1.26858127f)), //
								hasEntry(is("F"), closeTo(1.90287197f)), //
								hasEntry(is("H"), closeTo(1.90287197f)), //
								hasEntry(is("S"), closeTo(0.634290636f))
						)
				);
				assertThat(
						minPerSpecies, allOf(
								hasEntry(is("B"), closeTo(7.6f)), //
								hasEntry(is("C"), closeTo(7.6f)), //
								hasEntry(is("F"), closeTo(7.6f)), //
								hasEntry(is("H"), closeTo(7.6f)), //
								hasEntry(is("S"), closeTo(7.6f))
						)
				);
				assertThat(
						maxPerSpecies, allOf(
								hasEntry(is("B"), closeTo(13.8423338f)), //
								hasEntry(is("C"), closeTo(16.6669998f)), //
								hasEntry(is("F"), closeTo(15.5116472f)), //
								hasEntry(is("H"), closeTo(12.5369997f)), //
								hasEntry(is("S"), closeTo(12.6630001f))
						)
				);
			}
		}

		@Nested
		class ApplyResults {
			@Test
			void testApply() {

				VdypLayer layer = VdypLayer.build(lb -> {
					lb.polygonIdentifier("Test", 2024);
					lb.layerType(LayerType.PRIMARY);
					lb.controlMap(controlMap);
					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.percentGenus(10);
						sb.volumeGroup(15);
						sb.decayGroup(11);
						sb.breakageGroup(4);
						sb.loreyHeight(8.98269558f);
						sb.baseArea(0.634290636f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("C");
						sb.percentGenus(20);
						sb.volumeGroup(23);
						sb.decayGroup(15);
						sb.breakageGroup(10);
						sb.loreyHeight(5.06450224f);
						sb.baseArea(1.26858127f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("F");
						sb.percentGenus(30);
						sb.volumeGroup(33);
						sb.decayGroup(27);
						sb.breakageGroup(16);
						sb.loreyHeight(7.1979804f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("H");
						sb.percentGenus(30);
						sb.volumeGroup(40);
						sb.decayGroup(33);
						sb.breakageGroup(19);
						sb.loreyHeight(6.18095589f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.percentGenus(10);
						sb.volumeGroup(69);
						sb.decayGroup(59);
						sb.breakageGroup(30);
						sb.loreyHeight(6.89051533f);
						sb.baseArea(0.634290636f);
					});
				});

				float baseAreaTotal = 6.34290648f;

				Map<String, Float> baseAreaPerSpecies = Utils.constMap(map -> {
					map.put("B", 0.634290636f);
					map.put("C", 1.26858127f);
					map.put("F", 1.90287197f);
					map.put("H", 1.90287197f);
					map.put("S", 0.634290636f);
				});

				Map<String, Float> diameterPerSpecies = Utils.constMap(map -> {
					map.put("B", 12.9407434f);
					map.put("C", 8.88676834f);
					map.put("F", 12.6130743f);
					map.put("H", 9.35890579f);
					map.put("S", 10.9994669f);
				});

				app.applyDqBySpecies(layer, baseAreaTotal, baseAreaPerSpecies, diameterPerSpecies);

				assertThat(layer.getQuadraticMeanDiameterByUtilization(), utilizationAllOnly(10.3879948f));
				assertThat(layer.getTreesPerHectareByUtilization(), utilizationAllOnly(748.4021f));

				var spec = layer.getSpecies().get("C");
				assertThat(spec.getQuadraticMeanDiameterByUtilization(), utilizationAllOnly(8.88676834f));
				assertThat(spec.getTreesPerHectareByUtilization(), utilizationAllOnly(204.522324f));

				// Total is correct and one of the individual species is correct. No need to check the other 4.

			}
		}

		@Nested
		class CompleteRun {
			@Test
			void testCompute() throws ProcessingException {

				VdypLayer layer = VdypLayer.build(lb -> {
					lb.polygonIdentifier("Test", 2024);
					lb.layerType(LayerType.PRIMARY);
					lb.baseAreaByUtilization(6.34290648f);
					lb.treesPerHectareByUtilization(748.402222f);
					lb.quadraticMeanDiameterByUtilization(10.3879938f);
					lb.loreyHeightByUtilization(6.61390257f);
					lb.controlMap(controlMap);
					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.percentGenus(10);
						sb.volumeGroup(15);
						sb.decayGroup(11);
						sb.breakageGroup(4);
						sb.loreyHeight(8.98269558f);
						sb.baseArea(0.634290636f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("C");
						sb.percentGenus(20);
						sb.volumeGroup(23);
						sb.decayGroup(15);
						sb.breakageGroup(10);
						sb.loreyHeight(5.06450224f);
						sb.baseArea(1.26858127f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("F");
						sb.percentGenus(30);
						sb.volumeGroup(33);
						sb.decayGroup(27);
						sb.breakageGroup(16);
						sb.loreyHeight(7.1979804f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("H");
						sb.percentGenus(30);
						sb.volumeGroup(40);
						sb.decayGroup(33);
						sb.breakageGroup(19);
						sb.loreyHeight(6.18095589f);
						sb.baseArea(1.90287197f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.percentGenus(10);
						sb.volumeGroup(69);
						sb.decayGroup(59);
						sb.breakageGroup(30);
						sb.loreyHeight(6.89051533f);
						sb.baseArea(0.634290636f);
					});
				});

				app.getDqBySpecies(layer, Region.INTERIOR);

				assertThat(layer.getQuadraticMeanDiameterByUtilization(), utilizationAllOnly(10.3879948f));
				assertThat(layer.getTreesPerHectareByUtilization(), utilizationAllOnly(748.4021f));

				var spec = layer.getSpecies().get("C");
				assertThat(spec.getQuadraticMeanDiameterByUtilization(), utilizationAllOnly(8.88676834f));
				assertThat(spec.getTreesPerHectareByUtilization(), utilizationAllOnly(204.522324f));

				// Total is correct and one of the individual species is correct. No need to check the other 4.

			}

			@Test
			void testVDYP942Primary() throws ProcessingException {

				VdypLayer layer = VdypLayer.build(lb -> {
					lb.polygonIdentifier("Test", 2024);
					lb.layerType(LayerType.PRIMARY);
					lb.baseAreaByUtilization(66.666664f);
					lb.treesPerHectareByUtilization(1286.6666f);
					lb.quadraticMeanDiameterByUtilization(25.6848125f);
					lb.loreyHeightByUtilization(11.236668f);
					lb.empiricalRelationshipParameterIndex(117);
					lb.inventoryTypeGroup(28);
					lb.controlMap(controlMap);
					lb.addSpecies(sb -> {
						sb.speciesGroup("PL");
						sb.percentGenus(85);
						sb.volumeGroup(55);
						sb.decayGroup(46);
						sb.breakageGroup(24);
						sb.loreyHeight(11.4126329f);
						sb.baseArea(56.6666641f);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.percentGenus(15);
						sb.volumeGroup(70);
						sb.decayGroup(58);
						sb.breakageGroup(30);
						sb.loreyHeight(10.2395248f);
						sb.baseArea(10.0f);
					});
				});

				app.getDqBySpecies(layer, Region.INTERIOR);

				// Correct values from debugging VDYP7
				assertThat(
						"Layer", layer,
						hasProperty("quadraticMeanDiameterByUtilization", utilizationAllOnly(25.2116508f))
				);
				assertThat(
						"Layer", layer, hasProperty("treesPerHectareByUtilization", utilizationAllOnly(1335.41516f))
				);

				var spec = layer.getSpecies().get("PL");
				assertThat(
						"Species PL", spec,
						hasProperty("quadraticMeanDiameterByUtilization", utilizationAllOnly(25.437809f))
				);
				assertThat(
						"Species PL", spec, hasProperty("treesPerHectareByUtilization", utilizationAllOnly(1115.00903f))
				);

				spec = layer.getSpecies().get("S");
				assertThat(
						"Species S", spec,
						hasProperty("quadraticMeanDiameterByUtilization", utilizationAllOnly(24.0349503f))
				);
				assertThat(
						"Species S", spec, hasProperty("treesPerHectareByUtilization", utilizationAllOnly(220.406128f))
				);

			}
		}
	}

	@Nested
	class DebugModeExpandRootSerchWindow {
		Map<String, Object> controlMap = new HashMap<>();

		@BeforeEach
		void init() {
			NonFipDebugSettings debug = EasyMock.createMock(NonFipDebugSettings.class);
			EasyMock.expect(debug.getUpperBoundsMode()).andStubReturn(UpperBoundsMode.MODE_1);
			EasyMock.expect(debug.getMaxBreastHeightAge()).andStubReturn(Optional.of(300f));
			EasyMock.expect(debug.getNoBasalAreaLimit()).andStubReturn(false);
			EasyMock.expect(debug.getNoQuadraticMeanDiameterLimit()).andStubReturn(false);
			EasyMock.replay(debug);
			controlMap.put(ControlKey.DEBUG_SWITCHES.name(), debug);
		}

		@Test
		void testNoDebug() throws Exception {
			var control = EasyMock.createControl();

			VdypApplication<?> app = new TestStartApplication(controlMap, false);

			Map<String, Float> minDq = new HashMap<>();
			Map<String, Float> maxDq = new HashMap<>();

			minDq.put("A", 10f);
			minDq.put("B", 15f);

			maxDq.put("A", 20f);
			maxDq.put("B", 25f);

			UnivariateFunction func = control.createMock(UnivariateFunction.class);

			control.replay();

			app.debugModeExpandRootSearchWindow(Optional.empty(), minDq, maxDq, func);

			control.verify();

			// No Change
			assertThat(minDq, hasEntry(is("A"), is(10f)));
			assertThat(minDq, hasEntry(is("B"), is(15f)));
			assertThat(maxDq, hasEntry(is("A"), is(20f)));
			assertThat(maxDq, hasEntry(is("B"), is(25f)));

			app.close();

		}

		@Test
		void testDebug50PercentGoodWindow() throws Exception {
			var control = EasyMock.createControl();

			VdypApplication<?> app = new TestStartApplication(controlMap, false);

			Map<String, Float> minDq = new HashMap<>();
			Map<String, Float> maxDq = new HashMap<>();

			minDq.put("A", 10f);
			minDq.put("B", 15f);

			maxDq.put("A", 20f);
			maxDq.put("B", 25f);

			UnivariateFunction func = control.createMock(UnivariateFunction.class);
			EasyMock.expect(func.value(10d)).andReturn(1d);
			EasyMock.expect(func.value(-10d)).andReturn(-1d);

			control.replay();

			app.debugModeExpandRootSearchWindow(Optional.of(0.50f), minDq, maxDq, func);

			control.verify();

			// No Change
			assertThat(minDq, hasEntry(is("A"), is(10f)));
			assertThat(minDq, hasEntry(is("B"), is(15f)));
			assertThat(maxDq, hasEntry(is("A"), is(20f)));
			assertThat(maxDq, hasEntry(is("B"), is(25f)));

			app.close();

		}

		@Test
		void testDebug50PercentBadWindow() throws Exception {
			var control = EasyMock.createControl();

			VdypApplication<?> app = new TestStartApplication(controlMap, false);

			Map<String, Float> minDq = new HashMap<>();
			Map<String, Float> maxDq = new HashMap<>();

			minDq.put("A", 10f);
			minDq.put("B", 15f);

			maxDq.put("A", 20f);
			maxDq.put("B", 25f);

			UnivariateFunction func = control.createMock(UnivariateFunction.class);
			EasyMock.expect(func.value(10d)).andReturn(1d);
			EasyMock.expect(func.value(-10d)).andReturn(1d);

			control.replay();

			app.debugModeExpandRootSearchWindow(Optional.of(0.50f), minDq, maxDq, func);

			control.verify();

			// Limits expanded
			assertThat(minDq, hasEntry(is("A"), closeTo(8.75f)));
			assertThat(minDq, hasEntry(is("B"), closeTo(11.25f)));
			assertThat(maxDq, hasEntry(is("A"), closeTo(26.25f)));
			assertThat(maxDq, hasEntry(is("B"), closeTo(33.75f)));

			app.close();

		}
	}

}
