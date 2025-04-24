package ca.bc.gov.nrs.vdyp.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class GenusDefinitionTest {

	@Test
	void testSimpleDefinition() {
		GenusDefinition g1a = new GenusDefinition("A", 1, "A");
		GenusDefinition g1b = new GenusDefinition("A", 1, "A");
		GenusDefinition g2 = new GenusDefinition("B", 1, "B");
		GenusDefinition g3 = new GenusDefinition("A", 2, "A");

		assertThat(g1a.equals(g1a), is(true));
		assertThat(g1b.equals(g1a), is(true));
		assertThat(g2.equals(g1a), is(false));
		assertThat(g3.equals(g1a), is(false));
		assertThat(g1a.hashCode() == g1a.hashCode(), is(true));
		assertThat(g1b.hashCode() == g1a.hashCode(), is(true));
		assertThat(g2.hashCode() == g1a.hashCode(), is(false));
		assertThat(g3.hashCode() == g1a.hashCode(), is(false));
	}
}
