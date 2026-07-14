package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.causedBy;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Predicate;

import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationProcessingException;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.test.ProcessingTestUtils;

class VdypProcessingApplicationTest {

	@Nested
	class DoMain {

		VdypProcessingApplication app;
		Processor processor;

		Optional<? extends IOException> initIoError = Optional.empty();
		Optional<? extends ResourceParseException> initParseError = Optional.empty();

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
					return VdypApplicationIdentifier.VDYP_FORWARD;
				}

				@Override
				public void close() {
					// Do Nothing
				}

				@Override
				protected void init(
						FileSystemFileResolver resolver, PrintStream writeToIfNoArgs, InputStream readFromIfNoArgs,
						String... controlFilePaths
				) throws IOException, ResourceParseException {
					Utils.throwIfPresent(initIoError);
					Utils.throwIfPresent(initParseError);
					this.resolvedControlMap = new ProcessingResolvedControlMapImpl(
							ProcessingTestUtils.loadControlMap()
					);
				}

			};

		}

		EnumSet<Pass> defaultPasses = EnumSet.complementOf(EnumSet.of(Pass.ADDITIONAL_BASE_AREA_CRITERIA));

		@SuppressWarnings("unchecked")
		@Test
		void testErrorWhileProcessing() throws Exception {

			processor.process(
					eq(defaultPasses), isA(ProcessingResolvedControlMap.class), isA(Optional.class),
					isA(Predicate.class)
			);
			var cause = new ProcessingException("Test");
			EasyMock.expectLastCall().andThrow(cause).once();

			EasyMock.replay(processor);

			var ex = assertThrows(VdypApplicationProcessingException.class, () -> app.doMain("argument.ctl"));

			assertThat(ex, causedBy(sameInstance(cause)));

			EasyMock.verify(processor);
		}

		@Test
		void testIoErrorWhileInitializing() throws Exception {

			var cause = new IOException("Test");
			initIoError = Optional.of(cause);

			EasyMock.replay(processor);

			var ex = assertThrows(VdypApplicationInitializationException.class, () -> app.doMain("argument.ctl"));

			assertThat(ex, causedBy(sameInstance(cause)));

			EasyMock.verify(processor);
		}

		@Test
		void testParseErrorWhileInitializing() throws Exception {

			var cause = new ResourceParseException("Test");
			initParseError = Optional.of(cause);

			EasyMock.replay(processor);

			var ex = assertThrows(VdypApplicationInitializationException.class, () -> app.doMain("argument.ctl"));

			assertThat(ex, causedBy(sameInstance(cause)));

			EasyMock.verify(processor);
		}

		@SuppressWarnings("unchecked")
		@Test
		void testCleanRun() throws Exception {

			processor.process(
					eq(defaultPasses), isA(ProcessingResolvedControlMap.class), isA(Optional.class),
					isA(Predicate.class)
			);
			EasyMock.expectLastCall().once();

			EasyMock.replay(processor);

			assertDoesNotThrow(() -> app.doMain("argument.ctl"));

			EasyMock.verify(processor);
		}

	}

}
