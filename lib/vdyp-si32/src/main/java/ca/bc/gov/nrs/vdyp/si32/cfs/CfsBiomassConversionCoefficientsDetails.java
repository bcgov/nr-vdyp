package ca.bc.gov.nrs.vdyp.si32.cfs;

import java.text.MessageFormat;
import java.util.Arrays;

// Consider a record with named values for better clarity. It saves almost nothing to have the params be an array of loats they are never different.
// We also don't need to access them based on enum offsets this
public record CfsBiomassConversionCoefficientsDetails(boolean containsData, float[] parms) {

	public static final CfsBiomassConversionCoefficientsDetails LIVE_EMPTY = new CfsBiomassConversionCoefficientsDetails(
			false,
			new float[] { -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f,
					-9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f,
					-9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f,
					-9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f,
					-9.00000000f, -9.00000000f }

	);

	public static final CfsBiomassConversionCoefficientsDetails DEAD_EMPTY = new CfsBiomassConversionCoefficientsDetails(
			false,
			new float[] { -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f, -9.00000000f,
					-9.00000000f, -9.00000000f, -9.00000000f }
	);

	public float getParm(CfsLiveConversionParams parm) {
		return parms[parm.getIndex()];
	}

	public float getParm(CfsDeadConversionParams parm) {
		return parms[parm.getIndex()];
	}

	@Override
	public String toString() {
		return MessageFormat.format("ContainsData: {0}; parms: {1}", containsData, Arrays.toString(parms));
	}

	@Override
	public int hashCode() {
		return Boolean.valueOf(containsData).hashCode() * 17 + Arrays.hashCode(parms);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CfsBiomassConversionCoefficientsDetails that) {
			if (this.containsData != that.containsData) {
				return false;
			}
			if (this.parms.length != that.parms.length) {
				return false;
			}
			for (int i = 0; i < this.parms.length; i++) {
				if (this.parms[i] != that.parms[i])
					return false;
			}

			return true;
		} else {
			return false;
		}
	}
}
