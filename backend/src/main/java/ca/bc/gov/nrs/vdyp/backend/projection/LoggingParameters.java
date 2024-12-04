package ca.bc.gov.nrs.vdyp.backend.projection;

import java.util.ArrayList;
import java.util.Objects;

import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.DebugOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;

/**
 * The class represents a Parameter class instance whose logging related parameters have been validated and presents
 * those fields in an internal representation (i.e., as booleans) rather than as Strings as they are in Parameter.
 */
public class LoggingParameters {

	public static final LoggingParameters DEFAULT;

	public boolean doEnableErrorLogging;
	public boolean doEnableProgressLogging;
	public boolean doEnableDebugLogging;

	public boolean doIncludeDebugTimestamps;
	public boolean doIncludeDebugRoutineNames;
	public boolean doIncludeDebugEntryExit;
	public boolean doIncludeDebugIndentBlocks;

	private LoggingParameters(Parameters params) {

		if (params == null) {
			params = new Parameters() //
					.selectedExecutionOptions(new ArrayList<Parameters.ExecutionOption>()) //
					.selectedDebugOptions(new ArrayList<Parameters.DebugOption>());
		}

		if (params.getSelectedExecutionOptions()
				.contains(ExecutionOption.DO_ENABLE_ERROR_LOGGING.toString())) {
			doEnableErrorLogging = true;
		} else {
			doEnableErrorLogging = LoggingParameters.DEFAULT.doEnableErrorLogging;
		}

		if (params.getSelectedExecutionOptions()
				.contains(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING.toString())) {
			doEnableProgressLogging = true;
		} else {
			doEnableProgressLogging = LoggingParameters.DEFAULT.doEnableProgressLogging;
		}

		if (params.getSelectedExecutionOptions()
				.contains(ExecutionOption.DO_ENABLE_DEBUG_LOGGING.toString())) {
			doEnableDebugLogging = true;
		} else {
			doEnableDebugLogging = LoggingParameters.DEFAULT.doEnableDebugLogging;
		}

		if (params.getSelectedDebugOptions()
				.contains(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT.toString())) {
			doIncludeDebugEntryExit = true;
		} else {
			doIncludeDebugEntryExit = LoggingParameters.DEFAULT.doIncludeDebugEntryExit;
		}

		if (params.getSelectedDebugOptions()
				.contains(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS.toString())) {
			doIncludeDebugIndentBlocks = true;
		} else {
			doIncludeDebugIndentBlocks = LoggingParameters.DEFAULT.doIncludeDebugIndentBlocks;
		}

		if (params.getSelectedDebugOptions()
				.contains(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES.toString())) {
			doIncludeDebugRoutineNames = true;
		} else {
			doIncludeDebugRoutineNames = LoggingParameters.DEFAULT.doIncludeDebugRoutineNames;
		}

		if (params.getSelectedDebugOptions()
				.contains(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS.toString())) {
			doIncludeDebugTimestamps = true;
		} else {
			doIncludeDebugTimestamps = LoggingParameters.DEFAULT.doIncludeDebugTimestamps;
		}
	}

	private LoggingParameters() {
	}

	public static LoggingParameters of(Parameters params) {
		return new LoggingParameters(params);
	}

	public boolean isDoEnableErrorLogging() {
		return doEnableErrorLogging;
	}

	public boolean isDoEnableProgressLogging() {
		return doEnableProgressLogging;
	}

	public boolean isDoEnableDebugLogging() {
		return doEnableDebugLogging;
	}

	public boolean isDoIncludeDebugTimestamps() {
		return doIncludeDebugTimestamps;
	}

	public boolean isDoIncludeDebugRoutineNames() {
		return doIncludeDebugRoutineNames;
	}

	public boolean isDoIncludeDebugEntryExit() {
		return doIncludeDebugEntryExit;
	}

	public boolean isDoIncludeDebugIndentBlocks() {
		return doIncludeDebugIndentBlocks;
	}

	void setDoEnableErrorLogging(boolean doEnableErrorLogging) {
		this.doEnableErrorLogging = doEnableErrorLogging;
	}

	void setDoEnableProgressLogging(boolean doEnableProgressLogging) {
		this.doEnableProgressLogging = doEnableProgressLogging;
	}

	void setDoEnableDebugLogging(boolean doEnableDebugLogging) {
		this.doEnableDebugLogging = doEnableDebugLogging;
	}

	void setDoIncludeDebugTimestamps(boolean doIncludeDebugTimestamps) {
		this.doIncludeDebugTimestamps = doIncludeDebugTimestamps;
	}

	void setDoIncludeDebugRoutineNames(boolean doIncludeDebugRoutineNames) {
		this.doIncludeDebugRoutineNames = doIncludeDebugRoutineNames;
	}

	void setDoIncludeDebugEntryExit(boolean doIncludeDebugEntryExit) {
		this.doIncludeDebugEntryExit = doIncludeDebugEntryExit;
	}

	void setDoIncludeDebugIndentBlocks(boolean doIncludeDebugIndentBlocks) {
		this.doIncludeDebugIndentBlocks = doIncludeDebugIndentBlocks;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		LoggingParameters that = (LoggingParameters) o;
		return this.doEnableDebugLogging == that.doEnableDebugLogging
				&& this.doEnableErrorLogging == that.doEnableErrorLogging
				&& this.doEnableProgressLogging == that.doEnableProgressLogging
				&& this.doIncludeDebugEntryExit == that.doIncludeDebugEntryExit
				&& this.doIncludeDebugIndentBlocks == that.doIncludeDebugIndentBlocks
				&& this.doIncludeDebugRoutineNames == that.doIncludeDebugRoutineNames
				&& this.doIncludeDebugTimestamps == that.doIncludeDebugTimestamps;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				doEnableErrorLogging, doEnableProgressLogging, doEnableDebugLogging, doIncludeDebugTimestamps,
				doIncludeDebugRoutineNames, doIncludeDebugEntryExit, doIncludeDebugIndentBlocks
		);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class Parameters {\n");

		sb.append("    doEnableErrorLogging: ").append(toIndentedString(doEnableErrorLogging)).append("\n");
		sb.append("    doEnableProgressLogging: ").append(toIndentedString(doEnableProgressLogging)).append("\n");
		sb.append("    doEnableDebugLogging: ").append(toIndentedString(doEnableDebugLogging)).append("\n");
		sb.append("    doIncludeDebugTimestamps: ").append(toIndentedString(doIncludeDebugTimestamps)).append("\n");
		sb.append("    doIncludeDebugRoutineNames: ").append(toIndentedString(doIncludeDebugRoutineNames)).append("\n");
		sb.append("    doIncludeDebugEntryExit: ").append(toIndentedString(doIncludeDebugEntryExit)).append("\n");
		sb.append("    doIncludeDebugIndentBlocks: ").append(toIndentedString(doIncludeDebugIndentBlocks)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces (except the first line).
	 */
	private String toIndentedString(Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}

	static {
		DEFAULT = new LoggingParameters();

		DEFAULT.doEnableErrorLogging = false;
		DEFAULT.doEnableProgressLogging = false;
		DEFAULT.doEnableDebugLogging = false;
		DEFAULT.doIncludeDebugTimestamps = false;
		DEFAULT.doIncludeDebugRoutineNames = false;
		DEFAULT.doIncludeDebugEntryExit = false;
		DEFAULT.doIncludeDebugIndentBlocks = false;
	}
}
