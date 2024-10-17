package ca.bc.gov.nrs.vdyp.back;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import ca.bc.gov.nrs.vdyp.application.Pass;
import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.application.Processor;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class BackProcessor extends Processor {

	@Override
	protected BaseControlParser getControlFileParser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void process(
			Set<Pass> vdypPassSet, Map<String, Object> controlMap, Optional<FileResolver> outputFileResolver,
			Predicate<VdypPolygon> polygonFilter
	) throws ProcessingException {
		// TODO Auto-generated method stub

	}

}
