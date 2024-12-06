package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

public class HcsvPolygonStream extends AbstractPolygonStream {

	private CSVReader polygonCsvStream;
	private CSVReader layersCsvStream;

	public HcsvPolygonStream(InputStream polygonStream, InputStream layersStream) {
		polygonCsvStream = new CSVReaderBuilder(new BufferedReader(new InputStreamReader(polygonStream)))
                .withCSVParser(new CSVParserBuilder()
                        .withSeparator('\'')
                        .build())
                .withSkipLines(1)
                .build();
	}

	@Override
	Polygon getNextPolygon() {
		throw new UnsupportedOperationException("HCSV input files not (yet) supported.");
	}

	@Override
	public boolean hasNextPolygon() {
		return false;
	}
}
