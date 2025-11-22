package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.FileSetTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileSetTypeCodeModel;

class TestFileSetTypeCodeResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new FileSetTypeCodeResourceAssembler().toEntity(null)).isNull();
		assertThat(new FileSetTypeCodeResourceAssembler().toModel(null)).isNull();
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
		TestFileSetTypeCodeResourceAssembler.FileSetTypeCodeTestData data = TestFileSetTypeCodeResourceAssembler.FileSetTypeCodeTestData
				.builder().withCode(code).withDescription(description).withDisplayOrder(displayOrder);

		FileSetTypeCodeModel model = data.buildModel();
		FileSetTypeCodeEntity entity = data.buildEntity();
		FileSetTypeCodeResourceAssembler assembler = new FileSetTypeCodeResourceAssembler();
		FileSetTypeCodeModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(String code, String description, BigDecimal displayOrder) {
		TestFileSetTypeCodeResourceAssembler.FileSetTypeCodeTestData data = TestFileSetTypeCodeResourceAssembler.FileSetTypeCodeTestData
				.builder().withCode(code).withDescription(description).withDisplayOrder(displayOrder);

		FileSetTypeCodeModel model = data.buildModel();
		FileSetTypeCodeEntity entity = data.buildEntity();
		FileSetTypeCodeResourceAssembler assembler = new FileSetTypeCodeResourceAssembler();
		FileSetTypeCodeEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	private static final class FileSetTypeCodeTestData {
		private String code = null;
		private String description;
		private BigDecimal displayOrder;

		public static TestFileSetTypeCodeResourceAssembler.FileSetTypeCodeTestData builder() {
			return new TestFileSetTypeCodeResourceAssembler.FileSetTypeCodeTestData();
		}

		public TestFileSetTypeCodeResourceAssembler.FileSetTypeCodeTestData withCode(String code) {
			this.code = code;
			return this;
		}

		public TestFileSetTypeCodeResourceAssembler.FileSetTypeCodeTestData withDescription(String description) {
			this.description = description;
			return this;
		}

		public TestFileSetTypeCodeResourceAssembler.FileSetTypeCodeTestData withDisplayOrder(BigDecimal displayOrder) {
			this.displayOrder = displayOrder;
			return this;
		}

		public FileSetTypeCodeEntity buildEntity() {
			FileSetTypeCodeEntity data = new FileSetTypeCodeEntity();
			data.setFileSetTypeCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}

		public FileSetTypeCodeModel buildModel() {
			FileSetTypeCodeModel data = new FileSetTypeCodeModel();
			data.setFileSetTypeCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}
	}

}
