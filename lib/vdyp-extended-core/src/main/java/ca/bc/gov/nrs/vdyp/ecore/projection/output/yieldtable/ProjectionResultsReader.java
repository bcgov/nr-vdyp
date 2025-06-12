package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.util.Map;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public interface ProjectionResultsReader {

	Map<Integer, VdypPolygon> read(Polygon polygon) throws YieldTableGenerationException;
}
