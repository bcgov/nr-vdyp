package ca.bc.gov.nrs.vdyp.backend.io.write;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;

import org.apache.commons.lang3.StringUtils;

import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Vdyp7Constants;

public abstract class AbstractOutputWriter {

	protected static final String POLY_IDENTIFIER_FORMAT = "%-25s";
	protected String format(Double d, int width, int precision) {
		
		if (width < 0 || precision < 0) {
			throw new IllegalArgumentException("format: width: " + width + "; precision: " + precision);
		}
	
		if (d == null) {
			return String.format("%" + width + 's', Vdyp7Constants.EMPTY_DECIMAL_TEXT);
		} else if (precision == 0) {
			var format = new DecimalFormat(StringUtils.repeat('#', width));
			return format.format(d);
		} else {
			var format = new DecimalFormat(StringUtils.repeat('#', width - precision - 1) + "0.0" + StringUtils.repeat('#', precision - 1));
			return format.format(d);
		}	
	}

	protected String format(Integer d, int width) {
	
		if (width < 0) {
			throw new IllegalArgumentException("format: width: " + width);
		}
		
		if (d == null) {
			return String.format("%" + width + 's', Vdyp7Constants.EMPTY_INT_TEXT);
		} else {
			var format = new DecimalFormat(StringUtils.repeat('#', width - 1) + "0");
			return format.format(d);
		}	
	}

	protected String format(Short d, int width) {
	
		if (width < 0) {
			throw new IllegalArgumentException("format: width: " + width);
		}
		
		if (d == null) {
			return String.format("%" + width + 's', Vdyp7Constants.EMPTY_INT_TEXT);
		} else {
			var format = new DecimalFormat(StringUtils.repeat('#', width - 1) + "0");
			return format.format(d);
		}	
	}

	protected static void writeEndRecord(OutputStream os, Polygon polygon, String format) throws IOException {
		writeFormat(os, format, polygon.buildPolygonDescriptor());
	}

	protected static void writeFormat(OutputStream os, String format, Object... params) throws IOException {
		os.write(String.format(format, params).getBytes());
	}

	abstract protected void writeLayersEndRecord(Polygon polygon) throws IOException;

	abstract protected void writeSpeciesEndRecord(Polygon polygon) throws IOException;
}
