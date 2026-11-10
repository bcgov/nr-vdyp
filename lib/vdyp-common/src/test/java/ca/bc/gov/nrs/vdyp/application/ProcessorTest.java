package ca.bc.gov.nrs.vdyp.application;

import static org.easymock.EasyMock.same;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Predicate;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.controlmap.ProcessingResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.projection.ProcessingDataStreamReader;
import ca.bc.gov.nrs.vdyp.io.write.VdypOutputWriter;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class ProcessorTest {

	IMocksControl em;

	@BeforeEach
	void setup() {
		em = EasyMock.createControl();
	}

	@AfterEach
	void verify() {
		em.verify();
	}

	@Nested
	class Process {
		@Test
		void testUseConfiguredMaxPoly() throws ProcessingException {
			Processor<?> unit = EasyMock.partialMockBuilder(Processor.class).addMockedMethod("initialize")
					.addMockedMethod("processStands").createMock(em);
			ProcessingResolvedControlMap controlMap = em.createMock(ProcessingResolvedControlMap.class);

			final Predicate<VdypPolygon> polygonFilter = x -> true;
			final Optional<FileResolver> outputWriter = Optional.empty();

			EasyMock.expect(unit.initialize(controlMap, 0)).andReturn(42).once();

			unit.processStands(
					same(controlMap), EasyMock.same(outputWriter), EasyMock.same(polygonFilter), EasyMock.eq(42)
			);
			EasyMock.expectLastCall().once();

			em.replay();

			unit.process(EnumSet.of(Pass.INITIALIZE, Pass.PROCESS_STANDS), controlMap, outputWriter, polygonFilter);

		}

		@Test
		void testUseDefaultMaxPolyIfNoInitPass() throws ProcessingException {
			Processor<?> unit = EasyMock.partialMockBuilder(Processor.class).addMockedMethod("initialize")
					.addMockedMethod("processStands").createMock(em);
			ProcessingResolvedControlMap controlMap = em.createMock(ProcessingResolvedControlMap.class);

			final Predicate<VdypPolygon> polygonFilter = x -> true;
			final Optional<FileResolver> outputWriter = Optional.empty();

			unit.processStands(
					same(controlMap), EasyMock.same(outputWriter), EasyMock.same(polygonFilter), EasyMock.eq(0)
			);
			EasyMock.expectLastCall().once();

			em.replay();

			unit.process(EnumSet.of(Pass.PROCESS_STANDS), controlMap, outputWriter, polygonFilter);

		}

		@Test
		void testSkipProcessing() throws ProcessingException {
			Processor<?> unit = EasyMock.partialMockBuilder(Processor.class).addMockedMethod("initialize")
					.addMockedMethod("processStands").createMock(em);
			ProcessingResolvedControlMap controlMap = em.createMock(ProcessingResolvedControlMap.class);

			final Predicate<VdypPolygon> polygonFilter = x -> true;
			final Optional<FileResolver> outputWriter = Optional.empty();

			EasyMock.expect(unit.initialize(controlMap, 0)).andReturn(42).once();

			em.replay();

			unit.process(EnumSet.of(Pass.INITIALIZE), controlMap, outputWriter, polygonFilter);

		}
	}

	@Nested
	class Initialize {
		@Test
		void testMaxPolyConfigured() throws ProcessingException {
			Processor<?> unit = EasyMock.partialMockBuilder(Processor.class).createMock(em);

			ProcessingResolvedControlMap controlMap = em.createMock(ProcessingResolvedControlMap.class);

			EasyMock.expect(controlMap.getMaximumPolygonCount()).andStubReturn(Optional.of(42));
			em.replay();

			var maxPolyCount = unit.initialize(controlMap, 0);

			assertThat(maxPolyCount, is(42));

		}

		@Test
		void testMaxPolyDefault() throws ProcessingException {
			Processor<?> unit = EasyMock.partialMockBuilder(Processor.class).createMock(em);

			ProcessingResolvedControlMap controlMap = em.createMock(ProcessingResolvedControlMap.class);

			EasyMock.expect(controlMap.getMaximumPolygonCount()).andStubReturn(Optional.empty());
			em.replay();

			var maxPolyCount = unit.initialize(controlMap, 0);

			assertThat(maxPolyCount, is(0));

		}

	}

	@Nested
	@SuppressWarnings({ "unchecked", "rawtypes" })
	class ProcessStand {
		@Test
		void testEmptyStream() throws ProcessingException, IOException {
			final Processor unit = EasyMock.partialMockBuilder(Processor.class).addMockedMethod("createDataStream")
					.addMockedMethod("createWriter").addMockedMethod("createProcessingEngine").createMock(em);
			final ProcessingResolvedControlMap controlMap = em.createMock(ProcessingResolvedControlMap.class);
			final ProcessingDataStreamReader dataStream = em.createMock(ProcessingDataStreamReader.class);
			final VdypOutputWriter writer = em.createMock(VdypOutputWriter.class);
			final Optional<VdypOutputWriter> writerOpt = Optional.of(writer);

			final Predicate<VdypPolygon> polygonFilter = x -> true;
			final FileResolver outputResolver = em.createMock(FileResolver.class);
			final Optional<FileResolver> outputOpt = Optional.of(outputResolver);

			final ProcessingEngine pe = em.createMock(ProcessingEngine.class);

			EasyMock.expect(unit.createWriter(same(controlMap), same(outputOpt))).andReturn(writerOpt).once();
			EasyMock.expect(unit.createProcessingEngine(same(controlMap), same(writerOpt))).andReturn(pe).once();

			EasyMock.expect(unit.createDataStream(pe)).andReturn(dataStream).once();

			EasyMock.expect(dataStream.readNextPolygon()).andReturn(Optional.empty()).once();

			// Do nothing except close the IO.

			writer.close();
			EasyMock.expectLastCall().once();

			dataStream.close();
			EasyMock.expectLastCall().once();

			em.replay();

			unit.processStands(controlMap, outputOpt, polygonFilter, 42);

		}

		@Test
		void testSinglePolygonStream() throws ProcessingException, IOException {
			final Processor unit = EasyMock.partialMockBuilder(Processor.class).addMockedMethod("createDataStream")
					.addMockedMethod("createWriter").addMockedMethod("createProcessingEngine").createMock(em);
			final ProcessingResolvedControlMap controlMap = em.createMock(ProcessingResolvedControlMap.class);
			final ProcessingDataStreamReader dataStream = em.createMock(ProcessingDataStreamReader.class);
			final VdypOutputWriter writer = em.createMock(VdypOutputWriter.class);
			final Optional<VdypOutputWriter> writerOpt = Optional.of(writer);

			final Predicate<VdypPolygon> polygonFilter = em.createMock(Predicate.class);
			final FileResolver outputResolver = em.createMock(FileResolver.class);
			final Optional<FileResolver> outputOpt = Optional.of(outputResolver);

			final ProcessingEngine pe = em.createMock(ProcessingEngine.class);

			VdypPolygon polygon1 = EasyMock.createMock(VdypPolygon.class);

			EasyMock.expect(unit.createWriter(same(controlMap), same(outputOpt))).andReturn(writerOpt).once();
			EasyMock.expect(unit.createProcessingEngine(same(controlMap), same(writerOpt))).andReturn(pe).once();

			EasyMock.expect(unit.createDataStream(pe)).andReturn(dataStream).once();

			EasyMock.expect(dataStream.readNextPolygon()).andReturn(Optional.of(polygon1)).once();

			// Test the polygon
			EasyMock.expect(polygonFilter.test(polygon1)).andReturn(true).once();

			// Process the polygon
			pe.processPolygon(polygon1);
			EasyMock.expectLastCall().once();

			// End of stream
			EasyMock.expect(dataStream.readNextPolygon()).andReturn(Optional.empty()).once();

			// Close the IO.
			writer.close();
			EasyMock.expectLastCall().once();

			dataStream.close();
			EasyMock.expectLastCall().once();

			em.replay();

			unit.processStands(controlMap, outputOpt, polygonFilter, 42);

		}

		@Test
		void testSinglePolygonStreamWithError() throws ProcessingException, IOException {
			final Processor unit = EasyMock.partialMockBuilder(Processor.class).addMockedMethod("createDataStream")
					.addMockedMethod("createWriter").addMockedMethod("createProcessingEngine").createMock(em);
			final ProcessingResolvedControlMap controlMap = em.createMock(ProcessingResolvedControlMap.class);
			final ProcessingDataStreamReader dataStream = em.createMock(ProcessingDataStreamReader.class);
			final VdypOutputWriter writer = em.createMock(VdypOutputWriter.class);
			final Optional<VdypOutputWriter> writerOpt = Optional.of(writer);

			final Predicate<VdypPolygon> polygonFilter = em.createMock(Predicate.class);
			final FileResolver outputResolver = em.createMock(FileResolver.class);
			final Optional<FileResolver> outputOpt = Optional.of(outputResolver);

			final ProcessingEngine pe = em.createMock(ProcessingEngine.class);

			VdypPolygon polygon1 = EasyMock.createMock(VdypPolygon.class);

			EasyMock.expect(unit.createWriter(same(controlMap), same(outputOpt))).andReturn(writerOpt).once();
			EasyMock.expect(unit.createProcessingEngine(same(controlMap), same(writerOpt))).andReturn(pe).once();

			EasyMock.expect(unit.createDataStream(pe)).andReturn(dataStream).once();

			EasyMock.expect(dataStream.readNextPolygon()).andReturn(Optional.of(polygon1)).once();

			// Test the polygon
			EasyMock.expect(polygonFilter.test(polygon1)).andReturn(true).once();

			// Process the polygon
			final ProcessingException mockError = new ProcessingException();
			pe.processPolygon(polygon1);
			EasyMock.expectLastCall().andThrow(mockError).once();

			// Exit loop so no further calls to readNextPolygon

			// Close the IO.
			writer.close();
			EasyMock.expectLastCall().once();

			dataStream.close();
			EasyMock.expectLastCall().once();

			em.replay();

			var ex = assertThrows(
					ProcessingException.class, () -> unit.processStands(controlMap, outputOpt, polygonFilter, 42)
			);
			assertThat(ex, sameInstance(mockError));

		}

		@Test
		void testMultiplePolygonStream() throws ProcessingException, IOException {
			final Processor unit = EasyMock.partialMockBuilder(Processor.class).addMockedMethod("createDataStream")
					.addMockedMethod("createWriter").addMockedMethod("createProcessingEngine").createMock(em);
			final ProcessingResolvedControlMap controlMap = em.createMock(ProcessingResolvedControlMap.class);
			final ProcessingDataStreamReader dataStream = em.createMock(ProcessingDataStreamReader.class);
			final VdypOutputWriter writer = em.createMock(VdypOutputWriter.class);
			final Optional<VdypOutputWriter> writerOpt = Optional.of(writer);

			final Predicate<VdypPolygon> polygonFilter = em.createMock(Predicate.class);
			final FileResolver outputResolver = em.createMock(FileResolver.class);
			final Optional<FileResolver> outputOpt = Optional.of(outputResolver);

			final ProcessingEngine pe = em.createMock(ProcessingEngine.class);

			VdypPolygon polygon1 = EasyMock.createMock("polygon1", VdypPolygon.class);
			VdypPolygon polygon2 = EasyMock.createMock("polygon2", VdypPolygon.class);

			EasyMock.expect(unit.createWriter(same(controlMap), same(outputOpt))).andReturn(writerOpt).once();
			EasyMock.expect(unit.createProcessingEngine(same(controlMap), same(writerOpt))).andReturn(pe).once();

			EasyMock.expect(unit.createDataStream(pe)).andReturn(dataStream).once();

			// Polygon 1
			EasyMock.expect(dataStream.readNextPolygon()).andReturn(Optional.of(polygon1)).once();

			// Test the polygon
			EasyMock.expect(polygonFilter.test(polygon1)).andReturn(true).once();

			// Process the polygon
			pe.processPolygon(polygon1);
			EasyMock.expectLastCall().once();

			// Polygon 2
			EasyMock.expect(dataStream.readNextPolygon()).andReturn(Optional.of(polygon2)).once();

			// Test the polygon
			EasyMock.expect(polygonFilter.test(polygon2)).andReturn(true).once();

			// Process the polygon
			pe.processPolygon(polygon2);
			EasyMock.expectLastCall().once();

			// End of stream
			EasyMock.expect(dataStream.readNextPolygon()).andReturn(Optional.empty()).once();

			// Close the IO.
			writer.close();
			EasyMock.expectLastCall().once();

			dataStream.close();
			EasyMock.expectLastCall().once();

			em.replay();

			unit.processStands(controlMap, outputOpt, polygonFilter, 42);

		}

		@Test
		void testMaxPolygonApplied() throws ProcessingException, IOException {
			final Processor unit = EasyMock.partialMockBuilder(Processor.class).addMockedMethod("createDataStream")
					.addMockedMethod("createWriter").addMockedMethod("createProcessingEngine").createMock(em);
			final ProcessingResolvedControlMap controlMap = em.createMock(ProcessingResolvedControlMap.class);
			final ProcessingDataStreamReader dataStream = em.createMock(ProcessingDataStreamReader.class);
			final VdypOutputWriter writer = em.createMock(VdypOutputWriter.class);
			final Optional<VdypOutputWriter> writerOpt = Optional.of(writer);

			final Predicate<VdypPolygon> polygonFilter = em.createMock(Predicate.class);
			final FileResolver outputResolver = em.createMock(FileResolver.class);
			final Optional<FileResolver> outputOpt = Optional.of(outputResolver);

			final ProcessingEngine pe = em.createMock(ProcessingEngine.class);

			VdypPolygon polygon1 = EasyMock.createMock("polygon1", VdypPolygon.class);

			EasyMock.expect(unit.createWriter(same(controlMap), same(outputOpt))).andReturn(writerOpt).once();
			EasyMock.expect(unit.createProcessingEngine(same(controlMap), same(writerOpt))).andReturn(pe).once();

			EasyMock.expect(unit.createDataStream(pe)).andReturn(dataStream).once();

			// Polygon 1
			EasyMock.expect(dataStream.readNextPolygon()).andReturn(Optional.of(polygon1)).once();

			// Test the polygon
			EasyMock.expect(polygonFilter.test(polygon1)).andReturn(true).once();

			// Process the polygon
			pe.processPolygon(polygon1);
			EasyMock.expectLastCall().once();

			// Don't request another as we've hit the limit

			// Close the IO.
			writer.close();
			EasyMock.expectLastCall().once();

			dataStream.close();
			EasyMock.expectLastCall().once();

			em.replay();

			unit.processStands(controlMap, outputOpt, polygonFilter, 1);

		}

	}

}
