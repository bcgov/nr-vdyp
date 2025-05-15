package ca.bc.gov.nrs.vdyp.backend.endpoints.v1;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ca.bc.gov.nrs.api.helpers.TestHelper;

public class BaseHttpProjectionRequestTest {

	protected static final TestHelper testHelper = new TestHelper();

	protected Map<String /* entry name */, String /* entry contents */> parseZipResults(InputStream is)
			throws IOException {
		Map<String, String> entryMapByName = new HashMap<>();

		ZipInputStream zipFile = new ZipInputStream(is);

		ZipEntry entry;
		while ( (entry = zipFile.getNextEntry()) != null) {
			entryMapByName.put(entry.getName(), new String(testHelper.readZipEntry(zipFile, entry)));
		}

		return entryMapByName;
	}
}
