package ca.bc.gov.nrs.vdyp.backend.projection;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.DebugOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;

public class TestLoggingParameters {

	@Test
	void testNullLoggingParameters() {
		LoggingParameters lparams1 = LoggingParameters.of(null);

		Assert.assertFalse(lparams1.doEnableDebugLogging());
		Assert.assertFalse(lparams1.doEnableErrorLogging());
		Assert.assertFalse(lparams1.doEnableProgressLogging());
		Assert.assertFalse(lparams1.doIncludeDebugEntryExit());
		Assert.assertFalse(lparams1.doIncludeDebugIndentBlocks());
		Assert.assertFalse(lparams1.doIncludeDebugRoutineNames());
		Assert.assertFalse(lparams1.doIncludeDebugTimestamps());
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

		Assert.assertTrue(lparams1.doEnableDebugLogging());
		Assert.assertTrue(lparams1.doEnableErrorLogging());
		Assert.assertTrue(lparams1.doEnableProgressLogging());
		Assert.assertTrue(lparams1.doIncludeDebugEntryExit());
		Assert.assertTrue(lparams1.doIncludeDebugIndentBlocks());
		Assert.assertTrue(lparams1.doIncludeDebugRoutineNames());
		Assert.assertTrue(lparams1.doIncludeDebugTimestamps());

		var lparams2 = lparams1.copy();

		Assert.assertTrue(lparams1.equals(lparams1));
		Assert.assertTrue(lparams1.equals(lparams2));
		Assert.assertFalse(lparams2.equals(new Object()));

		lparams2.setDoEnableDebugLogging(false);
		lparams2.setDoEnableErrorLogging(false);
		lparams2.setDoEnableProgressLogging(false);
		lparams2.setDoIncludeDebugEntryExit(false);
		lparams2.setDoIncludeDebugIndentBlocks(false);
		lparams2.setDoIncludeDebugRoutineNames(false);
		lparams2.setDoIncludeDebugTimestamps(false);

		Assert.assertFalse(lparams2.doEnableDebugLogging());
		Assert.assertFalse(lparams2.doEnableErrorLogging());
		Assert.assertFalse(lparams2.doEnableProgressLogging());
		Assert.assertFalse(lparams2.doIncludeDebugEntryExit());
		Assert.assertFalse(lparams2.doIncludeDebugIndentBlocks());
		Assert.assertFalse(lparams2.doIncludeDebugRoutineNames());
		Assert.assertFalse(lparams2.doIncludeDebugTimestamps());

		System.out.println(LoggingParameters.DEFAULT.toString());
	}

	@Test
	void testHashcode() {
		LoggingParameters lparam = LoggingParameters.DEFAULT.copy();

		Assert.assertEquals(lparam.hashCode(), LoggingParameters.DEFAULT.hashCode());

		lparam.setDoEnableDebugLogging(true);

		Assert.assertNotEquals(lparam.hashCode(), LoggingParameters.DEFAULT.hashCode());
	}
}
