package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;

class TestUserTypeCodeResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new UserTypeCodeResourceAssembler().toEntity(null)).isNull();
		assertThat(new UserTypeCodeResourceAssembler().toModel(null)).isNull();
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
		TestUserTypeCodeResourceAssembler.UserTypeCodeTestData data = TestUserTypeCodeResourceAssembler.UserTypeCodeTestData
				.builder().withCode(code).withDescription(description).withDisplayOrder(displayOrder);

		UserTypeCodeModel model = data.buildModel();
		UserTypeCodeEntity entity = data.buildEntity();
		UserTypeCodeResourceAssembler assembler = new UserTypeCodeResourceAssembler();
		UserTypeCodeModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(String code, String description, BigDecimal displayOrder) {
		TestUserTypeCodeResourceAssembler.UserTypeCodeTestData data = TestUserTypeCodeResourceAssembler.UserTypeCodeTestData
				.builder().withCode(code).withDescription(description).withDisplayOrder(displayOrder);

		UserTypeCodeModel model = data.buildModel();
		UserTypeCodeEntity entity = data.buildEntity();
		UserTypeCodeResourceAssembler assembler = new UserTypeCodeResourceAssembler();
		UserTypeCodeEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	private static final class UserTypeCodeTestData {
		private String code = null;
		private String description;
		private BigDecimal displayOrder;

		public static TestUserTypeCodeResourceAssembler.UserTypeCodeTestData builder() {
			return new TestUserTypeCodeResourceAssembler.UserTypeCodeTestData();
		}

		public TestUserTypeCodeResourceAssembler.UserTypeCodeTestData withCode(String code) {
			this.code = code;
			return this;
		}

		public TestUserTypeCodeResourceAssembler.UserTypeCodeTestData withDescription(String description) {
			this.description = description;
			return this;
		}

		public TestUserTypeCodeResourceAssembler.UserTypeCodeTestData withDisplayOrder(BigDecimal displayOrder) {
			this.displayOrder = displayOrder;
			return this;
		}

		public UserTypeCodeEntity buildEntity() {
			UserTypeCodeEntity data = new UserTypeCodeEntity();
			data.setUserTypeCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}

		public UserTypeCodeModel buildModel() {
			UserTypeCodeModel data = new UserTypeCodeModel();
			data.setUserTypeCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}
	}

}
