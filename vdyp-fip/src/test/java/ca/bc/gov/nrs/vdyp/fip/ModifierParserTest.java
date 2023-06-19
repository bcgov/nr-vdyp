package ca.bc.gov.nrs.vdyp.fip;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.FileResolver;

public class ModifierParserTest {

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testNoFilenameForControlFile() throws Exception {
		var parser = new ModifierParser();

		Map<String, Object> controlMap = new HashMap<>();
		controlMap.put(ModifierParser.CONTROL_KEY, Optional.empty());

		var fileResolver = new FileResolver() {

			@Override
			public InputStream resolve(String filename) throws IOException {
				fail("Should not call FileResolver::resolve");
				return null;
			}

			@Override
			public String toString(String filename) throws IOException {
				fail("Should not call FileResolver::toString");
				return filename;
			}
			
		};
		
		parser.modify(controlMap, fileResolver);
		
		assertThat(controlMap, Matchers.aMapWithSize(1));
		assertThat(controlMap, (Matcher) hasEntry(is(ModifierParser.CONTROL_KEY), notPresent()));
		
	}
	
}
