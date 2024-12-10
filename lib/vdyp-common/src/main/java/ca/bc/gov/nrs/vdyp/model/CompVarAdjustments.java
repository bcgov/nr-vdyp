package ca.bc.gov.nrs.vdyp.model;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.variables.UtilizationClassVariable;

public class CompVarAdjustments {
	public static final int MIN_INDEX = 1;
	public static final int MAX_INDEX = 98;

	private static final int SMALL_VARIABLE_START_INDEX = 1;

	private static final int BA_ADJ_UC_START_INDEX = 5;
	private static final int DQ_ADJ_UC_START_INDEX = 15;

	private static final int LOREY_HEIGHT_PRIMARY_PARAM = 51;
	private static final int LOREY_HEIGHT_OTHER_PARAM = 52;

	private final Map<UtilizationClassVariable, Float> smallUtilizationClassVariables = new HashMap<>();
	private final Map<UtilizationClass, Float> utilizationClassBasalAreaVariables = new HashMap<>();
	private final Map<UtilizationClass, Float> utilizationClassQuadMeanDiameterVariables = new HashMap<>();
	private final MatrixMap2<UtilizationClassVariable, UtilizationClass, Float> utilizationClassVolumeVariables = new MatrixMap2Impl<>(
			VdypCompatibilityVariables.VOLUME_UTILIZATION_VARIABLES, UtilizationClass.UTIL_CLASSES, (k1, k2) -> 1.0f
	);

	private float loreyHeightPrimary;
	private float loreyHeightOther;

	private static final Map<UtilizationClassVariable, Integer> SMALL_INDEX = Utils
			.constMap(UtilizationClassVariable.class, map -> {
				map.put(UtilizationClassVariable.BASAL_AREA, 0);
				map.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 1);
				map.put(UtilizationClassVariable.LOREY_HEIGHT, 2);
				map.put(UtilizationClassVariable.WHOLE_STEM_VOL, 3);
			});

	private static final Map<UtilizationClass, Integer> CLASS_INDEX = Utils.constMap(UtilizationClass.class, map -> {
		map.put(UtilizationClass.U75TO125, 0);
		map.put(UtilizationClass.U125TO175, 1);
		map.put(UtilizationClass.U175TO225, 2);
		map.put(UtilizationClass.OVER225, 3);
	});

	private static final Map<UtilizationClassVariable, Integer> VOLUME_INDEX = Utils
			.constMap(UtilizationClassVariable.class, map -> {
				map.put(UtilizationClassVariable.WHOLE_STEM_VOL, 0);
				map.put(UtilizationClassVariable.CLOSE_UTIL_VOL, 1);
				map.put(UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY, 2);
				map.put(UtilizationClassVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, 3);
			});

	private final static Map<UtilizationClass, Integer> ucOffsets = new HashMap<>();
	private final static Map<Integer, Float> defaultValuesMap = new HashMap<>();

	static {
		ucOffsets.put(UtilizationClass.U75TO125, 11);
		ucOffsets.put(UtilizationClass.U125TO175, 21);
		ucOffsets.put(UtilizationClass.U175TO225, 31);
		ucOffsets.put(UtilizationClass.OVER225, 41);

		for (int i = MIN_INDEX; i <= MAX_INDEX; i++)
			defaultValuesMap.put(i, 1.0f);
	}

	/**
	 * Constructs a default instance, one in which all index values are 1.0f. See rd_e028.for.
	 */
	public CompVarAdjustments() {
		this(defaultValuesMap);
	}

	/**
	 * Constructs an instance from a data set.
	 */
	public CompVarAdjustments(Map<Integer, Float> values) {

		for (UtilizationClassVariable ucv : VdypCompatibilityVariables.SMALL_UTILIZATION_VARIABLES) {
			smallUtilizationClassVariables.put(ucv, values.get(SMALL_VARIABLE_START_INDEX + SMALL_INDEX.get(ucv)));
		}

		for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
			utilizationClassBasalAreaVariables.put(uc, values.get(BA_ADJ_UC_START_INDEX + CLASS_INDEX.get(uc)));
		}

		for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
			utilizationClassQuadMeanDiameterVariables.put(uc, values.get(DQ_ADJ_UC_START_INDEX + CLASS_INDEX.get(uc)));
		}

		for (UtilizationClassVariable vv : VdypCompatibilityVariables.VOLUME_UTILIZATION_VARIABLES) {
			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				utilizationClassVolumeVariables.put(vv, uc, values.get(ucOffsets.get(uc) + VOLUME_INDEX.get(vv)));
			}
		}

		loreyHeightPrimary = values.get(LOREY_HEIGHT_PRIMARY_PARAM);
		loreyHeightOther = values.get(LOREY_HEIGHT_OTHER_PARAM);
	}

	public float getLoreyHeightPrimaryParam() {
		return loreyHeightPrimary;
	}

	public float getLoreyHeightOther() {
		return loreyHeightOther;
	}

	public float getValue(UtilizationClass uc, UtilizationClassVariable v) {
		if (UtilizationClass.SMALL.equals(uc)) {
			return smallUtilizationClassVariables.get(v);
		} else if (!UtilizationClass.ALL.equals(uc)) {
			switch (v) {
			case BASAL_AREA:
				return utilizationClassBasalAreaVariables.get(uc);
			case QUAD_MEAN_DIAMETER:
				return utilizationClassQuadMeanDiameterVariables.get(uc);
			default:
				break;
			}
		}

		throw new IllegalArgumentException(
				MessageFormat.format(
						"getValue({}, {}) - combination of UtilizationClass and UtilizationClassVariable is invalid",
						uc, v
				)
		);
	}

	public float getVolumeValue(UtilizationClass uc, UtilizationClassVariable vv) {
		if (uc.ordinal() >= UtilizationClass.U75TO125.ordinal()) {
			return utilizationClassVolumeVariables.get(vv, uc);
		}

		throw new IllegalArgumentException(
				MessageFormat.format("getVolumeValue({}, {}) - UtilizationClass is invalid", uc, vv)
		);
	}
}
