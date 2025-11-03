package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionBatchMappingModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;
import lombok.Builder;

class TestProjectionBatchMappingResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new ProjectionBatchMappingResourceAssembler().toEntity(null)).isNull();
		assertThat(new ProjectionBatchMappingResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(
				Arguments.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 5, 1, 2, 3),
				Arguments.of(null, UUID.randomUUID(), UUID.randomUUID(), 3, 1, 0, 1),
				Arguments.of(UUID.randomUUID(), null, UUID.randomUUID(), 10, 5, 1, 0),
				Arguments.of(UUID.randomUUID(), UUID.randomUUID(), null, 5, 1, 2, 3)
		);
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(
			UUID projectionBatchId, UUID batchJobId, UUID projectionId, Integer partitionCount,
			Integer completedPartitionCount, Integer warningCount, Integer errorCount
	) {
		TestProjectionBatchMappingResourceAssembler.ProjectionBatchMappingTestData data = ProjectionBatchMappingTestData
				.builder().projectionBatchMappingUUID(projectionBatchId).batchJobUUID(batchJobId)
				.projectionUUID(projectionId).partitionCount(partitionCount)
				.completedPartitionCount(completedPartitionCount).warningCount(warningCount).errorCount(errorCount)
				.build();

		ProjectionBatchMappingModel model = data.buildModel();
		ProjectionBatchMappingEntity entity = data.buildEntity();
		ProjectionBatchMappingResourceAssembler assembler = new ProjectionBatchMappingResourceAssembler();
		ProjectionBatchMappingModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(
			UUID projectionBatchId, UUID batchJobId, UUID projectionId, Integer partitionCount,
			Integer completedPartitionCount, Integer warningCount, Integer errorCount
	) {
		TestProjectionBatchMappingResourceAssembler.ProjectionBatchMappingTestData data = ProjectionBatchMappingTestData
				.builder().projectionBatchMappingUUID(projectionBatchId).batchJobUUID(batchJobId)
				.projectionUUID(projectionId).partitionCount(partitionCount)
				.completedPartitionCount(completedPartitionCount).warningCount(warningCount).errorCount(errorCount)
				.build();

		ProjectionBatchMappingModel model = data.buildModel();
		ProjectionBatchMappingEntity entity = data.buildEntity();
		ProjectionBatchMappingResourceAssembler assembler = new ProjectionBatchMappingResourceAssembler();
		ProjectionBatchMappingEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	@Builder
	private static final class ProjectionBatchMappingTestData {
		private UUID projectionBatchMappingUUID = null;
		private UUID batchJobUUID = null;
		private UUID projectionUUID = null;
		private Integer partitionCount;
		private Integer completedPartitionCount;
		private Integer warningCount;
		private Integer errorCount;

		public ProjectionBatchMappingEntity buildEntity() {
			ProjectionBatchMappingEntity data = new ProjectionBatchMappingEntity();
			data.setProjectionBatchMappingGUID(projectionBatchMappingUUID);
			data.setBatchJobGUID(batchJobUUID);
			if (projectionUUID != null) {
				var projection = new ProjectionEntity();
				projection.setProjectionGUID(projectionUUID);
				data.setProjection(projection);
			}
			data.setPartitionCount(partitionCount);
			data.setCompletedPartitionCount(completedPartitionCount);
			data.setWarningCount(warningCount);
			data.setErrorCount(errorCount);
			return data;
		}

		public ProjectionBatchMappingModel buildModel() {
			ProjectionBatchMappingModel data = new ProjectionBatchMappingModel();
			data.setProjectionBatchMappingGUID(
					projectionBatchMappingUUID == null ? null : projectionBatchMappingUUID.toString()
			);
			data.setBatchJobGUID(batchJobUUID == null ? null : batchJobUUID.toString());

			if (projectionUUID != null) {
				var projection = new ProjectionModel();
				projection.setProjectionGUID(projectionUUID.toString());
				data.setProjection(projection);
			}
			data.setPartitionCount(partitionCount);
			data.setCompletedPartitionCount(completedPartitionCount);
			data.setWarningCount(warningCount);
			data.setErrorCount(errorCount);
			return data;
		}
	}

}
