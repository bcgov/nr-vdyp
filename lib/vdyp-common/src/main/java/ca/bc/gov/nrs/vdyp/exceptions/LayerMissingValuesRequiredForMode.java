package ca.bc.gov.nrs.vdyp.exceptions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

	final Optional<PolygonMode> mode;
	final List<String> missingValues;

	private static String getMessage(LayerType layer, Optional<PolygonMode> mode, List<String> values) {
		return MessageFormat
				.format(TEMPLATE, layer, Utils.optPretty(mode), Utils.prettyList(values, "and", Object::toString));

	}

	public LayerMissingValuesRequiredForMode(RuntimeStandProcessingException ex) {
		this(ex, unwrap(ex, LayerMissingValuesRequiredForMode.class));
	}

	private LayerMissingValuesRequiredForMode(
			RuntimeStandProcessingException wrapper, LayerMissingValuesRequiredForMode unwrapped
	) {
		super(unwrapped.getLayer(), unwrapped.getMessage(), wrapper);
		this.mode = unwrapped.getMode();
		this.missingValues = unwrapped.getMissingValues();
	}

	public LayerMissingValuesRequiredForMode(LayerType layer, Optional<PolygonMode> mode, List<String> values) {
		super(layer, getMessage(layer, mode, values));
		this.mode = mode;
		List<String> list = new ArrayList<>(values.size());
		list.addAll(values);
		this.missingValues = Collections.unmodifiableList(list);
	}

	@Override
	public Optional<Integer> getIpassCode(VdypApplicationIdentifier app) {
		if (app == VdypApplicationIdentifier.VRI_START)
			return Optional.of(-3);
		return Optional.empty();
	}

	public Optional<PolygonMode> getMode() {
		return this.mode;
	}

	public List<String> getMissingValues() {
		return this.missingValues;
	}

}
