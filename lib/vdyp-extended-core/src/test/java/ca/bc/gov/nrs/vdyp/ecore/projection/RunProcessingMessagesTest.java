package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;

class RunProcessingMessagesTest {

	@Test
	void includesEachApplicableMessageInOrder() {
		var params = new ValidatedParameters()
				.selectedExecutionOptions(EnumSet.of(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS));
		String messages = RunProcessingMessages.create(params);
		assertEquals(4, messages.lines().count());
		assertTrue(messages.startsWith("- I SUCCESS - CFS BIOMASS"));
		assertTrue(messages.contains("FORWARD GROW has been disabled."));
		assertTrue(messages.contains("BACK GROW has been disabled."));
		assertTrue(messages.endsWith("Substitution of Supplied BA/TPH as Projected values has been disabled.\n"));
	}

	@Test
	void omitsMessagesForEnabledFeatures() {
		var params = new ValidatedParameters().selectedExecutionOptions(
				EnumSet.of(
						ExecutionOption.FORWARD_GROW_ENABLED, ExecutionOption.BACK_GROW_ENABLED,
						ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION
				)
		);
		assertEquals("", RunProcessingMessages.create(params));
	}
}
