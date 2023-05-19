package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class CoefficientParserTest {
	
	@Test
	public void testParseSimple() throws Exception {
		
		var parser = new CoefficientParser();
		
		var is = TestUtils.makeStream("B1   A0 1  2.0028 -0.5343  1.3949 -0.3683 -0.3343  0.5699  0.2314  0.0528  0.2366 -0.3343  0.5076  0.5076  0.6680 -0.1353  1.2445 -0.4507");

		Map<String, Object> controlMap = new HashMap<>();
		
		Map<String, BecDefinition> becMap = new HashMap<>();
		becMap.put("B1", new BecDefinition("B1", Region.COASTAL, "Test BEC 1"));
		becMap.put("B2", new BecDefinition("B2", Region.COASTAL, "Test BEC 2"));
		
		controlMap.put(BecDefinitionParser.CONTROL_KEY, becMap);

		var result = parser.parse(is, controlMap);

	}
	
}
