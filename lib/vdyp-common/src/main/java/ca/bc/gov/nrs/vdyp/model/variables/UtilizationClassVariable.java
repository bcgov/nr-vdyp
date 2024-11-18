package ca.bc.gov.nrs.vdyp.model.variables;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

/**
 * Represents fields that have a UtilizationVector as a type
 */
public enum UtilizationClassVariable implements Property<VdypUtilizationHolder, UtilizationVector> {
	LOREY_HEIGHT("LoreyHeight", VdypUtilizationHolder::getLoreyHeightByUtilization) {
		// Lorey Height only has 2 classes instead of the usual 6
		@Override
		public Set<UtilizationClass> getClasses() {
			return HEIGHT_CLASSES;
		}
	},
	BASAL_AREA("BaseArea", VdypUtilizationHolder::getBaseAreaByUtilization),
	QUAD_MEAN_DIAMETER("QuadraticMeanDiameter", VdypUtilizationHolder::getQuadraticMeanDiameterByUtilization),
	TREES_PER_HECTARE("TreesPerHectare", VdypUtilizationHolder::getTreesPerHectareByUtilization),

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

	static private final Set<UtilizationClass> STANDARD_CLASSES = EnumSet.allOf(UtilizationClass.class);
	static private final Set<UtilizationClass> HEIGHT_CLASSES = EnumSet
			.of(UtilizationClass.SMALL, UtilizationClass.ALL);

	private final String shortName;
	private final String longName;
	private final Function<VdypUtilizationHolder, UtilizationVector> getter;

	private UtilizationClassVariable(String shortName, Function<VdypUtilizationHolder, UtilizationVector> accessor) {
		this.shortName = shortName;
		// Pre-computing it at init instead of using the default to improve performance
		this.longName = shortName + "ByUtilization";
		this.getter = accessor;
	}

	@Override
	public UtilizationVector get(VdypUtilizationHolder parent) {
		return getter.apply(parent);
	}

	/**
	 * Short name of the property without "ByUtilization"
	 *
	 * @return
	 */
	public String getShortName() {
		return shortName;
	}

	@Override
	public String getName() {
		return longName;
	}

	/**
	 * The UtilizationClasses this field covers
	 *
	 * @return
	 */
	public Set<UtilizationClass> getClasses() {
		return STANDARD_CLASSES;
	}
}
