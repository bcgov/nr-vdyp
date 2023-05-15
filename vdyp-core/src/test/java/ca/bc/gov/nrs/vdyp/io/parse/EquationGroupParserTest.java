package ca.bc.gov.nrs.vdyp.io.parse;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.causedBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;


import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.SP0Definition;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

@SuppressWarnings("unused")
public class EquationGroupParserTest {
	
	@Test
	public void testParse() throws Exception {
		var parser = new EquationGroupParser();
		
		var controlMap = makeControlMap();
		
		var is = makeStream("S1 B1   001");
		var result = parser.parse(is, Collections.unmodifiableMap(controlMap));
		
		assertThat(result, hasEntry(is("S1"), hasEntry(is("B1"), is(1))));
	}
	
	@Test
	public void testSP0MustExist() throws Exception {
		var parser = new EquationGroupParser();
		
		var controlMap = makeControlMap();
		
		var is = makeStream("SX B1   001");
		
		ResourceParseException ex1 = Assertions.assertThrows(ResourceParseException.class, ()->parser.parse(is, Collections.unmodifiableMap(controlMap)));
		
		assertThat(ex1, hasProperty("message", stringContainsInOrder("line 1", "SX", "SP0")));
		assertThat(ex1, hasProperty("line", is(1)));
		assertThat(ex1, causedBy(hasProperty("value", is("SX"))));
	}
	
	@Test
	public void testBecMustExist() throws Exception {
		var parser = new EquationGroupParser();
		
		var controlMap = makeControlMap();
		
		var is = makeStream("S1 BX   001");
		
		ResourceParseException ex1 = Assertions.assertThrows(ResourceParseException.class, ()->parser.parse(is, Collections.unmodifiableMap(controlMap)));
		
		assertThat(ex1, hasProperty("message", stringContainsInOrder("line 1", "BX", "BEC")));
		assertThat(ex1, hasProperty("line", is(1)));
		assertThat(ex1, causedBy(hasProperty("value", is("BX"))));
	}
	
	@Test
	public void testParseOvewrite() throws Exception {
		// Original Fortran allows subsequent entries to overwrite old ones so don't validate against that

		var parser = new EquationGroupParser();
		
		var controlMap = makeControlMap();
		
		var is = makeStream("S1 B1   001", "S1 B1   002");
		var result = parser.parse(is, Collections.unmodifiableMap(controlMap));
		
		assertThat(result, hasEntry(is("S1"), hasEntry(is("B1"), is(2))));
	}
	
	@Test
	public void testParseMultiple() throws Exception {
		// Original Fortran allows subsequent entries to overwrite old ones so don't validate against that

		var parser = new EquationGroupParser();
		
		var controlMap = makeControlMap();
		
		var is = makeStream("S1 B1   001", "S1 B2   002", "S2 B1   001", "S2 B3   002", "S2 B4   003");
		var result = parser.parse(is, Collections.unmodifiableMap(controlMap));
		
		assertThat(result, hasEntry(is("S1"), hasEntry(is("B1"), is(1))));
		assertThat(result, hasEntry(is("S1"), hasEntry(is("B2"), is(2))));
		assertThat(result, hasEntry(is("S2"), hasEntry(is("B1"), is(1))));
		assertThat(result, hasEntry(is("S2"), hasEntry(is("B3"), is(2))));
		assertThat(result, hasEntry(is("S2"), hasEntry(is("B4"), is(3))));
	}

	private HashMap<String, Object> makeControlMap() {
		var controlMap = new HashMap<String, Object>();
		
		var becMap = new HashMap<String, BecDefinition>();
		var sp0List = new ArrayList<SP0Definition>();
		
		becMap.put("B1", new BecDefinition("B1", Region.COASTAL, "Test BEC 1"));
		becMap.put("B2", new BecDefinition("B2", Region.INTERIOR, "Test BEC 2"));
		becMap.put("B3", new BecDefinition("B3", Region.COASTAL, "Test BEC 3"));
		becMap.put("B4", new BecDefinition("B4", Region.INTERIOR, "Test BEC 4"));
		
		sp0List.add(new SP0Definition("S1", Optional.empty(), "Test SP0 1"));
		sp0List.add(new SP0Definition("S2", Optional.empty(), "Test SP0 2"));
		
		controlMap.put(BecDefinitionParser.CONTROL_KEY, Collections.unmodifiableMap(becMap));
		controlMap.put(SP0DefinitionParser.CONTROL_KEY, Collections.unmodifiableList(sp0List));
		return controlMap;
	}
	
	private InputStream makeStream(String...lines) {
		return new ByteArrayInputStream(String.join("\r\n", lines).getBytes());
	}
}
