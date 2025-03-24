package ca.bc.gov.nrs.vdyp.backend.io.write;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.utils.Utils;

/**
 * Write the grow-to-year file used by Forward and Back to indicate the target years.
 */
public class VdypGrowToYearFileWriter implements Closeable {

	protected final OutputStream growToYearFile;

	/**
	 * Create a writer for the Forward and Back "grow-to-year" file. The stream will be closed when the writer is
	 * closed.
	 *
	 * @param growToYearFile
	 */
	public VdypGrowToYearFileWriter(OutputStream growToYearFile) {
		this.growToYearFile = growToYearFile;
	}

	public void writePolygon(Polygon polygon, int growthTarget) throws IOException {

		AbstractOutputWriter.writeFormat(
				growToYearFile, //
				AbstractOutputWriter.POLY_IDENTIFIER_FORMAT, //

				polygon.buildPolygonDescriptor(growthTarget) //
		);
	}

	@Override
	public void close() throws IOException {
		Utils.close(growToYearFile, "VdypGrowToYearFileWriter.growToYearFile");
	}
}
