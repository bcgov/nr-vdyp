package ca.bc.gov.nrs.vdyp.ecore.projection.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvException;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.LayerValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind;

class CsvExceptionQueueTest {

	@Test
	void errorsRetainSeparateRowsWithinAFeatureEvenWithoutFeatureIds() {
		var queue = new CsvExceptionQueue("Polygon");
		assertNull(queue.handleException(csvError(2, "first")));
		assertNull(queue.handleException(csvError(3, "second")));
		assertNull(queue.peek().featureId());
		var first = queue.peek();
		assertEquals("Polygon CSV row 2: first", first.messages().get(0).getMessage());
		assertEquals(first, queue.poll());
		assertEquals("Polygon CSV row 3: second", queue.poll().messages().get(0).getMessage());
		assertNull(queue.poll());
		assertNull(queue.peek());
	}

	@Test
	void pollingOneRowDoesNotConsumeOtherRowsForTheSameFeature() {
		var errors = new CsvExceptionQueue("CSV");
		errors.handleException(layerError(10, "1", "first"));
		errors.handleException(layerError(10, "2", "second"));
		errors.handleException(layerError(20, "1", "future"));
		assertEquals(
				"Polygon 10 Layer 1 - E ERROR_INVALIDSITEINFO - first", errors.poll().messages().get(0).getMessage()
		);
		assertEquals(
				List.of("Polygon 10 Layer 2 - E ERROR_INVALIDSITEINFO - second"),
				errors.drain(10).stream().map(ValidationMessage::getMessage).toList()
		);
		assertEquals(20L, errors.peek().featureId());
		assertEquals(
				"Polygon 20 Layer 1 - E ERROR_INVALIDSITEINFO - future", errors.poll().messages().get(0).getMessage()
		);
		assertNull(errors.peek());
		assertNull(errors.poll());
		assertEquals(List.of(), errors.drainAll());
	}

	@Test
	void errorsAreGroupedByFeatureAndConsumedOnlyOnce() {
		var errors = new CsvExceptionQueue("Layer");
		assertNull(errors.handleException(layerError(10, "1", "first")));
		assertNull(errors.handleException(layerError(20, "1", "future")));
		assertNull(errors.handleException(layerError(10, "2", "second")));
		assertNull(errors.handleException(csvError(4, "unassignable")));
		assertEquals(
				List.of(
						"Polygon 10 Layer 1 - E ERROR_INVALIDSITEINFO - first",
						"Polygon 10 Layer 2 - E ERROR_INVALIDSITEINFO - second"
				), errors.drain(10).stream().map(ValidationMessage::getMessage).toList()
		);
		assertEquals(List.of(), errors.drain(10));
		assertEquals(
				List.of("Polygon 20 Layer 1 - E ERROR_INVALIDSITEINFO - future", "Layer CSV row 4: unassignable"),
				errors.drainAll().stream().map(ValidationMessage::getMessage).toList()
		);
		assertEquals(List.of(), errors.drainAll());
	}

	private static CsvException layerError(long featureId, String layerId, String message) {
		return new CsvConstraintViolationException(
				new LayerValidationException(
						featureId, layerId, List.of(new ValidationMessage(ValidationMessageKind.GENERIC, message))
				)
		);
	}

	private static CsvException csvError(long line, String message) {
		var exception = new CsvException(message);
		exception.setLineNumber(line);
		return exception;
	}
}
