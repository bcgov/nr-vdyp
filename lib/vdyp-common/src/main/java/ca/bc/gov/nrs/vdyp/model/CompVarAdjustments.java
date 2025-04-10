package ca.bc.gov.nrs.vdyp.model;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

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
	private final MatrixMap2<VolumeVariable, UtilizationClass, Float> utilizationClassVolumeVariables = new MatrixMap2Impl<>(
			VolumeVariable.ALL, UtilizationClass.UTIL_CLASSES, (k1, k2) -> 1.0f
	);

	private float loreyHeightPrimary;
	private float loreyHeightOther;

	static {
		assert UtilizationClassVariable.BASAL_AREA.ordinal() == 0;
		assert UtilizationClassVariable.QUAD_MEAN_DIAMETER.ordinal() == 1;
		assert UtilizationClassVariable.LOREY_HEIGHT.ordinal() == 2;
		assert UtilizationClassVariable.WHOLE_STEM_VOLUME.ordinal() == 3;

		assert UtilizationClass.U75TO125.ordinal() == 2;
		assert UtilizationClass.U125TO175.ordinal() == 3;
		assert UtilizationClass.U175TO225.ordinal() == 4;
		assert UtilizationClass.OVER225.ordinal() == 5;

		assert VolumeVariable.WHOLE_STEM_VOL.ordinal() == 0;
		assert VolumeVariable.CLOSE_UTIL_VOL.ordinal() == 1;
		assert VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY.ordinal() == 2;
		assert VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE.ordinal() == 3;
	}

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

		for (UtilizationClassVariable ucv : UtilizationClassVariable.values()) {
			smallUtilizationClassVariables.put(ucv, values.get(SMALL_VARIABLE_START_INDEX + ucv.ordinal()));
		}

		for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
			utilizationClassBasalAreaVariables.put(uc, values.get(BA_ADJ_UC_START_INDEX + uc.ordinal() - 2));
		}

		for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
			utilizationClassQuadMeanDiameterVariables.put(uc, values.get(DQ_ADJ_UC_START_INDEX + uc.ordinal() - 2));
		}

		for (VolumeVariable vv : VolumeVariable.ALL) {
			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				utilizationClassVolumeVariables.put(vv, uc, values.get(ucOffsets.get(uc) + vv.ordinal()));
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

	public float getVolumeValue(UtilizationClass uc, VolumeVariable vv) {
		if (uc.ordinal() >= UtilizationClass.U75TO125.ordinal()) {
			return utilizationClassVolumeVariables.get(vv, uc);
		}

		throw new IllegalArgumentException(
				MessageFormat.format("getVolumeValue({0}, {1}) - UtilizationClass is invalid", uc, vv)
		);
	}
}
