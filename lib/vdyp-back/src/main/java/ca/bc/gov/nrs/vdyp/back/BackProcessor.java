package ca.bc.gov.nrs.vdyp.back;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ca.bc.gov.nrs.vdyp.application.Pass;
import ca.bc.gov.nrs.vdyp.application.Processor;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;

public class BackProcessor extends Processor<ForwardDebugSettings> {

	@Override
	protected BaseControlParser<ForwardDebugSettings> getControlFileParser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void process(
			Set<Pass> vdypPassSet, Map<String, Object> controlMap, Optional<FileResolver> outputFileResolver
	) throws ProcessingException {
		// TODO Auto-generated method stub

	}

}
