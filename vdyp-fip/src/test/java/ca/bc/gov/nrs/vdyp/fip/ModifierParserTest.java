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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.FileResolver;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ModifierParserTest {

	
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
	@Test
	public void testMissingControlFile() throws Exception {
		var parser = new ModifierParser();

		Map<String, Object> controlMap = new HashMap<>();
		controlMap.put(ModifierParser.CONTROL_KEY, Optional.of("testFilename"));

		var fileResolver = new FileResolver() {

			@Override
			public InputStream resolve(String filename) throws IOException {
				assertThat(filename, is("testFilename"));
				
				throw new IOException();
			}

			@Override
			public String toString(String filename) throws IOException {
				fail("Should not call FileResolver::toString");
				return filename;
			}
			
		};
		
		
		var ex = Assertions.assertThrows(IOException.class, ()->parser.modify(controlMap, fileResolver));
		
		assertThat(ex, Matchers.notNullValue());
		
	}
	
}
