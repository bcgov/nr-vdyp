package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.Coefficients;

/**
 * Parses a mapping from a Basal Area Group number to a list of eight coefficients. Each row contains 
 * <ol>
 * <li>(cols 0-2) int - Basal Area Group number</li>
 * <li>(cols 3-11, 12-20, 21-30, 31-39, 40-48, 49-57) float * 6 - coefficient list (9 characters)</li>
 * <li>(cols 58-63, 64-69) float * 2 - coefficient list (6 characters)</li>
 * </ol>
 * All lines are parsed. There is no provision for blank lines; all lines must have content.
 * <p>
 * The result of the parse is a map from a Basal Area Group number to a (zero-based) eight-element coefficient array.
 * <p>
 * Control index: 123
 * <p>
 * Example file: coe/REGDQL2.COE
 *
 * @author Michael Junkin, Vivid Solutions
 * @see ControlMapSubResourceParser
 */
public class DqGrowthEmpiricalLimitsParser implements ControlMapSubResourceParser<Map<Integer, Coefficients>> {
	
	public static final String CONTROL_KEY = "DQ_GROWTH_EMPIRICAL_LIMITS";
	
	public static final int MAX_BASAL_AREA_GROUP_ID = 40;
	
	public static final String BASAL_AREA_GROUP_ID_KEY = "BasalAreaGroupId";
	public static final String COEFFICIENTS_9_KEY = "Coefficients-9";
	public static final String COEFFICIENTS_6_KEY = "Coefficients-6";

	public DqGrowthEmpiricalLimitsParser() {
		
		this.lineParser = new LineParser() {
					@Override
					public boolean isStopLine(String line) {
						return line == null || line.trim().length() == 0;
					}
				}
			.value(3, BASAL_AREA_GROUP_ID_KEY, ValueParser.INTEGER)
			.multiValue(6, 9, COEFFICIENTS_9_KEY, ValueParser.FLOAT)
			.multiValue(2, 6, COEFFICIENTS_6_KEY, ValueParser.FLOAT);
	}

	private LineParser lineParser;

	@Override
	public Map<Integer, Coefficients> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {

		Map<Integer, Coefficients> result = new HashMap<>();
		
		lineParser.parse(is, result, (v, r) -> {
			var basalAreaGroupId = (Integer) v.get(BASAL_AREA_GROUP_ID_KEY);
			
			if (basalAreaGroupId < 1 || basalAreaGroupId > MAX_BASAL_AREA_GROUP_ID) {
				throw new ValueParseException("Basal Area group id " + basalAreaGroupId + " is out of range; expecting a value from 1 to " + basalAreaGroupId);
			}
			
			List<Float> coefficientList = new ArrayList<Float>();
			@SuppressWarnings("unchecked")
			var coefficient9List = (List<Float>) v.get(COEFFICIENTS_9_KEY);
			@SuppressWarnings("unchecked")
			var coefficient6List = (List<Float>) v.get(COEFFICIENTS_6_KEY);
			
			coefficientList.addAll(coefficient9List);
			coefficientList.addAll(coefficient6List);
			
			r.put(basalAreaGroupId, new Coefficients(coefficientList, 0));

			return r;
		}, control);

		return result;
	}

	@Override
	public String getControlKey()
	{
		return CONTROL_KEY;
	}
}