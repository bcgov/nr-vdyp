package ca.bc.gov.nrs.vdyp.model;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public enum VolumeVariable {
	WHOLE_STEM_VOL("WholeStemVolume", VdypUtilizationHolder::getWholeStemVolumeByUtilization),
	CLOSE_UTIL_VOL("CloseUtilizationVolume", VdypUtilizationHolder::getCloseUtilizationVolumeByUtilization),
	CLOSE_UTIL_VOL_LESS_DECAY(
			"CloseUtilizationVolumeNetOfDecay", VdypUtilizationHolder::getCloseUtilizationVolumeNetOfDecayByUtilization
	),
	CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE(
			"CloseUtilizationVolumeNetOfDecayAndWaste",
			VdypUtilizationHolder::getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization
	),
	CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE_LESS_BREAKAGE(
			"CloseUtilizationVolumeNetOfDecayWasteAndBreakage",
			VdypUtilizationHolder::getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
	);

	private VolumeVariable(String name, Function<VdypUtilizationHolder, UtilizationVector> accessor) {
		this.name = name;
		this.accessor = accessor;
	}

	public final Function<VdypUtilizationHolder, UtilizationVector> accessor;
	public final String name;

	public static final List<VolumeVariable> ALL_BUT_NET_OF_BREAKAGE = Collections.unmodifiableList(
			List.of(
					WHOLE_STEM_VOL, 
					CLOSE_UTIL_VOL,
					CLOSE_UTIL_VOL_LESS_DECAY,
					CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE)
	);
	public static final List<VolumeVariable> ALL = Collections.unmodifiableList(
			List.of(
					WHOLE_STEM_VOL,
					CLOSE_UTIL_VOL,
					CLOSE_UTIL_VOL_LESS_DECAY, 
					CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
					CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE_LESS_BREAKAGE
			)
	);
}
