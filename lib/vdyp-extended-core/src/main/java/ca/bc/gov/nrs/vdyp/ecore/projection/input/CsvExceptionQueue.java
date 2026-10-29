package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvException;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.ProjectionValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;

/**
 * Validation errors collected by the single-threaded HCSV iterators, consumed once by the polygon stream. Handlers
 * return null because the error is stored here rather than in OpenCSV's captured-exception list.
 */
final class CsvExceptionQueue implements CsvExceptionHandler {

	private final String streamName;
	private final Map<Long, Deque<Failure>> failuresByFeature = new LinkedHashMap<>();

	CsvExceptionQueue(String streamName) {
		this.streamName = streamName;
	}

	record Failure(Long featureId, List<ValidationMessage> messages) {
		static Failure from(CsvException exception, String streamName) {
			if (exception instanceof CsvConstraintViolationException constraint
					&& constraint.getSourceObject() instanceof ProjectionValidationException validation) {
				// Exceptions without a context prefix use 0 for an unknown feature ID.
				Long featureId = validation.getContextPrefix() != null ? validation.getFeatureID() : null;
				return new Failure(featureId, contextualMessages(validation));
			}
			var message = new ValidationMessage(
					ValidationMessageKind.GENERIC,
					streamName + " CSV row " + exception.getLineNumber() + ": " + exception.getMessage()
			);
			return new Failure(null, List.of(message));
		}
	}

	@Override
	public CsvException handleException(CsvException exception) {
		var failure = Failure.from(exception, streamName);
		failuresByFeature.computeIfAbsent(failure.featureId(), id -> new ArrayDeque<>()).addLast(failure);
		return null;
	}

	Failure peek() {
		var features = failuresByFeature.values().iterator();
		return features.hasNext() ? features.next().peekFirst() : null;
	}

	/** Consume one rejected row, preserving separate failures even when the feature ID is unknown. */
	Failure poll() {
		var features = failuresByFeature.values().iterator();
		if (!features.hasNext()) {
			return null;
		}
		var failures = features.next();
		var failure = failures.removeFirst();
		if (failures.isEmpty()) {
			features.remove();
		}
		return failure;
	}

	List<ValidationMessage> drain(long featureId) {
		var failures = failuresByFeature.remove(featureId);
		return failures == null ? List.of()
				: failures.stream().flatMap(failure -> failure.messages().stream()).toList();
	}

	List<ValidationMessage> drainAll() {
		var messages = failuresByFeature.values().stream().flatMap(Deque::stream)
				.flatMap(failure -> failure.messages().stream()).toList();
		failuresByFeature.clear();
		return messages;
	}

	/** Preserve each source's context before merging messages from different polygons or layers. */
	static List<ValidationMessage> contextualMessages(AbstractProjectionRequestException exception) {
		String prefix = exception.getContextPrefix();
		if (prefix == null) {
			return exception.getValidationMessages();
		}
		return exception.getValidationMessages().stream()
				.map(
						message -> new ValidationMessage(
								ValidationMessageKind.GENERIC,
								message.getErrorLogMessage(prefix)
						)
				).toList();
	}
}
