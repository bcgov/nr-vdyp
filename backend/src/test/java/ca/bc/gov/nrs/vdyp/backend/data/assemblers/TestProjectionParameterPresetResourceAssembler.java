package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionParameterPresetEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionParameterPresetModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import lombok.Builder;

public class TestProjectionParameterPresetResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new ProjectionParameterPresetResourceAssembler().toEntity(null)).isNull();
		assertThat(new ProjectionParameterPresetResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(Arguments.of(UUID.randomUUID(), UUID.randomUUID(), "ADMIN", "John", "Doe"));
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(UUID presetId, UUID ownerId, String presetName, String parametersJSON) {
		TestProjectionParameterPresetResourceAssembler.ProjectionParameterPresetTestData data = ProjectionParameterPresetTestData
				.builder().presetId(presetId).ownerId(ownerId).presetName(presetName).parametersJSON(parametersJSON)
				.build();

		ProjectionParameterPresetModel model = data.buildModel();
		ProjectionParameterPresetEntity entity = data.buildEntity();
		ProjectionParameterPresetResourceAssembler assembler = new ProjectionParameterPresetResourceAssembler();
		ProjectionParameterPresetModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(UUID presetId, UUID ownerId, String presetName, String parametersJSON) {
		TestProjectionParameterPresetResourceAssembler.ProjectionParameterPresetTestData data = ProjectionParameterPresetTestData
				.builder().presetId(presetId).ownerId(ownerId).presetName(presetName).parametersJSON(parametersJSON)
				.build();

		ProjectionParameterPresetModel model = data.buildModel();
		ProjectionParameterPresetEntity entity = data.buildEntity();
		ProjectionParameterPresetResourceAssembler assembler = new ProjectionParameterPresetResourceAssembler();
		ProjectionParameterPresetEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	@Builder
	private static final class ProjectionParameterPresetTestData {
		private UUID presetId;
		private UUID ownerId;
		private String presetName;
		private String parametersJSON;

		public ProjectionParameterPresetEntity buildEntity() {
			ProjectionParameterPresetEntity data = new ProjectionParameterPresetEntity();
			data.setProjectionParameterPresetGUID(presetId);
			data.setPresetName(presetName);
			data.setPresetParameters(parametersJSON);
			if (ownerId != null) {
				var userData = new VDYPUserEntity();
				userData.setVdypUserGUID(ownerId);
				data.setOwnerUser(userData);
			}
			data.setPresetName(presetName);
			data.setPresetParameters(parametersJSON);
			return data;
		}

		public ProjectionParameterPresetModel buildModel() {
			ProjectionParameterPresetModel data = new ProjectionParameterPresetModel();
			if (presetId != null)
				data.setProjectionParameterPresetGUID(presetId.toString());
			if (ownerId != null) {
				var userData = new VDYPUserModel();
				userData.setVdypUserGUID(ownerId.toString());
				data.setOwnerUser(userData);
			}
			data.setPresetName(presetName);
			data.setPresetParameters(parametersJSON);
			return data;
		}
	}
}
