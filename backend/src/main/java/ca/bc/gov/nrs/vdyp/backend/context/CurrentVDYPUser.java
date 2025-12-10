package ca.bc.gov.nrs.vdyp.backend.context;

import ca.bc.gov.nrs.vdyp.backend.data.models.VDYPUserModel;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CurrentVDYPUser {
	private VDYPUserModel user;

	public VDYPUserModel getUser() {
		return user;
	}

	public void setUser(VDYPUserModel user) {
		this.user = user;
	}

	public String getUserId() {
		return user != null ? user.getVdypUserGUID() : null;
	}
}
