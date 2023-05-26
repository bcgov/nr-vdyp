package ca.bc.gov.nrs.vdyp.io.parse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.SP0Definition;

public class BecDefinitionParserTest {

	@Test
	public void testParse() throws Exception {
		var parser = new BecDefinitionParser();

		var result = parser.parse(ControlFileParserTest.class, "coe/Becdef.dat", Collections.emptyMap());

		assertThat(
				result,
				hasEntry(
						equalTo("AT"),
						allOf(
								hasProperty("alias", equalTo("AT")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("Alpine Tundra"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("BG"),
						allOf(
								hasProperty("alias", equalTo("BG")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("Bunchgrass"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("BWBS"),
						allOf(
								hasProperty("alias", equalTo("BWBS")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("Boreal White and Black Spruce"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("CDF"),
						allOf(
								hasProperty("alias", equalTo("CDF")), hasProperty("region", equalTo(Region.COASTAL)),
								hasProperty("name", equalTo("Coastal Dougfir"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("CWH"),
						allOf(
								hasProperty("alias", equalTo("CWH")), hasProperty("region", equalTo(Region.COASTAL)),
								hasProperty("name", equalTo("Coastal Western Hemlock"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("ESSF"),
						allOf(
								hasProperty("alias", equalTo("ESSF")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("Englemann Sruce -SubAlpine Fir"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("ICH"),
						allOf(
								hasProperty("alias", equalTo("ICH")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("Interior Cedar-Hemlock"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("IDF"),
						allOf(
								hasProperty("alias", equalTo("IDF")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("Interior DougFir"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("MH"),
						allOf(
								hasProperty("alias", equalTo("MH")), hasProperty("region", equalTo(Region.COASTAL)),
								hasProperty("name", equalTo("Mountain Hemlock"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("MS"),
						allOf(
								hasProperty("alias", equalTo("MS")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("Montane Spruce"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("PP"),
						allOf(
								hasProperty("alias", equalTo("PP")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("Ponderosa Pine"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("SBPS"),
						allOf(
								hasProperty("alias", equalTo("SBPS")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("SubBoreal Pine-Spruce"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("SBS"),
						allOf(
								hasProperty("alias", equalTo("SBS")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("SubBoreal Spruce"))
						)
				)
		);
		assertThat(
				result,
				hasEntry(
						equalTo("SWB"),
						allOf(
								hasProperty("alias", equalTo("SWB")), hasProperty("region", equalTo(Region.INTERIOR)),
								hasProperty("name", equalTo("Spruce-Willow-Birch"))
						)
				)
		);
	}

	/**
	 * Add a mock control map entry for BEC parse results with species "B1" and "B2"
	 * for Coastal and Interior Regions respectively
	 */
	public static void populateControlMap(Map<String, Object> controlMap) {
		populateControlMap(controlMap, "B1", "B2");
	}

	/**
	 * Add a mock control map entry for SP0 parse results. Alternates assigning to
	 * Coastal and Interior regions, starting with Coastal.
	 */
	public static void populateControlMap(Map<String, Object> controlMap, String... aliases) {

		Map<String, BecDefinition> map = new HashMap<>();

		int i = 0;
		for (var alias : aliases) {
			map.put(alias, new BecDefinition(alias, Region.values()[i % 2], "Test " + alias));
			i++;
		}

		controlMap.put(BecDefinitionParser.CONTROL_KEY, map);
	}

}
