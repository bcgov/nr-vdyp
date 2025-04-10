package ca.bc.gov.nrs.vdyp.io.parse.coe;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;

/**
 * Volume Equation Number parser. Each line of this file contains:
 * <ol>
 * <li>(cols 0-1) a species code</li>
 * <li>(cols 3-6) a BEC Zone identifier</li>
 * <li>(cols 8-10) a volume group identifier (1-180)</li>
 * </ol>
 * Lines that are empty or contain only blanks in columns 0-1 and 3-6 are considered blank. The file must contain one
 * entry for each combination of Species and BEC Zone (excluding hidden BEC Zones) and thus there must be 16 * 13 values
 * in the file (at this time.)
 * <p>
 * The result is a map from Species x Visible BEC Zones to integers.
 * <p>
 * FIP Control index: 020
 * <p>
 * Example: coe/VGRPDEF1.DAT
 *
 * @see EquationGroupParser
 * @author Kevin Smith, Vivid Solutions
 */
public class VolumeEquationGroupParser extends EquationGroupParser {

	private static final Logger logger = LoggerFactory.getLogger(VolumeEquationGroupParser.class);

	private static final Set<String> HIDDEN_BECS = Set.of("BG");

	public VolumeEquationGroupParser() {
		super(3);
		this.setHiddenBecs(Arrays.asList("BG"));
	}

	@Override
	public MatrixMap2<String, String, Integer> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {

		var matrixMap = super.parse(is, control);

		var dimensions = matrixMap.getDimensions();
		for (var d1 : dimensions.get(0)) {
			for (var d2 : dimensions.get(1)) {
				var genusKey = (String) d1;
				var becKey = (String) d2;
				if (!HIDDEN_BECS.contains(becKey)) {
					var e = matrixMap.get(genusKey, becKey);
					if (e == 10) {
						logger.info(
								"Volume group 10 is no longer supported; replacing with 11 for genus {}, bec {}",
								genusKey, becKey
						);
						matrixMap.put(genusKey, becKey, 11);
					}
				}
			}
		}

		return matrixMap;
	}

	@Override
	public ControlKey getControlKey() {
		return ControlKey.VOLUME_EQN_GROUPS;
	}

}
