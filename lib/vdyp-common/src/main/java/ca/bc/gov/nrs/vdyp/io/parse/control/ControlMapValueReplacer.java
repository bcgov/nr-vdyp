package ca.bc.gov.nrs.vdyp.io.parse.control;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseValidException;

public interface ControlMapValueReplacer<Result, Raw> extends ControlMapModifier {

	/**
	 * Remap a raw control value
	 *
	 * @param rawValue
	 * @return
	 * @throws ResourceParseException
	 * @throws IOException
	 */
	public Result map(Raw rawValue, FileResolver fileResolver, Map<String, Object> control)
			throws ResourceParseException, IOException;

	@Override
	default void modify(Map<String, Object> control, Map<String, FileResolver> fileResolverContext)
			throws ResourceParseException, IOException {

		Optional<Raw> source = Utils.optSafe(control.get(this.getControlKeyName()));
		if (source.isPresent()) {
			control.put(
					getControlKeyName(), this.map(source.get(), getFileResolver(fileResolverContext), control)
			);
		} else {
			control.put(getControlKeyName(), defaultModification(control));
		}

	}

	default FileResolver getFileResolver(Map<String, FileResolver> fileResolverContext) {
		return fileResolverContext.getOrDefault(
				getControlKeyName(), fileResolverContext.get(null)
		);
	}

	/**
	 * Override this to provide appropriate behavior when the key is not present
	 *
	 * @return
	 * @throws ResourceParseValidException
	 */
	default Result defaultModification(Map<String, Object> control) throws ResourceParseValidException {
		throw new ResourceParseValidException(
				"Expected " + this.getControlKeyName()
						+ this.getControlKey().sequence.map(x -> "(" + x + ")").orElse("") + " but it was not present."
		);
	}

}
