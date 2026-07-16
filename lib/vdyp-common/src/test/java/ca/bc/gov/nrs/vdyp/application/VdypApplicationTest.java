package ca.bc.gov.nrs.vdyp.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
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
}
