package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.io.FileResolver;

public interface OptionalControlMapSubResourceParser<T> extends ControlMapSubResourceParser<T> {
	
	@Override
	default void modify(Map<String, Object> control, FileResolver fileResolver)
			throws IOException, ResourceParseException {
		@SuppressWarnings("unchecked")
		var filename = (Optional<String>) control.get(getControlKey());
		if(filename.isPresent()) {
			try (InputStream data = fileResolver.resolve(filename.get())) {
				modify(control, data);
			}
		} else {
			defaultModify(control);
		}
		
	}
	
	default void defaultModify(Map<String, Object> control) {
		control.put(getControlKey(), defaultResult());
	}
	
	T defaultResult();
	
}
