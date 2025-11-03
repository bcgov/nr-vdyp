package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;
import lombok.Builder;

class TestFileMappingResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new FileMappingResourceAssembler().toEntity(null)).isNull();
		assertThat(new FileMappingResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(
				Arguments.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
				Arguments.of(null, UUID.randomUUID(), UUID.randomUUID()),
				Arguments.of(UUID.randomUUID(), null, UUID.randomUUID()),
				Arguments.of(UUID.randomUUID(), UUID.randomUUID(), null)
		);
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(UUID fileMapping, UUID comsObjectId, UUID projectionFileSetUUID) {
		FileMappingTestData data = FileMappingTestData.builder().fileMappingUUID(fileMapping)
				.comsObjectUUID(comsObjectId).projectionFileSetUUID(projectionFileSetUUID).build();

		FileMappingModel model = data.buildModel();
		FileMappingEntity entity = data.buildEntity();
		FileMappingResourceAssembler assembler = new FileMappingResourceAssembler();
		FileMappingModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(UUID fileMapping, UUID comsObjectId, UUID projectionFileSetUUID) {
		FileMappingTestData data = FileMappingTestData.builder().fileMappingUUID(fileMapping)
				.comsObjectUUID(comsObjectId).projectionFileSetUUID(projectionFileSetUUID).build();

		FileMappingModel model = data.buildModel();
		FileMappingEntity entity = data.buildEntity();
		FileMappingResourceAssembler assembler = new FileMappingResourceAssembler();
		FileMappingEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	@Builder
	private static final class FileMappingTestData {
		private UUID fileMappingUUID = null;
		private UUID comsObjectUUID = null;
		private UUID projectionFileSetUUID = null;

		public FileMappingEntity buildEntity() {
			FileMappingEntity data = new FileMappingEntity();
			data.setFileMappingGUID(fileMappingUUID);
			data.setComsObjectGUID(comsObjectUUID);
			if (projectionFileSetUUID != null) {
				var projectionFileSet = new ProjectionFileSetEntity();
				projectionFileSet.setProjectionFileSetGUID(projectionFileSetUUID);
				data.setProjectionFileSet(projectionFileSet);
			}
			return data;
		}

		public FileMappingModel buildModel() {
			FileMappingModel data = new FileMappingModel();
			data.setFileMappingGUID(fileMappingUUID == null ? null : fileMappingUUID.toString());
			data.setComsObjectGUID(comsObjectUUID == null ? null : comsObjectUUID.toString());
			if (projectionFileSetUUID != null) {
				var projectionFileSet = new ProjectionFileSetModel();
				projectionFileSet.setProjectionFileSetGUID(projectionFileSetUUID.toString());
				data.setProjectionFileSet(projectionFileSet);
			}
			return data;
		}
	}

}
