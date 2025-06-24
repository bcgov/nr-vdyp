package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PolygonMessageList {

	private List<PolygonMessage> messages = new ArrayList<>();

	public void add(PolygonMessage m) {
		messages.add(m);
	}

	public List<PolygonMessage> getMessages() {
		return Collections.unmodifiableList(messages);
	}
}
