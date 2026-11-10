package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class YieldTableWriterTest {

	private TestYieldTableWriter startWriter() {
		var writer = new TestYieldTableWriter(CSVYieldTableRowValuesBean.class, Path.of("unused.csv"));
		writer.startNewRecord();
		return writer;
	}

	@Test
	void testRecordSiteInformationLeavesNegativeHeightsEmpty() {
		var writer = startWriter();

		writer.recordSiteInformation(null, null, -9.0, -9.0);

		assertThat(writer.currentRecord.getDominantHeight(), nullValue());
		assertThat(writer.currentRecord.getSecondaryHeight(), nullValue());
	}

	@Test
	void testRecordSiteInformationRecordsNonNegativeHeights() {
		var writer = startWriter();

		writer.recordSiteInformation(null, null, 0.0, 12.5);

		assertThat(writer.currentRecord.getDominantHeight(), is(FieldFormatter.format(0.0)));
		assertThat(writer.currentRecord.getSecondaryHeight(), is(FieldFormatter.format(12.5)));
	}
}
