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

	private boolean doEnableErrorLogging;
	private boolean doEnableProgressLogging;
	private boolean doEnableDebugLogging;

	private boolean doIncludeDebugTimestamps;
	private boolean doIncludeDebugRoutineNames;
	private boolean doIncludeDebugEntryExit;
	private boolean doIncludeDebugIndentBlocks;

	private LoggingParameters(Parameters params) {

		if (params == null) {
			params = new Parameters() //
					.selectedExecutionOptions(new ArrayList<Parameters.ExecutionOption>()) //
					.selectedDebugOptions(new ArrayList<Parameters.DebugOption>());
		}

		if (params.getSelectedExecutionOptions().contains(ExecutionOption.DO_ENABLE_ERROR_LOGGING.toString())) {
			doEnableErrorLogging = true;
		} else {
			doEnableErrorLogging = LoggingParameters.DEFAULT.doEnableErrorLogging;
		}

		if (params.getSelectedExecutionOptions().contains(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING.toString())) {
			doEnableProgressLogging = true;
		} else {
			doEnableProgressLogging = LoggingParameters.DEFAULT.doEnableProgressLogging;
		}

		if (params.getSelectedExecutionOptions().contains(ExecutionOption.DO_ENABLE_DEBUG_LOGGING.toString())) {
			doEnableDebugLogging = true;
		} else {
			doEnableDebugLogging = LoggingParameters.DEFAULT.doEnableDebugLogging;
		}

		if (params.getSelectedDebugOptions().contains(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT.toString())) {
			doIncludeDebugEntryExit = true;
		} else {
			doIncludeDebugEntryExit = LoggingParameters.DEFAULT.doIncludeDebugEntryExit;
		}

		if (params.getSelectedDebugOptions().contains(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS.toString())) {
			doIncludeDebugIndentBlocks = true;
		} else {
			doIncludeDebugIndentBlocks = LoggingParameters.DEFAULT.doIncludeDebugIndentBlocks;
		}

		if (params.getSelectedDebugOptions().contains(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES.toString())) {
			doIncludeDebugRoutineNames = true;
		} else {
			doIncludeDebugRoutineNames = LoggingParameters.DEFAULT.doIncludeDebugRoutineNames;
		}

		if (params.getSelectedDebugOptions().contains(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS.toString())) {
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

	public LoggingParameters copy() {
		var copy = new LoggingParameters();

		copy.doEnableErrorLogging = doEnableErrorLogging;
		copy.doEnableProgressLogging = doEnableProgressLogging;
		copy.doEnableDebugLogging = doEnableDebugLogging;

		copy.doIncludeDebugTimestamps = doIncludeDebugTimestamps;
		copy.doIncludeDebugRoutineNames = doIncludeDebugRoutineNames;
		copy.doIncludeDebugEntryExit = doIncludeDebugEntryExit;
		copy.doIncludeDebugIndentBlocks = doIncludeDebugIndentBlocks;

		return copy;
	}

	public boolean doEnableErrorLogging() {
		return doEnableErrorLogging;
	}

	public boolean doEnableProgressLogging() {
		return doEnableProgressLogging;
	}

	public boolean doEnableDebugLogging() {
		return doEnableDebugLogging;
	}

	public boolean doIncludeDebugTimestamps() {
		return doIncludeDebugTimestamps;
	}

	public boolean doIncludeDebugRoutineNames() {
		return doIncludeDebugRoutineNames;
	}

	public boolean doIncludeDebugEntryExit() {
		return doIncludeDebugEntryExit;
	}

	public boolean doIncludeDebugIndentBlocks() {
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
		} else if (o instanceof LoggingParameters that) {
			return this.doEnableDebugLogging == that.doEnableDebugLogging
					&& this.doEnableErrorLogging == that.doEnableErrorLogging
					&& this.doEnableProgressLogging == that.doEnableProgressLogging
					&& this.doIncludeDebugEntryExit == that.doIncludeDebugEntryExit
					&& this.doIncludeDebugIndentBlocks == that.doIncludeDebugIndentBlocks
					&& this.doIncludeDebugRoutineNames == that.doIncludeDebugRoutineNames
					&& this.doIncludeDebugTimestamps == that.doIncludeDebugTimestamps;
		} else {
			return false;
		}
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
		sb.append("class LoggingParameters {\n");
		sb.append("    doEnableErrorLogging: ").append(doEnableErrorLogging).append("\n");
		sb.append("    doEnableProgressLogging: ").append(doEnableProgressLogging).append("\n");
		sb.append("    doEnableDebugLogging: ").append(doEnableDebugLogging).append("\n");
		sb.append("    doIncludeDebugTimestamps: ").append(doIncludeDebugTimestamps).append("\n");
		sb.append("    doIncludeDebugRoutineNames: ").append(doIncludeDebugRoutineNames).append("\n");
		sb.append("    doIncludeDebugEntryExit: ").append(doIncludeDebugEntryExit).append("\n");
		sb.append("    doIncludeDebugIndentBlocks: ").append(doIncludeDebugIndentBlocks).append("\n");
		sb.append("}");
		return sb.toString();
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
