package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;

class TestProjectionFileSetResourceAssembler {

	@Test
	public void testNull() {
		assertThat(new ProjectionFileSetResourceAssembler().toEntity(null)).isNull();
		assertThat(new ProjectionFileSetResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(Arguments.of(UUID.randomUUID(), "Test File Set Name"));
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(UUID fileMapping, String fileSetName) {
		ProjectionFileSetTestData data = ProjectionFileSetTestData.builder().fileMappingUUID(fileMapping)
				.fileSetName(fileSetName);

		ProjectionFileSetModel model = data.buildModel();
		ProjectionFileSetEntity entity = data.buildEntity();
		ProjectionFileSetResourceAssembler assembler = new ProjectionFileSetResourceAssembler();
		ProjectionFileSetModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(UUID fileMapping, String fileSetName) {
		ProjectionFileSetTestData data = ProjectionFileSetTestData.builder().fileMappingUUID(fileMapping)
				.fileSetName(fileSetName);

		ProjectionFileSetModel model = data.buildModel();
		ProjectionFileSetEntity entity = data.buildEntity();
		ProjectionFileSetResourceAssembler assembler = new ProjectionFileSetResourceAssembler();
		ProjectionFileSetEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	private static final class ProjectionFileSetTestData {
		private UUID fileMappingUUID = null;
		private String fileSetName = null;

		public static TestProjectionFileSetResourceAssembler.ProjectionFileSetTestData builder() {
			return new TestProjectionFileSetResourceAssembler.ProjectionFileSetTestData();
		}

		public TestProjectionFileSetResourceAssembler.ProjectionFileSetTestData fileMappingUUID(UUID fileMappingUUID) {
			this.fileMappingUUID = fileMappingUUID;
			return this;
		}

		public TestProjectionFileSetResourceAssembler.ProjectionFileSetTestData fileSetName(String fileSetName) {
			this.fileSetName = fileSetName;
			return this;
		}

		public ProjectionFileSetEntity buildEntity() {
			ProjectionFileSetEntity data = new ProjectionFileSetEntity();
			data.setProjectionFileSetGUID(fileMappingUUID);
			return data;
		}

		public ProjectionFileSetModel buildModel() {
			ProjectionFileSetModel data = new ProjectionFileSetModel();
			data.setProjectionFileSetGUID(fileMappingUUID == null ? null : fileMappingUUID.toString());
			return data;
		}
	}
}
