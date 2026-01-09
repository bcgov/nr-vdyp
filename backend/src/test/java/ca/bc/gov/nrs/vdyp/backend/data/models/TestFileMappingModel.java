package ca.bc.gov.nrs.vdyp.backend.data.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

class TestFileMappingModel {
	@Test
	void testSetDownloadURL() throws MalformedURLException {
		var model = new FileMappingModel();
		URL url = new URL("http://test.com");
		model.setDownloadURL(url);
		assertEquals(url, model.getDownloadURL());
	}
}
