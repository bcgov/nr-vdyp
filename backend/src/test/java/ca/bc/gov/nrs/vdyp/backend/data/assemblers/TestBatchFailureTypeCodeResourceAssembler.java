package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.BatchFailureTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.BatchFailureTypeCodeModel;

class TestBatchFailureTypeCodeResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new BatchFailureTypeCodeResourceAssembler().toEntity(null)).isNull();
		assertThat(new BatchFailureTypeCodeResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(
				Arguments.of("INPUT", "Processing Inputs", BigDecimal.ONE), Arguments.of(null, null, null),
				Arguments.of("PROCESS", "Projecting Stands", BigDecimal.TEN),
				Arguments.of("OUTPUT", "Aggregating Results", null)
		);
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(String code, String description, BigDecimal displayOrder) {
		BatchFailureTypeCodeTestData data = BatchFailureTypeCodeTestData.builder().withCode(code)
				.withDescription(description).withDisplayOrder(displayOrder);

		BatchFailureTypeCodeModel model = data.buildModel();
		BatchFailureTypeCodeEntity entity = data.buildEntity();
		BatchFailureTypeCodeResourceAssembler assembler = new BatchFailureTypeCodeResourceAssembler();
		BatchFailureTypeCodeModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(String code, String description, BigDecimal displayOrder) {
		BatchFailureTypeCodeTestData data = BatchFailureTypeCodeTestData.builder().withCode(code)
				.withDescription(description).withDisplayOrder(displayOrder);

		BatchFailureTypeCodeModel model = data.buildModel();
		BatchFailureTypeCodeEntity entity = data.buildEntity();
		BatchFailureTypeCodeResourceAssembler assembler = new BatchFailureTypeCodeResourceAssembler();
		BatchFailureTypeCodeEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);
	}

	private static final class BatchFailureTypeCodeTestData {
		private String code = null;
		private String description;
		private BigDecimal displayOrder;

		public static BatchFailureTypeCodeTestData builder() {
			return new BatchFailureTypeCodeTestData();
		}

		public BatchFailureTypeCodeTestData withCode(String code) {
			this.code = code;
			return this;
		}

		public BatchFailureTypeCodeTestData withDescription(String description) {
			this.description = description;
			return this;
		}

		public BatchFailureTypeCodeTestData withDisplayOrder(BigDecimal displayOrder) {
			this.displayOrder = displayOrder;
			return this;
		}

		public BatchFailureTypeCodeEntity buildEntity() {
			BatchFailureTypeCodeEntity data = new BatchFailureTypeCodeEntity();
			data.setCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}

		public BatchFailureTypeCodeModel buildModel() {
			BatchFailureTypeCodeModel data = new BatchFailureTypeCodeModel();
			data.setCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}
	}
}
