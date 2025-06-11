package ca.bc.gov.nrs.vdyp.backend.endpoints.projection.hcsv._44grpA;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters.ExecutionOption;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.constraint.Assert;

@QuarkusTest
class Hcsv_OneDeadLayerTest extends BaseHcsv_44GrpATest {

	private static final Logger logger = LoggerFactory.getLogger(Hcsv_OneDeadLayerTest.class);

	// 19007816 contains one dead layer. Tests yield table generation for the layer consisting of
	// one row (i.e., year start == year end).
	@Test
	void run44GroupA_19007816_Test() throws IOException {
		logger.info("Starting run44GroupA_19007816_Test");

		parameters.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES);
		parameters.addSelectedExecutionOptionsItem(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_POLYGON);
		parameters.addSelectedExecutionOptionsItem(ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER);
		parameters.addSelectedExecutionOptionsItem(ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE);
		parameters.ageIncrement(10);

		var zipEntries = runTest("19007816");

		var errorEntryContent = zipEntries.get("ErrorLog.txt");
		Assert.assertTrue(errorEntryContent.length() == 0);
		var csvEntryContent = zipEntries.get("YieldTable.csv");
		Assert.assertTrue(csvEntryContent.length() > 0);
	}
}
