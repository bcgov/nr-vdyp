package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

public class PLOTSYYieldTableRecordBean extends UnsupportedYieldTableRecordBean {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PLOTSYYieldTableRecordBean.class);

	public static StatefulBeanToCsv<PLOTSYYieldTableRecordBean> createCsvOutputStream(FileWriter writer) {
		return new StatefulBeanToCsvBuilder<PLOTSYYieldTableRecordBean>(writer) //
				.build();
	}

	public PLOTSYYieldTableRecordBean() {
		// default constructor necessary for reflection
	}
}
