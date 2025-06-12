package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.DebugOption;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;

public class TestLoggingParameters {

	@Test
	void testNullLoggingParameters() {
		LoggingParameters lparams1 = LoggingParameters.of(null);

		assertFalse(lparams1.doEnableDebugLogging());
		assertFalse(lparams1.doEnableErrorLogging());
		assertFalse(lparams1.doEnableProgressLogging());
		assertFalse(lparams1.doIncludeDebugEntryExit());
		assertFalse(lparams1.doIncludeDebugIndentBlocks());
		assertFalse(lparams1.doIncludeDebugRoutineNames());
		assertFalse(lparams1.doIncludeDebugTimestamps());
	}

	@Test
	void testLoggingParametersFromParameters() {
		Parameters p = new Parameters() //
				.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ENTRY_EXIT) //
				.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_INDENT_BLOCKS) //
				.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_ROUTINE_NAMES) //
				.addSelectedDebugOptionsItem(DebugOption.DO_INCLUDE_DEBUG_TIMESTAMPS) //
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_DEBUG_LOGGING) //
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_ERROR_LOGGING) //
				.addSelectedExecutionOptionsItem(ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);

		LoggingParameters lparams1 = LoggingParameters.of(p);

		assertTrue(lparams1.doEnableDebugLogging());
		assertTrue(lparams1.doEnableErrorLogging());
		assertTrue(lparams1.doEnableProgressLogging());
		assertTrue(lparams1.doIncludeDebugEntryExit());
		assertTrue(lparams1.doIncludeDebugIndentBlocks());
		assertTrue(lparams1.doIncludeDebugRoutineNames());
		assertTrue(lparams1.doIncludeDebugTimestamps());

		var lparams2 = lparams1.copy();

		assertTrue(lparams1.equals(lparams1));
		assertTrue(lparams1.equals(lparams2));
		assertFalse(lparams2.equals(new Object()));

		lparams2.setDoEnableDebugLogging(false);
		lparams2.setDoEnableErrorLogging(false);
		lparams2.setDoEnableProgressLogging(false);
		lparams2.setDoIncludeDebugEntryExit(false);
		lparams2.setDoIncludeDebugIndentBlocks(false);
		lparams2.setDoIncludeDebugRoutineNames(false);
		lparams2.setDoIncludeDebugTimestamps(false);

		assertFalse(lparams2.doEnableDebugLogging());
		assertFalse(lparams2.doEnableErrorLogging());
		assertFalse(lparams2.doEnableProgressLogging());
		assertFalse(lparams2.doIncludeDebugEntryExit());
		assertFalse(lparams2.doIncludeDebugIndentBlocks());
		assertFalse(lparams2.doIncludeDebugRoutineNames());
		assertFalse(lparams2.doIncludeDebugTimestamps());

		System.out.println(LoggingParameters.DEFAULT.toString());
	}

	@Test
	void testHashcode() {
		LoggingParameters lparam = LoggingParameters.DEFAULT.copy();

		assertEquals(lparam.hashCode(), LoggingParameters.DEFAULT.hashCode());

		lparam.setDoEnableDebugLogging(true);

		assertNotEquals(lparam.hashCode(), LoggingParameters.DEFAULT.hashCode());
	}
}
