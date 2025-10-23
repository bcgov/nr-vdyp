package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionBatchMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionBatchMappingModel;

public class TestProjectionBatchMappingResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new ProjectionBatchMappingResourceAssembler().toEntity(null)).isNull();
		assertThat(new ProjectionBatchMappingResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(
				Arguments.of(UUID.randomUUID(), UUID.randomUUID()), Arguments.of(null, UUID.randomUUID()),
				Arguments.of(UUID.randomUUID(), null)
		);
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(UUID fileMapping, UUID batchJobId) {
		TestProjectionBatchMappingResourceAssembler.ProjectionBatchMappingTestData data = TestProjectionBatchMappingResourceAssembler.ProjectionBatchMappingTestData
				.builder().withProjectionBatchMappingGuid(fileMapping).withCOMSGuid(batchJobId);

		ProjectionBatchMappingModel model = data.buildModel();
		ProjectionBatchMappingEntity entity = data.buildEntity();
		ProjectionBatchMappingResourceAssembler assembler = new ProjectionBatchMappingResourceAssembler();
		ProjectionBatchMappingModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(UUID fileMapping, UUID batchJobId) {
		TestProjectionBatchMappingResourceAssembler.ProjectionBatchMappingTestData data = TestProjectionBatchMappingResourceAssembler.ProjectionBatchMappingTestData
				.builder().withProjectionBatchMappingGuid(fileMapping).withCOMSGuid(batchJobId);

		ProjectionBatchMappingModel model = data.buildModel();
		ProjectionBatchMappingEntity entity = data.buildEntity();
		ProjectionBatchMappingResourceAssembler assembler = new ProjectionBatchMappingResourceAssembler();
		ProjectionBatchMappingEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	private static final class ProjectionBatchMappingTestData {
		private UUID fileMappingUUID = null;
		private UUID batchJobUUID = null;

		public static TestProjectionBatchMappingResourceAssembler.ProjectionBatchMappingTestData builder() {
			return new TestProjectionBatchMappingResourceAssembler.ProjectionBatchMappingTestData();
		}

		public TestProjectionBatchMappingResourceAssembler.ProjectionBatchMappingTestData
				withProjectionBatchMappingGuid(UUID fileMappingUUID) {
			this.fileMappingUUID = fileMappingUUID;
			return this;
		}

		public TestProjectionBatchMappingResourceAssembler.ProjectionBatchMappingTestData
				withCOMSGuid(UUID batchJobUUID) {
			this.batchJobUUID = batchJobUUID;
			return this;
		}

		public ProjectionBatchMappingEntity buildEntity() {
			ProjectionBatchMappingEntity data = new ProjectionBatchMappingEntity();
			data.setProjectionBatchMappingGUID(fileMappingUUID);
			data.setBatchJobGUID(batchJobUUID);
			return data;
		}

		public ProjectionBatchMappingModel buildModel() {
			ProjectionBatchMappingModel data = new ProjectionBatchMappingModel();
			data.setProjectionBatchMappingGUID(fileMappingUUID == null ? null : fileMappingUUID.toString());
			data.setBatchJobGUID(batchJobUUID == null ? null : batchJobUUID.toString());
			return data;
		}
	}

}
