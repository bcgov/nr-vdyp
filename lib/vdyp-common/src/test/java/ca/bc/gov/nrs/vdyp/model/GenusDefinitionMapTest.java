package ca.bc.gov.nrs.vdyp.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class GenusDefinitionMapTest {

	private Map<String, Object> controlMap = new HashMap<>();

	@BeforeEach
	void before() {

		TestUtils.populateControlMapGenusReal(controlMap);
	}

	@Test
	void test() {
		var gdMap = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());

		assertThat(gdMap.contains("AC"), is(true));
		assertThat(gdMap.getByAlias("AC"), hasProperty("alias", is("AC")));
		assertThat(gdMap.getByIndex(3), hasProperty("alias", is("B")));
		assertThat(gdMap.getIndexByAlias("B"), is(3));
		assertThat(gdMap.getNGenera(), is(16));

		assertThat(gdMap.getGenera().size(), is(16));
		assertThat(gdMap.getGenera(), Matchers.hasItem(new GenusDefinition("B", 3, "Balsam")));
		assertThat(gdMap.getAllGeneraAliases(), Matchers.hasItems("B", "C"));
	}
}
