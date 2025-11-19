package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

public class CSVYieldTableRecordBeanTest extends AbstractYieldTableRecordBeanTest {
	@Override
	YieldTableRowBean createInstance() {
		return new CSVYieldTableRowValuesBean();
	}

}
