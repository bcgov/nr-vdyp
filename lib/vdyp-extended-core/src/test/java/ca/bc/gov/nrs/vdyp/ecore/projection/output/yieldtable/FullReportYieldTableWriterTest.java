package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;

class FullReportYieldTableWriterTest {

	@Test
	void testWriteRecordFailsWhenRowContextIsIncomplete() throws Exception {

		var polygon = new Polygon.Builder().featureId(13919428).build();
		var params = new Parameters().outputFormat(Parameters.OutputFormat.YIELD_TABLE).ageStart(0).ageEnd(100);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);

		var writer = FullReportYieldTableWriter.of(context);
		var rowContext = YieldTableRowContext.of(context, polygon, new PolygonProjectionState(), null);

		writer.startNewRecord();

		var ex = assertThrows(YieldTableGenerationException.class, () -> writer.writeRecord(rowContext));
		assertTrue(ex.getMessage().startsWith("Polygon 13919428"));
	}

	@Test
	void testToYieldTableGenerationExceptionIncludesFeatureIdWhenPolygonKnown() {

		var polygon = new Polygon.Builder().featureId(13919428).build();

		var ex = FullReportYieldTableWriter.toYieldTableGenerationException(polygon, new IOException("boom"));

		assertThat(ex.getMessage(), is("Polygon 13919428: boom"));
	}

	@Test
	void testToYieldTableGenerationExceptionOmitsFeatureIdWhenPolygonUnknown() {

		var ex = FullReportYieldTableWriter.toYieldTableGenerationException(null, new IOException("boom"));

		assertThat(ex.getMessage(), is("java.io.IOException: boom"));
	}
}
