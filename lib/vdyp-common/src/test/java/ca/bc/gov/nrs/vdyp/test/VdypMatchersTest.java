package ca.bc.gov.nrs.vdyp.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class VdypMatchersTest {

	@Test
	void suppressed() {
		IOException noSuppressed = new IOException("noSuppressed");
		IOException isSuppressed1 = new IOException("isSuppressed1");
		IOException isSuppressed2 = new IOException("isSuppressed2");
		IOException has1Suppressed = new IOException("has1Suppressed");
		IOException has1and2Suppressed = new IOException("has1and2Suppressed");

		has1Suppressed.addSuppressed(isSuppressed1);

		has1and2Suppressed.addSuppressed(isSuppressed1);
		has1and2Suppressed.addSuppressed(isSuppressed2);

		var matchNone = VdypMatchers.suppresses();
		var match1 = VdypMatchers.suppresses(sameInstance(isSuppressed1));
		var match2 = VdypMatchers.suppresses(sameInstance(isSuppressed2));
		var match1and2 = VdypMatchers.suppresses(sameInstance(isSuppressed1), sameInstance(isSuppressed2));
		var match2and1 = VdypMatchers.suppresses(sameInstance(isSuppressed2), sameInstance(isSuppressed1));

		assertThat(noSuppressed, matchNone);
		assertThat(has1Suppressed, not(matchNone));
		assertThat(has1and2Suppressed, not(matchNone));

		assertThat(noSuppressed, not(match1));
		assertThat(has1Suppressed, match1);
		assertThat(has1and2Suppressed, not(match1));

		assertThat(noSuppressed, not(match2));
		assertThat(has1Suppressed, not(match2));
		assertThat(has1and2Suppressed, not(match2));

		assertThat(noSuppressed, not(match1and2));
		assertThat(has1Suppressed, not(match1and2));
		assertThat(has1and2Suppressed, match1and2);

		assertThat(noSuppressed, not(match1and2));
		assertThat(has1Suppressed, not(match1and2));
		assertThat(has1and2Suppressed, not(match2and1));

	}

}
