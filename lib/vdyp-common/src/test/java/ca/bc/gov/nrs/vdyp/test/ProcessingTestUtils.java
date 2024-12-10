package ca.bc.gov.nrs.vdyp.test;

import java.util.List;

import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;

public class ProcessingTestUtils {
	public static VdypLayer normalizeLayer(VdypLayer layer) {

		// Set uc All to the sum of the UC values, UC 7.5 and above only, for the summable
		// values, and calculate quad-mean-diameter from these values.

		UtilizationClass ucAll = UtilizationClass.ALL;
		UtilizationClass ucSmall = UtilizationClass.SMALL;

		for (var species : layer.getSpecies().values()) {

			species.getBaseAreaByUtilization().set(
					ucAll, sumUtilizationClassValues(species.getBaseAreaByUtilization(), UtilizationClass.UTIL_CLASSES)
			);
			species.getTreesPerHectareByUtilization().set(
					ucAll,
					sumUtilizationClassValues(species.getTreesPerHectareByUtilization(), UtilizationClass.UTIL_CLASSES)
			);
			species.getWholeStemVolumeByUtilization().set(
					ucAll,
					sumUtilizationClassValues(species.getWholeStemVolumeByUtilization(), UtilizationClass.UTIL_CLASSES)
			);
			species.getCloseUtilizationVolumeByUtilization().set(
					ucAll,
					sumUtilizationClassValues(
							species.getCloseUtilizationVolumeByUtilization(), UtilizationClass.UTIL_CLASSES
					)
			);
			species.getCloseUtilizationVolumeNetOfDecayByUtilization().set(
					ucAll,
					sumUtilizationClassValues(
							species.getCloseUtilizationVolumeNetOfDecayByUtilization(), UtilizationClass.UTIL_CLASSES
					)
			);
			species.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().set(
					ucAll,
					sumUtilizationClassValues(
							species.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(),
							UtilizationClass.UTIL_CLASSES
					)
			);

			if (species.getBaseAreaByUtilization().get(ucAll) > 0.0f) {
				species.getQuadraticMeanDiameterByUtilization().set(
						ucAll,
						BaseAreaTreeDensityDiameter.quadMeanDiameter(
								species.getBaseAreaByUtilization().get(ucAll),
								species.getTreesPerHectareByUtilization().get(ucAll)
						)
				);
			}
		}

		// Set the layer's uc All values (for summable types) to the sum of those of the
		// individual species.

		layer.getBaseAreaByUtilization()
				.set(ucAll, sumSpeciesUtilizationClassValues(layer, "BaseArea", UtilizationClass.ALL));
		layer.getTreesPerHectareByUtilization()
				.set(ucAll, sumSpeciesUtilizationClassValues(layer, "TreesPerHectare", UtilizationClass.ALL));
		layer.getWholeStemVolumeByUtilization()
				.set(ucAll, sumSpeciesUtilizationClassValues(layer, "WholeStemVolume", UtilizationClass.ALL));
		layer.getCloseUtilizationVolumeByUtilization()
				.set(ucAll, sumSpeciesUtilizationClassValues(layer, "CloseUtilizationVolume", UtilizationClass.ALL));
		layer.getCloseUtilizationVolumeNetOfDecayByUtilization().set(
				ucAll, sumSpeciesUtilizationClassValues(layer, "CloseUtilizationVolumeNetOfDecay", UtilizationClass.ALL)
		);
		layer.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().set(
				ucAll,
				sumSpeciesUtilizationClassValues(
						layer, "CloseUtilizationVolumeNetOfDecayAndWaste", UtilizationClass.ALL
				)
		);

		// Calculate the layer's uc All values for quad-mean-diameter and lorey height

		float sumLoreyHeightByBasalAreaSmall = 0.0f;
		float sumBasalAreaSmall = 0.0f;
		float sumLoreyHeightByBasalAreaAll = 0.0f;

		for (var species : layer.getSpecies().values()) {
			sumLoreyHeightByBasalAreaSmall += species.getLoreyHeightByUtilization().get(ucSmall)
					* species.getBaseAreaByUtilization().get(ucSmall);
			sumBasalAreaSmall += species.getBaseAreaByUtilization().get(ucSmall);
			sumLoreyHeightByBasalAreaAll += species.getLoreyHeightByUtilization().get(ucAll)
					* species.getBaseAreaByUtilization().get(ucAll);
		}

		if (layer.getBaseAreaByUtilization().get(ucAll) > 0.0f) {
			layer.getQuadraticMeanDiameterByUtilization().set(
					ucAll,
					BaseAreaTreeDensityDiameter.quadMeanDiameter(
							layer.getBaseAreaByUtilization().get(ucAll),
							layer.getTreesPerHectareByUtilization().get(ucAll)
					)
			);
			layer.getLoreyHeightByUtilization()
					.set(ucAll, sumLoreyHeightByBasalAreaAll / layer.getBaseAreaByUtilization().get(ucAll));
		}

		// Calculate the layer's lorey height uc Small value

		if (sumBasalAreaSmall > 0.0f) {
			layer.getLoreyHeightByUtilization().set(ucSmall, sumLoreyHeightByBasalAreaSmall / sumBasalAreaSmall);
		}

		// Finally, set the layer's summable UC values (other than All, which was computed above) to
		// the sums of those of each of the species.

		for (UtilizationClass uc : UtilizationClass.ALL_CLASSES) {
			layer.getBaseAreaByUtilization().set(uc, sumSpeciesUtilizationClassValues(layer, "BaseArea", uc));
			layer.getTreesPerHectareByUtilization()
					.set(uc, sumSpeciesUtilizationClassValues(layer, "TreesPerHectare", uc));
			layer.getWholeStemVolumeByUtilization()
					.set(uc, sumSpeciesUtilizationClassValues(layer, "WholeStemVolume", uc));
			layer.getCloseUtilizationVolumeByUtilization()
					.set(uc, sumSpeciesUtilizationClassValues(layer, "CloseUtilizationVolume", uc));
			layer.getCloseUtilizationVolumeNetOfDecayByUtilization()
					.set(uc, sumSpeciesUtilizationClassValues(layer, "CloseUtilizationVolumeNetOfDecay", uc));
			layer.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization()
					.set(uc, sumSpeciesUtilizationClassValues(layer, "CloseUtilizationVolumeNetOfDecayAndWaste", uc));
		}

		return layer;
	}

