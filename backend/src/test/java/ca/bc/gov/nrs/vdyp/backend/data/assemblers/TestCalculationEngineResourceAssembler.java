package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.CalculationEngineCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.CalculationEngineCodeModel;

class TestCalculationEngineResourceAssembler {
	@Test
	void testNull() {
		assertThat(new CalculationEngineResourceAssembler().toEntity(null)).isNull();
		assertThat(new CalculationEngineResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(
				Arguments.of("Test", "Test Description", BigDecimal.ONE), Arguments.of(null, null, null),
				Arguments.of(null, "Test Description", BigDecimal.ONE), Arguments.of("Test", null, BigDecimal.ONE),
				Arguments.of("Test", "Test Description", null),
				Arguments.of("OVERLYLONGCODE", "Test Description", BigDecimal.ONE)
		);
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(String code, String description, BigDecimal displayOrder) {
		CalculationEngineTestData data = CalculationEngineTestData.builder().code(code).description(description)
				.displayOrder(displayOrder);

		CalculationEngineCodeModel model = data.buildModel();
		CalculationEngineCodeEntity entity = data.buildEntity();
		CalculationEngineResourceAssembler assembler = new CalculationEngineResourceAssembler();
		CalculationEngineCodeModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(String code, String description, BigDecimal displayOrder) {
		CalculationEngineTestData data = CalculationEngineTestData.builder().code(code).description(description)
				.displayOrder(displayOrder);

		CalculationEngineCodeModel model = data.buildModel();
		CalculationEngineCodeEntity entity = data.buildEntity();
		CalculationEngineResourceAssembler assembler = new CalculationEngineResourceAssembler();
		CalculationEngineCodeEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	static final class CalculationEngineTestData {
		private String code = null;
		private String description;
		private BigDecimal displayOrder;

		public static TestCalculationEngineResourceAssembler.CalculationEngineTestData builder() {
			return new TestCalculationEngineResourceAssembler.CalculationEngineTestData();
		}

		public TestCalculationEngineResourceAssembler.CalculationEngineTestData code(String code) {
			this.code = code;
			return this;
		}

		public TestCalculationEngineResourceAssembler.CalculationEngineTestData description(String description) {
			this.description = description;
			return this;
		}

		public TestCalculationEngineResourceAssembler.CalculationEngineTestData displayOrder(BigDecimal displayOrder) {
			this.displayOrder = displayOrder;
			return this;
		}

		public CalculationEngineCodeEntity buildEntity() {
			CalculationEngineCodeEntity data = new CalculationEngineCodeEntity();
			data.setCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}

		public CalculationEngineCodeModel buildModel() {
			CalculationEngineCodeModel data = new CalculationEngineCodeModel();
			data.setCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}
	}

}
