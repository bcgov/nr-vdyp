package ca.bc.gov.nrs.vdyp.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

	@Nested
	class IndexedProperty {

		interface TestInterface {
			String testProperty(int i);
		}

		interface TestInterfaceNotIndexed {
			String testProperty();
		}

		interface TestInterfaceNoProperty {
		}

		IMocksControl em;

		@BeforeEach
		void setup() {
			em = EasyMock.createControl();
		}

		@Nested
		class HasIndexedPropertyAt {

			TestInterface toMatch;
			Matcher<Object> valueMatcher;
			Matcher<Object> mismatchDelegate;

			@BeforeEach
			void setup() {
				toMatch = em.createMock("toMatch", TestInterface.class);
				valueMatcher = em.createMock("valueMatcher", Matcher.class);
				mismatchDelegate = new CustomMatcher<Object>("") {

					@Override
					public boolean matches(Object actual) {
						fail();
						return false;
					}

					@Override
					public void describeMismatch(Object item, Description description) {
						description.appendText("delegated mismatch");
					}

				};

			}

			@ParameterizedTest
			@ValueSource(ints = { 0, -1, 1, 100 })
			void testMatchAt(int i) {
				final String result = "Result " + i;

				expect(toMatch.testProperty(0)).andStubReturn(result);
				expect(valueMatcher.matches(result)).andStubReturn(true);

				em.replay();

				var matcher = VdypMatchers.hasIndexedPropertyAt("testProperty", 0, valueMatcher);
				var matches = matcher.matches(toMatch);
				assertTrue(matches, "Matches");

				em.verify();
			}

			@ParameterizedTest
			@ValueSource(ints = { 0, -1, 1, 100 })
			void testMismatchAt(int i) {
				final String result = "Result " + i;

				expect(toMatch.testProperty(0)).andStubReturn(result);
				expect(valueMatcher.matches(result)).andStubReturn(false);

				valueMatcher.describeMismatch(eq(result), anyObject(Description.class));
				expectLastCall().andStubDelegateTo(mismatchDelegate);

				em.replay();

				var matcher = VdypMatchers.hasIndexedPropertyAt("testProperty", 0, valueMatcher);
				var matches = matcher.matches(toMatch);
				assertFalse(matches, "Matches");

				Description description = new StringDescription();
				matcher.describeMismatch(toMatch, description);
				assertThat(description.toString(), equalTo("entry <0> of \"testProperty\" delegated mismatch"));

				em.verify();
			}

			@Test
			void testNoProperty() {

				var toMatchNoProp = em.createMock(TestInterfaceNoProperty.class);

				em.replay();

				var matcher = VdypMatchers.hasIndexedPropertyAt("testProperty", 0, valueMatcher);
				var matches = matcher.matches(toMatchNoProp);
				assertFalse(matches, "Matches");

				Description description = new StringDescription();
				matcher.describeMismatch(toMatchNoProp, description);
				assertThat(description.toString(), equalTo("did not have indexed property \"testProperty\""));

				em.verify();
			}

			@Test
			void testNotIndexed() {

				var toMatchNoIndex = em.createMock(TestInterfaceNotIndexed.class);

				em.replay();

				var matcher = VdypMatchers.hasIndexedPropertyAt("testProperty", 0, valueMatcher);
				var matches = matcher.matches(toMatchNoIndex);
				assertFalse(matches, "Matches");

				Description description = new StringDescription();
				matcher.describeMismatch(toMatchNoIndex, description);
				assertThat(description.toString(), equalTo("did not have indexed property \"testProperty\""));

				em.verify();
			}
		}

		@Nested
		class HasIndexedPropertyFrom {

			Matcher<Object> mismatchDelegate;

			@BeforeEach
			void setup() {
				mismatchDelegate = new CustomMatcher<Object>("") {

					@Override
					public boolean matches(Object actual) {
						fail();
						return false;
					}

					@Override
					public void describeMismatch(Object item, Description description) {
						description.appendText("delegated mismatch");
					}

				};

			}

			@ParameterizedTest
			@ValueSource(ints = { 0, -1, 1, 100 })
			void testZeroEntriesMatches(int offset) {
				TestInterface toMatch = em.createMock("toMatch", TestInterface.class);

				em.replay();

				var matcher = VdypMatchers.hasIndexedPropertyFrom("testProperty", offset);
				var matches = matcher.matches(toMatch);
				assertTrue(matches, "Matches");

				em.verify();
			}

			@ParameterizedTest
			@ValueSource(ints = { 0, -1, 1, 100 })
			void testOneEntryMatches(int offset) {
				TestInterface toMatch = em.createMock("toMatch", TestInterface.class);
				Matcher<Object> valueMatcher = em.createMock("valueMatcher", Matcher.class);

				final String result = "Result " + offset;

				expect(toMatch.testProperty(offset)).andStubReturn(result);
				expect(valueMatcher.matches(result)).andStubReturn(true);

				em.replay();

				var matcher = VdypMatchers.hasIndexedPropertyFrom("testProperty", offset, valueMatcher);
				var matches = matcher.matches(toMatch);
				assertTrue(matches, "Matches");

				em.verify();
			}

			@ParameterizedTest
			@ValueSource(ints = { 0, -1, 1, 100 })
			void testTwoEntriesMatches(int offset) {
				TestInterface toMatch = em.createMock("toMatch", TestInterface.class);
				Matcher<Object> valueMatcher0 = em.createMock("valueMatcher0", Matcher.class);
				Matcher<Object> valueMatcher1 = em.createMock("valueMatcher1", Matcher.class);

				final String result0 = "Result " + offset;
				final String result1 = "Result " + offset + 1;

				expect(toMatch.testProperty(offset)).andStubReturn(result0);
				expect(valueMatcher0.matches(result0)).andStubReturn(true);

				expect(toMatch.testProperty(offset + 1)).andStubReturn(result1);
				expect(valueMatcher1.matches(result1)).andStubReturn(true);

				em.replay();

				var matcher = VdypMatchers.hasIndexedPropertyFrom("testProperty", offset, valueMatcher0, valueMatcher1);
				var matches = matcher.matches(toMatch);
				assertTrue(matches, "Matches");

				em.verify();
			}

			@ParameterizedTest
			@CsvSource({ "0, 0", "0, 1", "-1, 0", "-1, 1", "1, 0", "1, 1", "100, 0", "100, 1" })
			void testOneEntryFails(int offset, int i) {
				TestInterface toMatch = em.createMock("toMatch", TestInterface.class);
				Matcher<Object> valueMatcher0 = em.createMock("valueMatcher0", Matcher.class);
				Matcher<Object> valueMatcher1 = em.createMock("valueMatcher1", Matcher.class);

				final String result0 = "Result " + offset + 0;
				final String result1 = "Result " + offset + 1;

				expect(toMatch.testProperty(offset)).andStubReturn(result0);
				expect(valueMatcher0.matches(result0)).andStubReturn(i != 0);
				expect(toMatch.testProperty(offset + 1)).andStubReturn(result1);
				expect(valueMatcher1.matches(result1)).andStubReturn(i != 1);

				(i == 0 ? valueMatcher0 : valueMatcher1)
						.describeMismatch(eq(i == 0 ? result0 : result1), anyObject(Description.class));
				expectLastCall().andStubDelegateTo(mismatchDelegate);

				em.replay();

				var matcher = VdypMatchers.hasIndexedPropertyFrom("testProperty", offset, valueMatcher0, valueMatcher1);
				var matches = matcher.matches(toMatch);
				assertFalse(matches, "Matches");

				Description description = new StringDescription();
				matcher.describeMismatch(toMatch, description);
				assertThat(
						description.toString(),
						equalTo("entry <" + (offset + i) + "> of \"testProperty\" delegated mismatch")
				);

				em.verify();
			}

		}
	}
}
