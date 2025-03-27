package ca.bc.gov.nrs.vdyp.math;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class VdypMathTest {

	@Test
	void testClampInteger() {
		assertThat(VdypMath.clamp(5, 0, 10), equalTo(5));
		assertThat(VdypMath.clamp(-5, 0, 10), equalTo(0));
		assertThat(VdypMath.clamp(15, 0, 10), equalTo(10));
	}

	@Test
	void testClampDouble() {
		assertThat(VdypMath.clamp(5.1, 0.5, 10.5), equalTo(5.1));
		assertThat(VdypMath.clamp(-5, 0.5, 10.5), equalTo(0.5));
		assertThat(VdypMath.clamp(15.3, 0.5, 10.5), equalTo(10.5));
	}

	@Test
	void testMax3() {
		assertThat(VdypMath.max(5.1, 0.5, 10.5), equalTo(10.5));
		assertThat(VdypMath.max(15.1, 0.5, 10.5), equalTo(15.1));
		assertThat(VdypMath.max(5, 0, 10), equalTo(10));
		assertThat(VdypMath.max(15, 0, 10), equalTo(15));
	}

	@Test
	void testMax4() {
		assertThat(VdypMath.max(5.1, 0.5, 10.5, 15.5), equalTo(15.5));
		assertThat(VdypMath.max(15.1, 10.5, 0.5, 3.2), equalTo(15.1));
		assertThat(VdypMath.max(3, 5, 0, 10), equalTo(10));
		assertThat(VdypMath.max(20, 15, 10, 0), equalTo(20));
	}
}
