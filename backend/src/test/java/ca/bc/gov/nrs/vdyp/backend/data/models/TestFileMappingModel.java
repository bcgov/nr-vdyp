package ca.bc.gov.nrs.vdyp.backend.data.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TestFileMappingModel {
	@Test
	void testSetDownloadURL() {
		var model = new FileMappingModel();
		model.setDownloadURL("http://test.com");
		assertEquals("http://test.com", model.getDownloadURL());
	}
}
