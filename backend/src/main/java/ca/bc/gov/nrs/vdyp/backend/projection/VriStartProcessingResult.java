package ca.bc.gov.nrs.vdyp.backend.projection;

public class VriStartProcessingResult {

	// VRIStart processing result codes:
	//
	// In the case of single stand processing with VRIPASS(4) set, behaves as follows:
	// -100 due to EOF, nothing to read
	// -99: unspecified reason.
	// other -ve value. Could not process the stand - see below
	// 0: stand was processed and written
	// +ve value: serious error
	//
	// -14 VRI_YNG +80 yrs nonmerch size
	// -13 Predict BA very small
	// -12 BEC missing or bad
	// -11 Vet layer lacks CC and (BA,T)
	// -10 HD-vet too low
	// -9 Missing CC
	// -8 Sum of PCTs not 100
	// -7 Missing BA or TPH, or DQ < 7.5
	// -6 Missing HD
	// -5 Missing age
	// -4 Missing SI
	// -3 Inadequate H-A-SI
	// -2 Logic error, PRIMFIND
	// -1 No primary layer
	// 0 OK
}
