package ca.bc.gov.nrs.vdyp.fip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.fip.model.FipDebugSettings;
import ca.bc.gov.nrs.vdyp.io.parse.coe.DebugSettingsParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.ModifierParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.StockingClassFactorParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapModifier;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapValueReplacer;
import ca.bc.gov.nrs.vdyp.io.parse.control.OutputFileLocationResolver;
import ca.bc.gov.nrs.vdyp.io.parse.control.StartApplicationControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;

/**
 * Parser for FIP control files
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class FipControlParser extends StartApplicationControlParser<FipDebugSettings> {
	private static final Logger log = LoggerFactory.getLogger(FipControlParser.class);

	public FipControlParser() {

		initialize();

		this.controlParser //
				// FIP only
				.record(ControlKey.STOCKING_CLASS_FACTORS, FILENAME) // RD_STK33
		;
	}

	@Override
	protected ValueParser<Map<String, Float>> minimaParser() {
		return ValueParser.callback(
				ValueParser.toMap(
						ValueParser.list(ValueParser.FLOAT), Collections.singletonMap(MINIMUM_VETERAN_HEIGHT, 10.0f),
						MINIMUM_HEIGHT, MINIMUM_BASE_AREA, MINIMUM_FULLY_STOCKED_AREA, MINIMUM_VETERAN_HEIGHT
				), minima -> {
					log.atDebug().setMessage(
							"Minima read from FIPSTART Control at line {}\n  Minimum Height: {}\n  Minimum BA: {}\n  Minimum  BA Fully Stocked: {}\n  Minimum Veteran Height: {}"
					)//
							.addArgument(ControlKey.MINIMA.sequence.map(i -> Integer.toString(i)).orElse("N/A"))
							.addArgument(minima.get(MINIMUM_HEIGHT)) //
							.addArgument(minima.get(MINIMUM_BASE_AREA)) //
							.addArgument(minima.get(MINIMUM_FULLY_STOCKED_AREA)) //
							.addArgument(minima.get(MINIMUM_VETERAN_HEIGHT));
				}
		);
	}

	List<ControlMapModifier> fipstartOnly = Arrays.asList(

			// RD_STK33
			new StockingClassFactorParser()
	);

	List<ControlMapModifier> additionalModifiers = Arrays.asList(

			// RD_E198
			new ModifierParser(VdypApplicationIdentifier.VRI_START)
	);

	/**
	 * The data file parser factories and output file locations are applied afterwards, by {@link #applyRunModifiers}.
	 */
	@Override
	protected List<ControlMapModifier> sharedModifiers() {
		List<ControlMapModifier> modifiers = new ArrayList<>();

		modifiers.addAll(basicDefinitions);

		// Read Groups

		modifiers.addAll(groupDefinitions);

		modifiers.addAll(fipstartOnly);

		modifiers.addAll(siteCurves);

		// Coeff for Empirical relationships

		modifiers.addAll(coefficients);

		// Modifiers, IPSJF155-Appendix XII

		// RD_E198
		modifiers.addAll(additionalModifiers);

		return modifiers;
	}

	@Override
	protected VdypApplicationIdentifier getProgramId() {
		return VdypApplicationIdentifier.FIP_START;
	}

	@Override
	protected List<ControlMapValueReplacer<Object, String>> inputFileParsers() {
		return inputParserList(

				// V7O_FIP
				new FipPolygonParser(),

				// V7O_FIL
				new FipLayerParser(),

				// V7O_FIS
				new FipSpeciesParser()
		);
	}

	@Override
	protected List<OutputFileLocationResolver> outputFiles() {
		return List.of(
				new OutputFileLocationResolver(ControlKey.VDYP_OUTPUT_VDYP_POLYGON), //
				new OutputFileLocationResolver(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SPECIES), //
				new OutputFileLocationResolver(ControlKey.VDYP_OUTPUT_VDYP_LAYER_BY_SP0_BY_UTIL)
		);
	}

	@Override
	protected DebugSettingsParser<FipDebugSettings> getDebugSettingsParser() {
		return new DebugSettingsParser<FipDebugSettings>() {

			@Override
			protected FipDebugSettings build(Integer[] debugSettingsValues) {
				return new FipDebugSettings(debugSettingsValues);
			}
		};
	}

}
