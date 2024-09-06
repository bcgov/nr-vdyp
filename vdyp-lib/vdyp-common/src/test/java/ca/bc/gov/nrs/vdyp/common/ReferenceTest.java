package ca.bc.gov.nrs.vdyp.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class ReferenceTest {

	@Test
	void testReference() {

		var reference = new Reference<Integer>(3);
		assertThat(reference.get(), Matchers.is(Integer.valueOf(3)));
		assertThat(reference.isPresent(), Matchers.is(true));
		reference.set(4);
		assertThat(reference.get(), Matchers.is(Integer.valueOf(4)));

		var reference2 = new Reference<Integer>();
		assertThat(reference2.isPresent(), Matchers.is(false));
		assertThrows(IllegalStateException.class, () -> reference2.get());
	}
}
