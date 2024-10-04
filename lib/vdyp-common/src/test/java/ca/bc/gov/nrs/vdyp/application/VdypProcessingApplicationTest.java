package ca.bc.gov.nrs.vdyp.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.EnumSet;
import java.util.List;

import org.easymock.EasyMock;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.FileResolver;

class VdypProcessingApplicationTest {

	@Nested
	class Run {

		VdypProcessingApplication app;
		Processor processor;

		@BeforeEach
		void init() {
			processor = EasyMock.createMock(Processor.class);

			app = new VdypProcessingApplication() {

				@Override
				public String getDefaultControlFileName() {
					return "default.ctl";
				}

				@Override
				protected Processor getProcessor() {
					return processor;
				}

				@Override
				public VdypApplicationIdentifier getId() {
					fail();
					return null;
				}
			};

		}

		@Test
		void testCommandLineControlNoError() throws Exception {
			var outBytes = new ByteArrayOutputStream();
			var outPrint = new PrintStream(outBytes);
			var input = new ByteArrayInputStream("\n".getBytes());

			processor.run(
					EasyMock.isA(FileResolver.class),
					EasyMock.isA(FileResolver.class),
					EasyMock.eq(List.of("argument.ctl")),
					EasyMock.eq(EnumSet.allOf(Pass.class))
			);
			EasyMock.expectLastCall().once();

			EasyMock.replay(processor);

			int result = app.run(outPrint, input, "argument.ctl");

			assertThat(result, is(0));

			EasyMock.verify(processor);
		}

		@Test
		void testMultipleCommandLineControlNoError() throws Exception {
			var outBytes = new ByteArrayOutputStream();
			var outPrint = new PrintStream(outBytes);
			var input = new ByteArrayInputStream("\n".getBytes());

			processor.run(
					EasyMock.isA(FileResolver.class),
					EasyMock.isA(FileResolver.class),
					EasyMock.eq(List.of("argument1.ctl", "argument2.ctl")),
					EasyMock.eq(EnumSet.allOf(Pass.class))
			);
			EasyMock.expectLastCall().once();

			EasyMock.replay(processor);

			int result = app.run(outPrint, input, "argument1.ctl", "argument2.ctl");

			assertThat(result, is(0));

			EasyMock.verify(processor);
		}

		@Test
		void testConsoleInputControlNoError() throws Exception {
			var outBytes = new ByteArrayOutputStream();
			var outPrint = new PrintStream(outBytes);
			var input = new ByteArrayInputStream("alternate1.ctl alternate2.ctl\n".getBytes());

			processor.run(
					EasyMock.isA(FileResolver.class),
					EasyMock.isA(FileResolver.class),
					EasyMock.eq(List.of("alternate1.ctl", "alternate2.ctl")),
					EasyMock.eq(EnumSet.allOf(Pass.class))
			);
			EasyMock.expectLastCall().once();

			EasyMock.replay(processor);

			int result = app.run(outPrint, input);

			assertThat(result, is(0));

			EasyMock.verify(processor);
		}

		@Test
		void testErrorGettingControlFileNames() throws Exception {
			PrintStream outPrint = null;
			InputStream input = null;

			EasyMock.replay(processor);

			int result = app.run(outPrint, input);

			assertThat(result, is(1));

			EasyMock.verify(processor);
		}

		@Test
		void testErrorWhileProcessing() throws Exception {
			var outBytes = new ByteArrayOutputStream();
			var outPrint = new PrintStream(outBytes);
			var input = new ByteArrayInputStream("\n".getBytes());

			processor.run(
					EasyMock.isA(FileResolver.class),
					EasyMock.isA(FileResolver.class),
					EasyMock.eq(List.of("argument.ctl")),
					EasyMock.eq(EnumSet.allOf(Pass.class))
			);
			EasyMock.expectLastCall().andThrow(new ProcessingException("Test")).once();

			EasyMock.replay(processor);

			int result = app.run(outPrint, input, "argument.ctl");

			assertThat(result, is(2));

			EasyMock.verify(processor);
		}
	}

	@Nested
	class GetControlFileNamesFromUser {

		VdypProcessingApplication app;

		@BeforeEach
		void init() {
			app = new VdypProcessingApplication() {

				@Override
				public String getDefaultControlFileName() {
					return "default.ctl";
				}

				@Override
				protected Processor getProcessor() {
					fail();
					return null;
				}

				@Override
				public VdypApplicationIdentifier getId() {
					fail();
					return null;
				}

			};
		}

		@Test
		void testJustDefault() throws Exception {
			var outBytes = new ByteArrayOutputStream();
			var outPrint = new PrintStream(outBytes);
			var input = new ByteArrayInputStream("\n".getBytes());
			List<String> result = app.getControlFileNamesFromUser(outPrint, input);
			assertThat(result, Matchers.contains("default.ctl"));
		}

		@Test
		void testOneEntry() throws Exception {
			var outBytes = new ByteArrayOutputStream();
			var outPrint = new PrintStream(outBytes);
			var input = new ByteArrayInputStream("alternate.ctl\n".getBytes());
			List<String> result = app.getControlFileNamesFromUser(outPrint, input);
			assertThat(result, Matchers.contains("alternate.ctl"));
		}

		@Test
		void testTwoEntries() throws Exception {
			var outBytes = new ByteArrayOutputStream();
			var outPrint = new PrintStream(outBytes);
			var input = new ByteArrayInputStream("alternate1.ctl alternate2.ctl\n".getBytes());
			List<String> result = app.getControlFileNamesFromUser(outPrint, input);
			assertThat(result, Matchers.contains("alternate1.ctl", "alternate2.ctl"));
		}

		@Test
		void testOneEntryPlusDefault() throws Exception {
			var outBytes = new ByteArrayOutputStream();
			var outPrint = new PrintStream(outBytes);
			var input = new ByteArrayInputStream("*alternate.ctl\n".getBytes());
			List<String> result = app.getControlFileNamesFromUser(outPrint, input);
			assertThat(result, Matchers.contains("default.ctl", "alternate.ctl"));
		}

		@Test
		void testTwoEntriesPlusDefault() throws Exception {
			var outBytes = new ByteArrayOutputStream();
			var outPrint = new PrintStream(outBytes);
			var input = new ByteArrayInputStream("*alternate1.ctl alternate2.ctl\n".getBytes());
			List<String> result = app.getControlFileNamesFromUser(outPrint, input);
			assertThat(result, Matchers.contains("default.ctl", "alternate1.ctl", "alternate2.ctl"));
		}
	}
}
