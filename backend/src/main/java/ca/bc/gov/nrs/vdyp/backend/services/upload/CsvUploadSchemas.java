package ca.bc.gov.nrs.vdyp.backend.services.upload;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvLayerRecordBean;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonRecordBean;

final class CsvUploadSchemas {
	private static final int LAYER_REQUIRED_COLUMN_COUNT = 38;

	private CsvUploadSchemas() {
	}

	static Optional<HeaderSchema> expectedHeaders(String fileSetTypeCode) {
		if (FileSetTypeCodeModel.POLYGON.equals(fileSetTypeCode)) {
			return Optional.of(HeaderSchema.exact(headersFor(HcsvPolygonRecordBean.class)));
		}
		if (FileSetTypeCodeModel.LAYER.equals(fileSetTypeCode)) {
			List<String> headers = headersFor(HcsvLayerRecordBean.class);
			return Optional.of(
					HeaderSchema.withOptionalTail(
							headers.subList(0, LAYER_REQUIRED_COLUMN_COUNT),
							new LinkedHashSet<>(headers.subList(LAYER_REQUIRED_COLUMN_COUNT, headers.size()))
					)
			);
		}
		return Optional.empty();
	}

	private static List<String> headersFor(Class<?> beanClass) {
		return Arrays.stream(beanClass.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(CsvBindByPosition.class))
				.sorted(Comparator.comparingInt(field -> field.getAnnotation(CsvBindByPosition.class).position()))
				.map(CsvUploadSchemas::csvName).toList();
	}

	private static String csvName(Field field) {
		CsvBindByName bindByName = field.getAnnotation(CsvBindByName.class);
		if (bindByName == null || bindByName.column() == null || bindByName.column().isBlank()) {
			return field.getName();
		}
		return bindByName.column();
	}

	record HeaderSchema(List<String> requiredPrefix, Set<String> allowedOptionalTail, boolean exact) {
		static HeaderSchema exact(List<String> headers) {
			return new HeaderSchema(headers, Set.of(), true);
		}

		static HeaderSchema withOptionalTail(List<String> requiredPrefix, Set<String> allowedOptionalTail) {
			return new HeaderSchema(requiredPrefix, allowedOptionalTail, false);
		}

		boolean matches(List<String> actualHeaders) {
			if (actualHeaders.size() < requiredPrefix.size()) {
				return false;
			}
			if (exact && actualHeaders.size() != requiredPrefix.size()) {
				return false;
			}
			for (int i = 0; i < requiredPrefix.size(); i++) {
				if (!requiredPrefix.get(i).equals(actualHeaders.get(i))) {
					return false;
				}
			}
			Set<String> seenOptional = new LinkedHashSet<>();
			for (int i = requiredPrefix.size(); i < actualHeaders.size(); i++) {
				String header = actualHeaders.get(i);
				if (!allowedOptionalTail.contains(header) || !seenOptional.add(header)) {
					return false;
				}
			}
			return true;
		}
	}
}
