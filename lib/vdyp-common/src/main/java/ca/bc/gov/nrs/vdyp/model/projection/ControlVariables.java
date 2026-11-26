package ca.bc.gov.nrs.vdyp.model.projection;

import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;

public abstract class ControlVariables {

	private static final int DEFAULT_CONTROL_VARIABLE_VALUE = 0;

	private final int[] controlVariableValues = new int[10];
	private final int maximum;
	private boolean readOnly = false;

	protected ControlVariables(Integer[] controlVariableValues, int maximum) throws ValueParseException {
		int index = 0;
		this.maximum = maximum;
		if (controlVariableValues != null) {
			for (; index < Math.min(controlVariableValues.length, maximum); index++)
				this.controlVariableValues[index] = controlVariableValues[index];
		}

		for (; index < maximum; index++)
			this.controlVariableValues[index] = DEFAULT_CONTROL_VARIABLE_VALUE;

		validate();
	}

	protected abstract void validate() throws ValueParseException;

	public int getControlVariable(int elementNumber) {

		if (elementNumber < 1 || elementNumber > maximum) {
			throw new IllegalArgumentException(
					"Element number (" + elementNumber + ") is out of range - must be from 1 to " + maximum
			);
		}

		return controlVariableValues[elementNumber - 1];
	}

	/**
	 * Explicitly set a control variable value. To be used by unit tests only.
	 *
	 * @param elementNumber the variable to set
	 * @param value         its new value
	 * @throws ValueParseException
	 */
	public void setControlVariable(int elementNumber, int value) {
		if (readOnly) {
			throw new UnsupportedOperationException(
					"These control variables are shared between runs and are read only; control variable "
							+ elementNumber + " can not be changed"
			);
		}
		controlVariableValues[elementNumber - 1] = value;
	}

	/**
	 * Make these control variables read only, so that {@link #setControlVariable} throws
	 * {@link UnsupportedOperationException}. Used when the control variables are shared between runs. They can not be
	 * made modifiable again.
	 */
	public void makeReadOnly() {
		readOnly = true;
	}

	/**
	 * @return whether {@link #makeReadOnly} has been called
	 */
	public boolean isReadOnly() {
		return readOnly;
	}
}
