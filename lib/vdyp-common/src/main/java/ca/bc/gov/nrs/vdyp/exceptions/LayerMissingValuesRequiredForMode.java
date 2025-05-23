package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;

/**
 * Equivalent to IPASS = -3 in VRIStart
 */
public class LayerMissingValuesRequiredForMode extends LayerValidationException {

	private static final long serialVersionUID = -345800984515714774L;

	static final String TEMPLATE = "{2} must be present and positive for a {0} layer in a polygon with mode {1}";

	@Nullable
	private final PolygonMode mode;
	private final ArrayList<String> missingValues;

	private static String getMessage(LayerType layer, Optional<PolygonMode> mode, List<String> values) {
		return MessageFormat
				.format(TEMPLATE, layer, Utils.optPretty(mode), Utils.prettyList(values, "and", Object::toString));

	}

	public LayerMissingValuesRequiredForMode(RuntimeProcessingException ex) {
		this(ex, unwrap(ex, LayerMissingValuesRequiredForMode.class));
	}

	private LayerMissingValuesRequiredForMode(
			RuntimeProcessingException wrapper, LayerMissingValuesRequiredForMode unwrapped
	) {
		super(unwrapped.getLayer(), unwrapped.getMessage(), wrapper);
		this.mode = unwrapped.mode;
		this.missingValues = unwrapped.missingValues;
	}

	public LayerMissingValuesRequiredForMode(LayerType layer, Optional<PolygonMode> mode, List<String> values) {
		super(layer, getMessage(layer, mode, values));
		this.mode = mode.orElse(null);
		ArrayList<String> list = new ArrayList<>(values.size());
		list.addAll(values);
		this.missingValues = list;
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VRI_START)
			return Optional.of(-3);
		return Optional.empty();
	}

	public Optional<PolygonMode> getMode() {
		return Optional.ofNullable(this.mode);
	}

	public List<String> getMissingValues() {
		return Collections.unmodifiableList(this.missingValues);
	}

}
