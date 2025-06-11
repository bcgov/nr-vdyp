package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.io.IOException;
import java.nio.file.Path;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;

public class TestYieldTableWriter extends YieldTableWriter<CSVYieldTableRowValuesBean> {

	protected TestYieldTableWriter(Class<CSVYieldTableRowValuesBean> rowValuesClass, Path yieldTableFilePath) {
		super(rowValuesClass, yieldTableFilePath);
	}

	@Override
	protected void writeRecord(YieldTableRowContext rowContext) throws YieldTableGenerationException {

	}

	@Override
	protected void writeProjectionGrowthInfo() throws YieldTableGenerationException {
	}

	@Override
	public void close() throws IOException {

	}
}
