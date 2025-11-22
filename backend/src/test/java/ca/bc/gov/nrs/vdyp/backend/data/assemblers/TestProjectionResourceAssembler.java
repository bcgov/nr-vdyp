package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionFileSetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionFileSetModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import lombok.Builder;

class TestProjectionResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new ProjectionResourceAssembler().toEntity(null)).isNull();
		assertThat(new ProjectionResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(
				Arguments.of(
						UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
						"{\"testParam\":\"testVValue\"}", OffsetDateTime.now(), OffsetDateTime.now()
				)
		);
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(
			UUID projectionId, UUID ownerId, UUID polygonFileSetId, UUID layerFileSetId, UUID resultFileSetId,
			String parameters, OffsetDateTime startDate, OffsetDateTime endDate
	) {
		ProjectionTestData data = ProjectionTestData.builder().projectionId(projectionId).ownerId(ownerId)
				.polygonFileSetId(polygonFileSetId).layerFileSetId(layerFileSetId).restulFileSetId(resultFileSetId)
				.parameters(parameters).startDate(startDate).endDate(endDate).build();

		ProjectionModel model = data.buildModel();
		ProjectionEntity entity = data.buildEntity();
		ProjectionResourceAssembler assembler = new ProjectionResourceAssembler();
		ProjectionModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(
			UUID projectionId, UUID ownerId, UUID polygonFileSetId, UUID layerFileSetId, UUID resultFileSetId,
			String parameters, OffsetDateTime startDate, OffsetDateTime endDate
	) {
		ProjectionTestData data = ProjectionTestData.builder().projectionId(projectionId).ownerId(ownerId)
				.polygonFileSetId(polygonFileSetId).layerFileSetId(layerFileSetId).restulFileSetId(resultFileSetId)
				.parameters(parameters).startDate(startDate).endDate(endDate).build();
		ProjectionModel model = data.buildModel();
		ProjectionEntity entity = data.buildEntity();
		ProjectionResourceAssembler assembler = new ProjectionResourceAssembler();
		ProjectionEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	@Builder
	private static final class ProjectionTestData {
		private UUID projectionId;
		private UUID ownerId;
		private UUID polygonFileSetId;
		private UUID layerFileSetId;
		private UUID restulFileSetId;

		private String parameters;
		private OffsetDateTime startDate;
		private OffsetDateTime endDate;

		public ProjectionEntity buildEntity() {
			ProjectionEntity data = new ProjectionEntity();
			data.setProjectionGUID(projectionId);
			if (ownerId != null) {
				var ownerUser = new VDYPUserEntity();
				ownerUser.setVdypUserGUID(ownerId);
				data.setOwnerUser(ownerUser);
			}
			var fileSets = new UUID[] { polygonFileSetId, layerFileSetId, restulFileSetId };
			for (int i = 0; i < fileSets.length; i++) {
				UUID fileSetId = fileSets[i];
				if (fileSetId != null) {
					var fileSetEntity = new ProjectionFileSetEntity();
					fileSetEntity.setProjectionFileSetGUID(fileSetId);
					switch (i) {
					case 0 -> data.setPolygonFileSet(fileSetEntity);
					case 1 -> data.setLayerFileSet(fileSetEntity);
					case 2 -> data.setResultFileSet(fileSetEntity);
					}
				}
			}
			data.setProjectionParameters(parameters);
			data.setStartDate(startDate);
			data.setEndDate(endDate);
			return data;
		}

		public ProjectionModel buildModel() {
			ProjectionModel data = new ProjectionModel();
			data.setProjectionGUID(projectionId == null ? null : projectionId.toString());
			if (ownerId != null) {
				var ownerUser = new VDYPUserModel();
				ownerUser.setVdypUserGUID(ownerId.toString());
				data.setOwnerUser(ownerUser);
			}
			var fileSets = new UUID[] { polygonFileSetId, layerFileSetId, restulFileSetId };
			for (int i = 0; i < fileSets.length; i++) {
				UUID fileSetId = fileSets[i];
				if (fileSetId != null) {
					var fileSetModel = new ProjectionFileSetModel();
					fileSetModel.setProjectionFileSetGUID(fileSetId.toString());
					switch (i) {
					case 0 -> data.setPolygonFileSet(fileSetModel);
					case 1 -> data.setLayerFileSet(fileSetModel);
					case 2 -> data.setResultFileSet(fileSetModel);
					}
				}
			}
			data.setProjectionParameters(parameters);
			data.setStartDate(startDate);
			data.setEndDate(endDate);
			return data;
		}
	}
}
