package ca.bc.gov.nrs.vdyp.back;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.back.processing_state.BackProcessingState;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class BackProcessingStateTest {

	Map<String, Object> rawControlMap;

	@BeforeEach
	void setup() {

		rawControlMap = TestUtils.loadControlMap(new ProcessingControlParser(), Path.of("VDYP.CTR"));

	}

	@Nested
	class GetSpeciesGroupIndex {
		@Test
		void testOne() throws ProcessingException {
			BackProcessingState state = new BackProcessingState(rawControlMap);
			var polygon = VdypPolygon.build(pb -> {
				pb.controlMap(rawControlMap);
				pb.polygonIdentifier("092P037  72999905UNK 2011");
				pb.biogeoclimaticZone("MS");
				pb.forestInventoryZone("");
				pb.percentAvailable(61f);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.addSpecies(sb -> {
						sb.speciesGroup("PL");
						sb.percentGenus(100);
					});
					lb.primaryGenus("PL");
					lb.empiricalRelationshipParameterIndex(118);
				});
			});
			state.setPolygon(polygon);

			assertThat("B", state.getSpeciesGroupIndex("PL"), equalTo(1));

		}

		@Test
		void testMultiple() throws ProcessingException {
			BackProcessingState state = new BackProcessingState(rawControlMap);
			var polygon = VdypPolygon.build(pb -> {
				pb.controlMap(rawControlMap);
				pb.polygonIdentifier("092P037  72999905UNK 2011");
				pb.biogeoclimaticZone("MS");
				pb.forestInventoryZone("");
				pb.percentAvailable(61f);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.percentGenus(10);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("PL");
						sb.percentGenus(70);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.percentGenus(20);
					});
					lb.primaryGenus("PL");
					lb.empiricalRelationshipParameterIndex(118);
				});
			});
			state.setPolygon(polygon);

			assertThat("B", state.getSpeciesGroupIndex("B"), equalTo(1));
			assertThat("B", state.getSpeciesGroupIndex("PL"), equalTo(2));
			assertThat("B", state.getSpeciesGroupIndex("S"), equalTo(3));

		}

		@Test
		void testNotPresent() throws ProcessingException {
			BackProcessingState state = new BackProcessingState(rawControlMap);
			var polygon = VdypPolygon.build(pb -> {
				pb.controlMap(rawControlMap);
				pb.polygonIdentifier("092P037  72999905UNK 2011");
				pb.biogeoclimaticZone("MS");
				pb.forestInventoryZone("");
				pb.percentAvailable(61f);
				pb.addLayer(lb -> {
					lb.layerType(LayerType.PRIMARY);
					lb.addSpecies(sb -> {
						sb.speciesGroup("B");
						sb.percentGenus(10);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("PL");
						sb.percentGenus(70);
					});
					lb.addSpecies(sb -> {
						sb.speciesGroup("S");
						sb.percentGenus(20);
					});
					lb.primaryGenus("PL");
					lb.empiricalRelationshipParameterIndex(118);
				});
			});
			state.setPolygon(polygon);

			var ex = assertThrows(IllegalArgumentException.class, () -> state.getSpeciesGroupIndex("MB"));
			assertThat(ex, hasMessage(containsString("MB")));
		}
	}
}
