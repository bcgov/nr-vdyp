package ca.bc.gov.nrs.vdyp.backend.projection;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.Set;

public class ProcessingResult {

	public static final int RETURN_CODE_SUCCESS = 0;

	public static final int RUN_CODE_NULL_PROCESSING_RUN_CODE = 0;
	public static final int RUN_CODE_FIP_RETRY_USING_VRI_START = -2;
	public static final int RUN_CODE_SUCCESS = -99;

	public static final ProcessingResult PROCESSING_RESULT_NULL = new ProcessingResult(0, 0);
	public static final ProcessingResult PROCESSING_RESULT_SUCCESS = new ProcessingResult(
			RETURN_CODE_SUCCESS, RUN_CODE_SUCCESS
	);

	private int resultCode;
	private Optional<Integer> runCode;

	public ProcessingResult(int resultCode, Optional<Integer> runCode) {
		this.resultCode = resultCode;
		this.runCode = runCode;
	}

	public ProcessingResult(int resultCode, int runCode) {
		this.resultCode = resultCode;
		this.runCode = Optional.of(runCode);
	}

	public ProcessingResult(int resultCode) {
		this.resultCode = resultCode;
		this.runCode = Optional.empty();
	}

	// FIPStart processing result codes:
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

	public boolean doRetryUsingVriStart() {
		return runCode.isPresent() ? fipStartRetryUsingVriStartRunCodes.contains(runCode.get()) : false;
	}

	public int getResultCode() {
		return resultCode;
	}

	public Optional<Integer> getRunCode() {
		return runCode;
	}

	@Override
	public String toString() {
		return MessageFormat
				.format("res: {0}; run: {1}", resultCode, runCode.isPresent() ? runCode.get() : "<not supplied>");
	}
}
