package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.FileMappingEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.FileMappingModel;

public class TestFileMappingResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new FileMappingResourceAssembler().toEntity(null)).isNull();
		assertThat(new FileMappingResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(
				Arguments.of(UUID.randomUUID(), UUID.randomUUID()), Arguments.of(null, UUID.randomUUID()),
				Arguments.of(UUID.randomUUID(), null)
		);
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(UUID fileMapping, UUID comsObjectId) {
		FileMappingTestData data = FileMappingTestData.builder().withFileMappingGuid(fileMapping)
				.withCOMSGuid(comsObjectId);

		FileMappingModel model = data.buildModel();
		FileMappingEntity entity = data.buildEntity();
		FileMappingResourceAssembler assembler = new FileMappingResourceAssembler();
		FileMappingModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(UUID fileMapping, UUID comsObjectId) {
		FileMappingTestData data = FileMappingTestData.builder().withFileMappingGuid(fileMapping)
				.withCOMSGuid(comsObjectId);

		FileMappingModel model = data.buildModel();
		FileMappingEntity entity = data.buildEntity();
		FileMappingResourceAssembler assembler = new FileMappingResourceAssembler();
		FileMappingEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	private static final class FileMappingTestData {
		private UUID fileMappingUUID = null;
		private UUID comsObjectUUID = null;

		public static FileMappingTestData builder() {
			return new FileMappingTestData();
		}

		public FileMappingTestData withFileMappingGuid(UUID fileMappingUUID) {
			this.fileMappingUUID = fileMappingUUID;
			return this;
		}

		public FileMappingTestData withCOMSGuid(UUID comsObjectUUID) {
			this.comsObjectUUID = comsObjectUUID;
			return this;
		}

		public FileMappingEntity buildEntity() {
			FileMappingEntity data = new FileMappingEntity();
			data.setFileMappingGUID(fileMappingUUID);
			data.setComsObjectGUID(comsObjectUUID);
			return data;
		}

		public FileMappingModel buildModel() {
			FileMappingModel data = new FileMappingModel();
			data.setFileMappingGUID(fileMappingUUID == null ? null : fileMappingUUID.toString());
			data.setComsObjectGUID(comsObjectUUID == null ? null : comsObjectUUID.toString());
			return data;
		}
	}

}
