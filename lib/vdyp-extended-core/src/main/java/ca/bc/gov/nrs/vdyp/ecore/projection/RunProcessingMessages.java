package ca.bc.gov.nrs.vdyp.ecore.projection;

import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ReturnCode;

/** Provides projection-wide information for final error logs and report notes. */
public final class RunProcessingMessages {

	private RunProcessingMessages() {
	}

	public static String create(ValidatedParameters params) {
		StringBuilder messages = new StringBuilder();
		for (String message : messages(params)) {
			messages.append("- ").append(MessageSeverityCode.INFORMATION.getShortText()) //
					.append(" ").append(ReturnCode.SUCCESS.errorLogString).append(" - ") //
					.append(message).append('\n');
		}
		return messages.toString();
	}

	public static List<String> messages(ValidatedParameters params) {
		List<String> messages = new ArrayList<>();
		if (params.containsOption(ExecutionOption.DO_INCLUDE_PROJECTED_CFS_BIOMASS)) {
			messages.add(
					"CFS BIOMASS Output has been selected. Utilization Levels Pine and Hardwood - 12.5cm+; Others 17.5cm+ Processing Message"
			);
		}
		if (!params.containsOption(ExecutionOption.FORWARD_GROW_ENABLED)) {
			messages.add(
					"FORWARD GROW has been disabled. No projection will occur after the Reference Year for any polygon."
			);
		}
		if (!params.containsOption(ExecutionOption.BACK_GROW_ENABLED)) {
			messages.add(
					"BACK GROW has been disabled. No projection will occur prior to Reference Year for any polygon."
			);
		}
		if (!params.containsOption(ExecutionOption.DO_ALLOW_BA_AND_TPH_VALUE_SUBSTITUTION)) {
			messages.add("Substitution of Supplied BA/TPH as Projected values has been disabled.");
		}
		return List.copyOf(messages);
	}
}
