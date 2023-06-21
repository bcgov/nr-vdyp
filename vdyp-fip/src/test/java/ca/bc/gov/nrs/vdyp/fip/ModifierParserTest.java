package ca.bc.gov.nrs.vdyp.fip;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasSpecificEntry;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.mmAll;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.mmDimensions;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
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
import ca.bc.gov.nrs.vdyp.io.parse.SP0DefinitionParserTest;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ModifierParserTest {

	
	@Test
	public void testNoFilenameForControlFile() throws Exception {
		var parser = new ModifierParser();

		Map<String, Object> controlMap = new HashMap<>();
		controlMap.put(ModifierParser.CONTROL_KEY, Optional.empty());
		SP0DefinitionParserTest.populateControlMap(controlMap);

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
		
		assertThat(controlMap, (Matcher) hasSpecificEntry(ModifierParser.CONTROL_KEY, notPresent()));
		assertThat(controlMap, (Matcher) hasSpecificEntry(ModifierParser.CONTROL_KEY_MOD200_BA, mmDimensions(contains("S1", "S2"), contains(Region.values()))));
		assertThat(controlMap, (Matcher) hasSpecificEntry(ModifierParser.CONTROL_KEY_MOD200_BA, mmAll(is(1.0f))));
		assertThat(controlMap, (Matcher) hasSpecificEntry(ModifierParser.CONTROL_KEY_MOD200_DQ, mmDimensions(contains("S1", "S2"), contains(Region.values()))));
		assertThat(controlMap, (Matcher) hasSpecificEntry(ModifierParser.CONTROL_KEY_MOD200_DQ, mmAll(is(1.0f))));
		
	}
	
	@Test
	public void testMissingControlFile() throws Exception {
		var parser = new ModifierParser();

		Map<String, Object> controlMap = new HashMap<>();
		controlMap.put(ModifierParser.CONTROL_KEY, Optional.of("testFilename"));
		SP0DefinitionParserTest.populateControlMap(controlMap);

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
	
	@Test
	public void testLoadEmptyFile() throws Exception {
		var parser = new ModifierParser();

		Map<String, Object> controlMap = new HashMap<>();
		controlMap.put(ModifierParser.CONTROL_KEY, Optional.of("testFilename"));
		SP0DefinitionParserTest.populateControlMap(controlMap);
		
		var is = TestUtils.makeStream();
		
		var fileResolver = new FileResolver() {

			@Override
			public InputStream resolve(String filename) throws IOException {
				assertThat(filename, is("testFilename"));
				
				return is;
			}

			@Override
			public String toString(String filename) throws IOException {
				return filename;
			}
			
		};
		
		
		parser.modify(controlMap, fileResolver);
		
		assertThat(controlMap, (Matcher) hasSpecificEntry(ModifierParser.CONTROL_KEY, present(is("testFilename"))));
		assertThat(controlMap, (Matcher) hasSpecificEntry(ModifierParser.CONTROL_KEY_MOD200_BA, mmDimensions(contains("S1", "S2"), contains(Region.values()))));
		assertThat(controlMap, (Matcher) hasSpecificEntry(ModifierParser.CONTROL_KEY_MOD200_BA, mmAll(is(1.0f))));
		assertThat(controlMap, (Matcher) hasSpecificEntry(ModifierParser.CONTROL_KEY_MOD200_DQ, mmDimensions(contains("S1", "S2"), contains(Region.values()))));
		assertThat(controlMap, (Matcher) hasSpecificEntry(ModifierParser.CONTROL_KEY_MOD200_DQ, mmAll(is(1.0f))));
		
	}
	
}
