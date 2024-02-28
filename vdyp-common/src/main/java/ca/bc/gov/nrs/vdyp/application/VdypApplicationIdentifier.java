package ca.bc.gov.nrs.vdyp.application;

/**
 * Identifies the VDYP applications. Matches the values used for the FORTRAN
 * implementation's JPROGRAM COMMON variable.
 *
 * @author Michael Junkin, Vivid Solutions
 */
@SuppressWarnings("java:S115")
public enum VdypApplicationIdentifier {
	
	FIPStart(1),
	VRIStart(3),
	VDYPForward(6),
	VDYPBack(7),
	VRIAdjust(8);
	
	private final int jProgramNumber;

	VdypApplicationIdentifier(int jProgramNumber)
	{
		this.jProgramNumber = jProgramNumber;
	}
	
	public int getJProgramNumber()
	{
		return jProgramNumber;
	}
}
