package ca.bc.gov.nrs.vdyp.test;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

public class VdypEasyMock {
	public static <T> T adapt(Matcher<T> match) {

		EasyMock.reportMatcher(new IArgumentMatcher() {

			@Override
			public boolean matches(Object argument) {
				return match.matches(argument);
			}

			@Override
			public void appendTo(StringBuffer buffer) {
				var desc = new StringDescription(buffer);
				match.describeTo(desc);
			}

		});
		return null;
	}

}
