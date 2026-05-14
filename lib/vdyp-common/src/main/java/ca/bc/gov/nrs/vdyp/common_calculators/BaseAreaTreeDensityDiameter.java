package ca.bc.gov.nrs.vdyp.common_calculators;

/**
 * Converts between trees per hectare and quad mean diameter in a given base area
 */
public class BaseAreaTreeDensityDiameter {

	private BaseAreaTreeDensityDiameter() {
	}

	/**
	 * π/4/10⁴ = π/(4 * 10⁴)
	 */
	public static final double PI_40K = (float) (Math.PI / 40_000);

	/**
	 * FT_BD - return an estimate of the number of trees per hectare based on a given base area (a) and quadratic mean
	 * diameter (q) according to the formula
	 * <p>
	 * a / PI_40K / q**2
	 * <p>
	 *
	 * @param baseArea              the base area (m**2 / hectare)
	 * @param quadraticMeanDiameter the quadratic mean diameter (cm / tree)
	 * @return as described. If baseArea or quadraticMeanDiameter is not positive, 0 is returned.
	 */
	public static float treesPerHectare(float baseArea, float quadraticMeanDiameter) {
		return (float) treesPerHectare(baseArea, (double) quadraticMeanDiameter);
	}

	public static double treesPerHectare(double baseArea, double quadraticMeanDiameter) {
		if (baseArea > 0 && quadraticMeanDiameter > 0f) {
			return baseArea / PI_40K / (quadraticMeanDiameter * quadraticMeanDiameter);
		}

		return 0f;
	}

	/**
	 * FD_BT - return an estimate of the quadratic mean diameter based on a given number of trees per hectare (t) and
	 * base area (a) according to the formula
	 * <p>
	 * (b / t / PI_40K)**1/2
	 * <p>
	 *
	 * @param baseArea        the base area value
	 * @param treesPerHectare the trees per hectare value
	 * @return as described. If baseArea or treesPerHectare is Nan, not positive or more than 1,000,000, 0.0 is
	 *         returned.
	 */
	public static float quadMeanDiameter(float baseArea, float treesPerHectare) {
		return (float) quadMeanDiameter(baseArea, (double) treesPerHectare);
	}

	public static double quadMeanDiameter(double baseArea, double treesPerHectare) {
		if (baseArea > 1e6f || treesPerHectare > 1e6f || Double.isNaN(baseArea) || Double.isNaN(treesPerHectare)) {
			return 0f;
		} else if (baseArea > 0f && treesPerHectare > 0f) {
			return Math.sqrt(baseArea / treesPerHectare / PI_40K);
		}
		return 0f;

	}

	/**
	 * FB_DT - return an estimate of the basal area based on a given quad-mean-diameter value (q) and a
	 * trees-per-hectare value according to the formula
	 * <p>
	 * q**2 * PI_40K * t
	 * <p>
	 *
	 * @param quadraticMeanDiameter the quadratic mean diameter (cm / tree)
	 * @param treesPerHectare       the trees per hectare value
	 * @return as described. If either parameter is NaN, 0f is returned.
	 */
	public static float basalArea(float quadraticMeanDiameter, float treesPerHectare) {
		return (float) basalArea(quadraticMeanDiameter, (double) treesPerHectare);
	}

	public static double basalArea(double quadraticMeanDiameter, double treesPerHectare) {

		if (Double.isNaN(quadraticMeanDiameter) || Double.isNaN(treesPerHectare)) {
			return 0f;
		} else {
			// qmd is diameter in cm (per tree); qmd**2 is in cm**2. Multiplying by pi/4 converts
			// to area in cm**2. Dividing by 10000 converts into m**2. Finally, multiplying
			// by trees-per-hectare takes the per-tree area and converts it into a per-hectare
			// area - that is, the basal area per hectare.

			return quadraticMeanDiameter * quadraticMeanDiameter * PI_40K * treesPerHectare;
		}
	}
}
