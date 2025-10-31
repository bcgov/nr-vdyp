package ca.bc.gov.nrs.vdyp.backend.data.assemblers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity;
import ca.bc.gov.nrs.vdyp.backend.data.entities.VDYPUserEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.UserTypeCodeModel;
import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import lombok.Builder;

public class TestVDYPUserResourceAssembler {
	@Test
	public void testNull() {
		assertThat(new VDYPUserResourceAssembler().toEntity(null)).isNull();
		assertThat(new VDYPUserResourceAssembler().toModel(null)).isNull();
	}

	static Stream<Arguments> modelEntityData() {
		return Stream.of(Arguments.of(UUID.randomUUID(), "TestingID", "ADMIN", "John", "Doe"));
	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testEntityToModel(UUID userId, String oidcId, String userTypeCode, String firstName, String lastName) {
		VDYPUserTestData data = VDYPUserTestData.builder().userId(userId).oidcId(oidcId).userTypeCode(userTypeCode)
				.firstName(firstName).lastName(lastName).build();

		VDYPUserModel model = data.buildModel();
		VDYPUserEntity entity = data.buildEntity();
		VDYPUserResourceAssembler assembler = new VDYPUserResourceAssembler();
		VDYPUserModel assembledModel = assembler.toModel(entity);
		assertThat(assembledModel).usingRecursiveComparison().isEqualTo(model);

	}

	@ParameterizedTest
	@MethodSource("modelEntityData")
	void testModelToEntity(UUID userId, String oidcId, String userTypeCode, String firstName, String lastName) {
		VDYPUserTestData data = VDYPUserTestData.builder().userId(userId).oidcId(oidcId).userTypeCode(userTypeCode)
				.firstName(firstName).lastName(lastName).build();

		VDYPUserModel model = data.buildModel();
		VDYPUserEntity entity = data.buildEntity();
		VDYPUserResourceAssembler assembler = new VDYPUserResourceAssembler();
		VDYPUserEntity assembledEntity = assembler.toEntity(model);
		assertThat(assembledEntity).usingRecursiveComparison().isEqualTo(entity);

	}

	@Builder
	private static final class VDYPUserTestData {
		private UUID userId;
		private String oidcId;
		private String userTypeCode;
		private String firstName;
		private String lastName;

		public VDYPUserEntity buildEntity() {
			VDYPUserEntity data = new VDYPUserEntity();
			data.setVdypUserGUID(userId);
			data.setOidcGUID(oidcId);
			if (userTypeCode != null) {
				UserTypeCodeEntity codeData = new UserTypeCodeEntity();
				codeData.setUserTypeCode(userTypeCode);
				data.setUserTypeCode(codeData);
			}
			data.setFirstName(firstName);
			data.setLastName(lastName);
			return data;
		}

		public VDYPUserModel buildModel() {
			VDYPUserModel data = new VDYPUserModel();
			if (userId != null)
				data.setVdypUserGUID(userId.toString());
			data.setOidcGUID(oidcId);
			if (userTypeCode != null) {
				UserTypeCodeModel codeData = new UserTypeCodeModel();
				codeData.setUserTypeCode(userTypeCode);
				data.setUserTypeCode(codeData);
			}
			data.setFirstName(firstName);
			data.setLastName(lastName);
			return data;
		}
	}
}
