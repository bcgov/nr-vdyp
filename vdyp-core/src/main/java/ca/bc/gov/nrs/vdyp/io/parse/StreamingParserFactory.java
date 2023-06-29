package ca.bc.gov.nrs.vdyp.io.parse;

import java.util.function.Supplier;

public interface StreamingParserFactory<T> extends Supplier<StreamingParser<T>>{

	/**
	 * Open the resource and get a streaming parser for it.  This must be closed.
	 */
	@Override
	StreamingParser<T> get();
	
}
