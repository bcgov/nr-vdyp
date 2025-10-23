package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.CalculationEngineCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CalculationEngineCodeModel;

public class TestCalculationEngineResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new CalculationEngineResourceAssembler().toEntity(null)).isNull();
		assertThat(new CalculationEngineResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(
				Arguments.of("Test", "Test Description", 1), Arguments.of(null, null, null),
				Arguments.of(null, "Test Description", 1), Arguments.of("Test", null, 1),
				Arguments.of("Test", "Test Description", null), Arguments.of("OVERLYLONGCODE", "Test Description", 1)
		);
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(String code, String description, Integer displayOrder) {
		CalculationEngineTestData data = CalculationEngineTestData.builder().withCode(code).withDescription(description)
				.withDisplayOrder(displayOrder);

		CalculationEngineCodeModel model = data.buildModel();
		CalculationEngineCodeEntity entity = data.buildEntity();
		CalculationEngineResourceAssembler assembler = new CalculationEngineResourceAssembler();
		CalculationEngineCodeModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(String code, String description, Integer displayOrder) {
		CalculationEngineTestData data = CalculationEngineTestData.builder().withCode(code).withDescription(description)
				.withDisplayOrder(displayOrder);

		CalculationEngineCodeModel model = data.buildModel();
		CalculationEngineCodeEntity entity = data.buildEntity();
		CalculationEngineResourceAssembler assembler = new CalculationEngineResourceAssembler();
		CalculationEngineCodeEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	private static final class CalculationEngineTestData {
		private String code = null;
		private String description;
		private Integer displayOrder;

		public static CalculationEngineTestData builder() {
			return new CalculationEngineTestData();
		}

		public CalculationEngineTestData withCode(String code) {
			this.code = code;
			return this;
		}

		public CalculationEngineTestData withDescription(String description) {
			this.description = description;
			return this;
		}

		public CalculationEngineTestData withDisplayOrder(Integer displayOrder) {
			this.displayOrder = displayOrder;
			return this;
		}

		public CalculationEngineCodeEntity buildEntity() {
			CalculationEngineCodeEntity data = new CalculationEngineCodeEntity();
			data.setCalculationEngineCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}

		public CalculationEngineCodeModel buildModel() {
			CalculationEngineCodeModel data = new CalculationEngineCodeModel();
			data.setCalculationEngineCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}
	}

}
