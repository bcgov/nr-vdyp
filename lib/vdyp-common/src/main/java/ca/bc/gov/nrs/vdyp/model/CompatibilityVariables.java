package ca.bc.gov.nrs.vdyp.model;

import java.util.Map;

public record CompatibilityVariables(
		MatrixMap2<UtilizationClass, VolumeVariable, Float> volume, Map<UtilizationClass, Float> basalArea,
		Map<UtilizationClass, Float> quadraticMeanDiameter, Map<UtilizationClassVariable, Float> primaryLayerSmall
) {
	/**
	 * Given species indexed arrays of each variable map, return a species indexed array of CompatibilityVariables
	 * objects.
	 *
	 * @param volume
	 * @param basalArea
	 * @param quadraticMeanDiameter
	 * @param primaryLayerSmall
	 * @return
	 */
	public static CompatibilityVariables[] fromArrays(
			MatrixMap2<UtilizationClass, VolumeVariable, Float>[] volume, Map<UtilizationClass, Float>[] basalArea,
			Map<UtilizationClass, Float>[] quadraticMeanDiameter,
			Map<UtilizationClassVariable, Float>[] primaryLayerSmall
	) {
		if (volume.length != basalArea.length || volume.length != quadraticMeanDiameter.length
				|| volume.length != primaryLayerSmall.length) {
			throw new IllegalArgumentException("Arrays must be the same length");
		}
		var result = new CompatibilityVariables[volume.length];

		for (int i = 0; i < result.length; i++) {
			result[i] = new CompatibilityVariables(
					volume[i], basalArea[i], quadraticMeanDiameter[i], primaryLayerSmall[i]
			);
		}

		return result;
	}
}