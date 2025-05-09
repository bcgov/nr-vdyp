package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
