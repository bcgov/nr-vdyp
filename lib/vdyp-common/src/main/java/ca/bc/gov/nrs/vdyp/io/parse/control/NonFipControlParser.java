package ca.bc.gov.nrs.vdyp.io.parse.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BasalAreaYieldParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.QuadraticMeanDiameterYieldParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.UpperBoundsParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;

public abstract class NonFipControlParser<S extends DebugSettings> extends StartApplicationControlParser<S> {

	private static final Logger log = LoggerFactory.getLogger(NonFipControlParser.class);

	List<ControlMapModifier> nonFipStart = Arrays.asList(
			// RD_E106
			new BasalAreaYieldParser(),
			// RD_E106
			new QuadraticMeanDiameterYieldParser(),
			// RD_E106
			new UpperBoundsParser()
	);

	protected NonFipControlParser() {
		super();

		this.controlParser //

				.record(ControlKey.BA_YIELD, FILENAME) // COE106
				.record(ControlKey.DQ_YIELD, FILENAME) // COE107
				.record(ControlKey.BA_DQ_UPPER_BOUNDS, FILENAME) // COE108

				.record(
						ControlKey.MINIMA,
						ValueParser.toMap(
								ValueParser.list(ValueParser.FLOAT),
								Collections.singletonMap(MINIMUM_VETERAN_HEIGHT, 10.0f), MINIMUM_HEIGHT, // VMINH
								MINIMUM_BASE_AREA, // VMINBA
								MINIMUM_PREDICTED_BASE_AREA, // VMINBAeqn
								MINIMUM_VETERAN_HEIGHT // VMINVetH
						)
				)

		;
	}

	/**
	 * The data file parser factories and output file locations are applied afterwards, by {@link #applyRunModifiers}.
	 */
	@Override
	protected List<ControlMapModifier> sharedModifiers() {
		List<ControlMapModifier> modifiers = new ArrayList<>();

		modifiers.addAll(basicDefinitions);

		// Read Groups

		modifiers.addAll(groupDefinitions);

		modifiers.addAll(siteCurves);

		// Coeff for Empirical relationships

		modifiers.addAll(coefficients);

		// Initiation items NOT for FIPSTART

		modifiers.addAll(nonFipStart);

		// RD_E198
		modifiers.addAll(additionalModifiers);

		return modifiers;
	}

	@Override
	protected ValueParser<Map<String, Float>> minimaParser() {
		return ValueParser.callback(
				ValueParser.toMap(
						ValueParser.list(ValueParser.FLOAT), Collections.singletonMap(MINIMUM_VETERAN_HEIGHT, 10.0f),
						MINIMUM_HEIGHT, MINIMUM_BASE_AREA, MINIMUM_PREDICTED_BASE_AREA, MINIMUM_VETERAN_HEIGHT
				), minima -> {
					log.atDebug().setMessage(
							"Minima read from VRISTART Control at line {}\n  Minimum Height: {}\n  Minimum BA: {}\n  Minimum Predicted BA: {}\n  Minimum Veteran Height: {}"
					)//
							.addArgument(ControlKey.MINIMA.sequence.map(i -> Integer.toString(i)).orElse("N/A"))
							.addArgument(minima.get(MINIMUM_HEIGHT)) //
							.addArgument(minima.get(MINIMUM_BASE_AREA)) //
							.addArgument(minima.get(MINIMUM_PREDICTED_BASE_AREA)) //
							.addArgument(minima.get(MINIMUM_VETERAN_HEIGHT));
				}
		);
	}

}