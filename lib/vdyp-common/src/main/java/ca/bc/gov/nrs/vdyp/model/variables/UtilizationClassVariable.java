package ca.bc.gov.nrs.vdyp.model.variables;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

/**
 * Represents fields that have a UtilizationVector as a type
 */
public enum UtilizationClassVariable implements Variable<VdypUtilizationHolder, UtilizationVector> {
	LOREY_HEIGHT(
			"LoreyHeight", VdypUtilizationHolder::getLoreyHeightByUtilization,
			VdypUtilizationHolder::setLoreyHeightByUtilization
	) {
		// Lorey Height only has 2 classes instead of the usual 6
		@Override
		public Set<UtilizationClass> getClasses() {
			return HEIGHT_CLASSES;
		}
	},
	BASAL_AREA(
			"BaseArea", VdypUtilizationHolder::getBaseAreaByUtilization, VdypUtilizationHolder::setBaseAreaByUtilization
	),
	QUAD_MEAN_DIAMETER(
			"QuadraticMeanDiameter", VdypUtilizationHolder::getQuadraticMeanDiameterByUtilization,
			VdypUtilizationHolder::setQuadraticMeanDiameterByUtilization
	),
	TREES_PER_HECTARE(
			"TreesPerHectare", VdypUtilizationHolder::getTreesPerHectareByUtilization,
			VdypUtilizationHolder::setTreesPerHectareByUtilization
	),

	WHOLE_STEM_VOL(
			"WholeStemVolume", VdypUtilizationHolder::getWholeStemVolumeByUtilization,
			VdypUtilizationHolder::setWholeStemVolumeByUtilization
	),
	CLOSE_UTIL_VOL(
			"CloseUtilizationVolume", VdypUtilizationHolder::getCloseUtilizationVolumeByUtilization,
			VdypUtilizationHolder::setCloseUtilizationVolumeByUtilization
	),
	CLOSE_UTIL_VOL_LESS_DECAY(
			"CloseUtilizationVolumeNetOfDecay", VdypUtilizationHolder::getCloseUtilizationVolumeNetOfDecayByUtilization,
			VdypUtilizationHolder::setCloseUtilizationVolumeNetOfDecayByUtilization
	),
	CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE(
			"CloseUtilizationVolumeNetOfDecayAndWaste",
			VdypUtilizationHolder::getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization,
			VdypUtilizationHolder::setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization
	),
	CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE_LESS_BREAKAGE(
			"CloseUtilizationVolumeNetOfDecayWasteAndBreakage",
			VdypUtilizationHolder::getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization,
			VdypUtilizationHolder::setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
	);

	private static final Set<UtilizationClass> STANDARD_CLASSES = EnumSet.allOf(UtilizationClass.class);
	private static final Set<UtilizationClass> HEIGHT_CLASSES = EnumSet
			.of(UtilizationClass.SMALL, UtilizationClass.ALL);

	public static final Set<UtilizationClassVariable> VOLUME_VARIABLES = EnumSet.of(
			WHOLE_STEM_VOL, CLOSE_UTIL_VOL, CLOSE_UTIL_VOL_LESS_DECAY, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE,
			CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE_LESS_BREAKAGE
	);

	private final String shortName;
	private final String longName;
	private final Function<VdypUtilizationHolder, UtilizationVector> getter;
	private final BiConsumer<VdypUtilizationHolder, UtilizationVector> setter;

	private UtilizationClassVariable(
			String shortName, Function<VdypUtilizationHolder, UtilizationVector> getter,
			BiConsumer<VdypUtilizationHolder, UtilizationVector> setter
	) {
		this.shortName = shortName;
		// Pre-computing it at init instead of using the default to improve performance
		this.longName = shortName + "ByUtilization";
		this.getter = getter;
		this.setter = setter;
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

	@Override
	public void set(VdypUtilizationHolder parent, UtilizationVector value) {
		setter.accept(parent, value);
	}
}