	private static float sumUtilizationClassValues(UtilizationVector ucValues, List<UtilizationClass> subjects) {
		float sum = 0.0f;

		for (UtilizationClass uc : UtilizationClass.values()) {
			if (subjects.contains(uc)) {
				sum += ucValues.get(uc);
			}
		}

		return sum;
	}

	private static float sumSpeciesUtilizationClassValues(VdypLayer layer, String uvName, UtilizationClass uc) {
		float sum = 0.0f;

		for (VdypSpecies species : layer.getSpecies().values()) {
			switch (uvName) {
			case "CloseUtilizationVolumeNetOfDecayWasteAndBreakage":
				sum += species.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization().get(uc);
				break;
			case "CloseUtilizationVolumeNetOfDecayAndWaste":
				sum += species.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().get(uc);
				break;
			case "CloseUtilizationVolumeNetOfDecay":
				sum += species.getCloseUtilizationVolumeNetOfDecayByUtilization().get(uc);
				break;
			case "CloseUtilizationVolume":
				sum += species.getCloseUtilizationVolumeByUtilization().get(uc);
				break;
			case "WholeStemVolume":
				sum += species.getWholeStemVolumeByUtilization().get(uc);
				break;
			case "TreesPerHectare":
				sum += species.getTreesPerHectareByUtilization().get(uc);
				break;
			case "BaseArea":
				sum += species.getBaseAreaByUtilization().get(uc);
				break;
			default:
				throw new IllegalStateException(uvName + " is not a known utilization vector name");
			}
		}

		return sum;
	}
}
