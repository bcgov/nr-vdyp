package ca.bc.gov.nrs.vdyp.forward;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.Change;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.ExecutionStep;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardControlVariables;
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ForwardProcessingEngineTest {

	@Nested
	class GrowSpecies {
		IMocksControl em;
		ForwardProcessingEngine fpe;
		LayerProcessingState lps;
		Map<String, Object> controlMap;

		@BeforeEach
		void setup() throws IOException, ResourceParseException, ValueParseException {
			var parser = new ForwardControlParser();
			controlMap = ForwardTestUtils.parse(parser, "VDYP.CTR");
			Map<String, Object> controlMap = TestUtils.loadControlMap();
			controlMap.put(ControlKey.VTROL.name(), new ForwardControlVariables(new Integer[] {}));
			em = EasyMock.createControl();
			fpe = EasyMock.partialMockBuilder(ForwardProcessingEngine.class).addMockedMethod("growLoreyHeights")
					.addMockedMethod("growUsingPartialSpeciesDynamics").addMockedMethod("growUsingNoSpeciesDynamics")
					.addMockedMethod("growUsingFullSpeciesDynamics").withConstructor(controlMap, Optional.empty())
					.createMock(em);
			lps = em.createMock(LayerProcessingState.class);
		}

		@Test
		void testPartialSuccess() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.30947846f);
			var ba = Change.delta(45.386444f, 0.35185215f);
			var tph = Change.range(601.3737f, 594.1138f);

			fpe.growLoreyHeights(
					EasyMock.same(lps), EasyMock.eq(dh.start(), 0.01f), EasyMock.eq(dh.end(), 0.01f),
					EasyMock.eq(400f, 0.01f), EasyMock.eq(395.17108f, 0.01f), EasyMock.eq(31.0f, 0.01f)
			);
			EasyMock.expectLastCall();

			EasyMock.expect(
					fpe.growUsingPartialSpeciesDynamics(
							EasyMock.same(ba), EasyMock.same(dq), EasyMock.eq(tph.start(), 0.01f), EasyMock.anyObject()
					)
			).andReturn(true);

			// Leads to no other methods being used.

			em.replay();

			// Dynamics mode 2, Partial
			fpe.growSpecies(lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, 2);

			em.verify();
		}

		@Test
		void testPartialFailOneSpecies() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.30947846f);
			var ba = Change.delta(45.386444f, 0.35185215f);
			var tph = Change.range(601.3737f, 594.1138f);

			fpe.growLoreyHeights(
					EasyMock.same(lps), EasyMock.eq(dh.start(), 0.01f), EasyMock.eq(dh.end(), 0.01f),
					EasyMock.eq(400f, 0.01f), EasyMock.eq(395.17108f, 0.01f), EasyMock.eq(31.0f, 0.01f)
			);
			EasyMock.expectLastCall().once();

			EasyMock.expect(
					fpe.growUsingPartialSpeciesDynamics(
							EasyMock.same(ba), EasyMock.same(dq), EasyMock.eq(tph.start(), 0.01f), EasyMock.anyObject()
					)
			).andReturn(false).once();

			// Leads to no dynamics method being used
			fpe.growUsingNoSpeciesDynamics(EasyMock.eq(ba.rate(), 0.01f), EasyMock.eq(tph.factor(), 0.01f));
			EasyMock.expectLastCall().once();

			em.replay();

			// Dynamics mode 2, Partial
			fpe.growSpecies(lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, 2);

			em.verify();
		}

		@Test
		void testPartialFailTwoSpeciesNoGrowthDQ() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
				lb.addSpecies(sb -> {
					sb.genus("S", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.0f); // No Growth
			var ba = Change.delta(45.386444f, 0.1f); // No Growth
			var tph = Change.range(601.3737f, 594.1138f);

			fpe.growLoreyHeights(
					EasyMock.same(lps), EasyMock.eq(dh.start(), 0.01f), EasyMock.eq(dh.end(), 0.01f),
					EasyMock.eq(400f, 0.01f), EasyMock.eq(395.17108f, 0.01f), EasyMock.eq(31.0f, 0.01f)
			);
			EasyMock.expectLastCall().once();

			EasyMock.expect(
					fpe.growUsingPartialSpeciesDynamics(
							EasyMock.same(ba), EasyMock.same(dq), EasyMock.eq(tph.start(), 0.01f), EasyMock.anyObject()
					)
			).andReturn(false).once(); // No Growth will result in failure

			// Leads to no dynamics method being used
			fpe.growUsingNoSpeciesDynamics(
					EasyMock.eq(0.002f, 0.001f), // No Growth
					EasyMock.eq(tph.factor(), 0.01f)
			);
			EasyMock.expectLastCall().once();

			em.replay();

			// Dynamics mode 2, Partial
			fpe.growSpecies(lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, 2);

			em.verify();
		}

		@Test
		void testPartialFailTwoSpeciesNoGrowthBA() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
				lb.addSpecies(sb -> {
					sb.genus("S", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.1f); // No Growth
			var ba = Change.delta(45.386444f, 0.0f); // No Growth
			var tph = Change.range(601.3737f, 594.1138f);

			fpe.growLoreyHeights(
					EasyMock.same(lps), EasyMock.eq(dh.start(), 0.01f), EasyMock.eq(dh.end(), 0.01f),
					EasyMock.eq(400f, 0.01f), EasyMock.eq(395.17108f, 0.01f), EasyMock.eq(31.0f, 0.01f)
			);
			EasyMock.expectLastCall().once();

			EasyMock.expect(
					fpe.growUsingPartialSpeciesDynamics(
							EasyMock.same(ba), EasyMock.same(dq), EasyMock.eq(tph.start(), 0.01f), EasyMock.anyObject()
					)
			).andReturn(false).once(); // No Growth will result in failure

			// Leads to no dynamics method being used
			fpe.growUsingNoSpeciesDynamics(
					EasyMock.eq(0.0f, 0.01f), // No Growth
					EasyMock.eq(tph.factor(), 0.01f)
			);
			EasyMock.expectLastCall().once();

			em.replay();

			// Dynamics mode 2, Partial
			fpe.growSpecies(lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, 2);

			em.verify();
		}

		@Test
		void testDebugSpeciesDynamicsDisabled() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
				lb.addSpecies(sb -> {
					sb.genus("S", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.30947846f);
			var ba = Change.delta(45.386444f, 0.35185215f);
			var tph = Change.range(601.3737f, 594.1138f);

			// Go straight to No Species Dynamics
			fpe.growUsingNoSpeciesDynamics(EasyMock.eq(ba.rate(), 0.01f), EasyMock.eq(tph.factor(), 0.01f));
			EasyMock.expectLastCall().once();

			em.replay();

			// Dynamics mode 1, None
			fpe.growSpecies(lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, 1);

			em.verify();
		}

		@Test
		void testDebug1SpeciesDynamicsFull() throws ProcessingException {

			var layer = VdypLayer.build(lb -> {
				lb.polygonIdentifier("Test", 2020);
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.genus("H", controlMap);
				});
				lb.addSpecies(sb -> {
					sb.genus("S", controlMap);
				});
			});
			Bank bank = new Bank(layer, null, x -> true);

			var dh = Change.delta(35.3f, 0.17338027f);
			var dq = Change.delta(30.998875f, 0.30947846f);
			var ba = Change.delta(45.386444f, 0.35185215f);
			var tph = Change.range(601.3737f, 594.1138f);

			// Go straight to Full Species Dynamics
			fpe.growUsingFullSpeciesDynamics(
					EasyMock.same(ba), EasyMock.same(dq), EasyMock.eq(tph.start(), 0.01f), EasyMock.eq(30.0f, 0.01f)
			);
			EasyMock.expectLastCall().once();

			em.replay();

			// Dynamics mode 0, full
			fpe.growSpecies(lps, 2025, ExecutionStep.ALL, bank, dh, dq, ba, tph, 30.0f, 31.0f, 400f, 0);

			em.verify();
		}

	}
}
