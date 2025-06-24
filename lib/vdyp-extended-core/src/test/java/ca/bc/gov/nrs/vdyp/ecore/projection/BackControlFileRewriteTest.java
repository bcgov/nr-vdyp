package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;

class BackControlFileRewriteTest {

	@Test
	void testRewritingBackControlFile() throws IOException {

		Path executionFolder = Files.createTempDirectory("polygon-projection");
		Path projectionExecutionFolderPath = Path.of(executionFolder.toString(), ProjectionTypeCode.PRIMARY.toString());
		Path projectionExecutionFolder = Files.createDirectory(projectionExecutionFolderPath);
		Path controlFile = Files
				.createFile(Path.of(projectionExecutionFolder.toString(), Vdyp7Constants.BACK_CONTROL_FILE_NAME));

		try {
			Files.write(
					controlFile,
					("097 coe/vetdq2.dat                                    DQ for Vet layer           RD_YDQV\n" + "\n"
							+ "101 %YR%   0   2   4   1   0                          Control variables (10I4)\n"
							+ "106 coe/YLDBA407.coe                                  BA Yield (Primary layer)   RD_E106 ipsjf160\n")
							.getBytes()
			);

			PolygonProjectionRunner
					.rewriteTargetYearToBackControlFile(executionFolder, 9999, ProjectionTypeCode.PRIMARY);

			String contents = Files.readString(controlFile);

			assertTrue(contents.indexOf("9999") >= 0);
		} catch (IOException | PolygonExecutionException e) {
			fail(e.getMessage());
		}
	}
}
