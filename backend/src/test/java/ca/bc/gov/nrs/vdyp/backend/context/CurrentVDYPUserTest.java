package ca.bc.gov.nrs.vdyp.backend.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;

class CurrentVDYPUserTest {
	CurrentVDYPUser currentVDYPUser;

	@BeforeEach
	void setup() {
		currentVDYPUser = new CurrentVDYPUser();
	}

	@Test
	void getUserId_returnsNullForNullCurrentUser() {
		String currentUserGUID = currentVDYPUser.getUserId();
		assertNull(currentUserGUID);
	}

	@Test
	void getUserId_returnsUserIdOfCurrentUser() {
		UUID currentId = UUID.randomUUID();

		VDYPUserModel currentUser = new VDYPUserModel();
		currentUser.setVdypUserGUID(currentId.toString());

		currentVDYPUser.setUser(currentUser);

		VDYPUserModel returnedModel = currentVDYPUser.getUser();
		assertEquals(currentUser, returnedModel);

		String currentUserGUID = currentVDYPUser.getUserId();
		assertNotNull(currentUserGUID);
		assertEquals(currentUserGUID, currentUser.getVdypUserGUID());
	}

}
