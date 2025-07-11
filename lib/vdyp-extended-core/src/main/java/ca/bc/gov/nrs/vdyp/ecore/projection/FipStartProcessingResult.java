package ca.bc.gov.nrs.vdyp.ecore.projection;

import java.util.Set;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.exceptions.StandProcessingException;

public class FipStartProcessingResult {

	// FIPStart processing result codes:
	//
	// In the case of single stand processing with FIPPASS(4) = 1, behaves as follows:
	// -100 EOF, nothing to read
	// -99 Processing was O.K.
	// -ve (other) Could not process the stand (see below)
	// 0 Unknown condition, or not applicable
	// >0 Serious error. Set IER to this.
	//
	// Process return codes for FIPPASS(10)
	// -1 There is NO Layer 1 (with a H > 0 and CC > 0)
	// -2 Incorrect layer codes.
	// -3 Incorrect layer codes, or program logic error
	// -4 Height of layer 1 < the requirement (was 7.2 m. See control file note)
	// -5 Height of Overstory less than the requirment (10 m, or SEQ 197 on ctr)
	// -6 BH AGE < 0.5 yrs
	// -8 % volumes for Primary layer do not sum to 100
	// -9 % volumes for Overstory layer do not sum to 100
	// -10 Primary Layer YTBH invalid
	// -11 Primary Layer SI invalid
	// -12 Predicted basal area of primary layer <= 0.05 (can't be used)
	// -13 Predicted basal area does not meet minimum requirements.

	private static Set<Integer> fipStartRetryUsingVriStartRunCodes = Set.of(-4, -6, -12, -13);

	public static boolean doRetryUsingVriStart(StandProcessingException e) {
		return e.getIpassCode(VdypApplicationIdentifier.FIP_START)
				.map(n -> fipStartRetryUsingVriStartRunCodes.contains(n)).orElse(false);
	}
}
