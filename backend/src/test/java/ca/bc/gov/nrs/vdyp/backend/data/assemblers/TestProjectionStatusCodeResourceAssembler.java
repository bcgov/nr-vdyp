package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionStatusCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionStatusCodeModel;

public class TestProjectionStatusCodeResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new ProjectionStatusCodeResourceAssembler().toEntity(null)).isNull();
		assertThat(new ProjectionStatusCodeResourceAssembler().toModel(null)).isNull();
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
		TestProjectionStatusCodeResourceAssembler.ProjectionStatusCodeTestData data = TestProjectionStatusCodeResourceAssembler.ProjectionStatusCodeTestData
				.builder().withCode(code).withDescription(description).withDisplayOrder(displayOrder);

		ProjectionStatusCodeModel model = data.buildModel();
		ProjectionStatusCodeEntity entity = data.buildEntity();
		ProjectionStatusCodeResourceAssembler assembler = new ProjectionStatusCodeResourceAssembler();
		ProjectionStatusCodeModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(String code, String description, Integer displayOrder) {
		TestProjectionStatusCodeResourceAssembler.ProjectionStatusCodeTestData data = TestProjectionStatusCodeResourceAssembler.ProjectionStatusCodeTestData
				.builder().withCode(code).withDescription(description).withDisplayOrder(displayOrder);

		ProjectionStatusCodeModel model = data.buildModel();
		ProjectionStatusCodeEntity entity = data.buildEntity();
		ProjectionStatusCodeResourceAssembler assembler = new ProjectionStatusCodeResourceAssembler();
		ProjectionStatusCodeEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	private static final class ProjectionStatusCodeTestData {
		private String code = null;
		private String description;
		private Integer displayOrder;

		public static TestProjectionStatusCodeResourceAssembler.ProjectionStatusCodeTestData builder() {
			return new TestProjectionStatusCodeResourceAssembler.ProjectionStatusCodeTestData();
		}

		public TestProjectionStatusCodeResourceAssembler.ProjectionStatusCodeTestData withCode(String code) {
			this.code = code;
			return this;
		}

		public TestProjectionStatusCodeResourceAssembler.ProjectionStatusCodeTestData
				withDescription(String description) {
			this.description = description;
			return this;
		}

		public TestProjectionStatusCodeResourceAssembler.ProjectionStatusCodeTestData
				withDisplayOrder(Integer displayOrder) {
			this.displayOrder = displayOrder;
			return this;
		}

		public ProjectionStatusCodeEntity buildEntity() {
			ProjectionStatusCodeEntity data = new ProjectionStatusCodeEntity();
			data.setProjectionStatusCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}

		public ProjectionStatusCodeModel buildModel() {
			ProjectionStatusCodeModel data = new ProjectionStatusCodeModel();
			data.setProjectionStatusCode(code);
			data.setDescription(description);
			data.setDisplayOrder(displayOrder);
			return data;
		}
	}

}
