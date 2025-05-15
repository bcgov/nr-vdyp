package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DCSVYieldTableRecordBean extends UnsupportedYieldTableRecordBean {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(DCSVYieldTableRecordBean.class);

	public static StatefulBeanToCsv<DCSVYieldTableRecordBean> createCsvOutputStream(FileWriter writer) {
		return new StatefulBeanToCsvBuilder<DCSVYieldTableRecordBean>(writer) //
				.build();
	}

	public DCSVYieldTableRecordBean() {
		// default constructor necessary for reflection
	}
}
