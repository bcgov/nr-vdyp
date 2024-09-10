package ca.bc.gov.nrs.vdyp.forward;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Sp64DistributionSet;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSite;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

class Bank {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(Bank.class);

	private final VdypLayer layer;
	private final BecDefinition becZone;

	/**
	 * The number of species in the state. Note that all arrays have this value plus one elements in them; the element
	 * at index 0 is unused for the species values* and contains the default utilization in the Utilization values.
	 *
	 * (*) except: siteCurveNumbers[0] is used to store the site curve of the primary species.
	 */
	private int nSpecies; // BANK1 NSPB
	private int[] indices;

	// Species information

	public final String[/* nSpecies + 1 */] speciesNames; // BANK2 SP0B
	public final Sp64DistributionSet[/* nSpecies + 1 */] sp64Distributions; // BANK2 SP64DISTB
	public final float[/* nSpecies + 1 */] siteIndices; // BANK3 SIB
	public final float[/* nSpecies + 1 */] dominantHeights; // BANK3 HDB
	public final float[/* nSpecies + 1 */] ageTotals; // BANK3 AGETOTB
	public final float[/* nSpecies + 1 */] yearsAtBreastHeight; // BANK3 AGEBHB
	public final float[/* nSpecies + 1 */] yearsToBreastHeight; // BANK3 YTBHB
	public final int[/* nSpecies + 1 */] siteCurveNumbers; // BANK3 SCNB
	public final int[/* nSpecies + 1 */] speciesIndices; // BANK1 ISPB
	public final float[/* nSpecies + 1 */] percentagesOfForestedLand; // BANK1 PCTB

	// Utilization information, per Species

	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] basalAreas; // BANK1 BAB. Units: m^2/hectare
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] closeUtilizationVolumes; // BANK1 VOLCUB
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] cuVolumesMinusDecay; // BANK1 VOL_DB
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] cuVolumesMinusDecayAndWastage; // BANK1 VOL_DW_B
	public final float[/* nSpecies + 1, including 0 */][/* uc -1 and 0 only */] loreyHeights; // BANK1 HLB
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] quadMeanDiameters; // BANK1 DQB
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] treesPerHectare; // BANK1 TPHB
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] wholeStemVolumes; // BANK1 VOLWSB

	public Bank(VdypLayer layer, BecDefinition becZone, Predicate<VdypSpecies> retainCriteria)
			throws ProcessingException {

		this.layer = layer;
		this.becZone = becZone;

		List<VdypSpecies> speciesToRetain = layer.getSpecies().values().stream().filter(s -> retainCriteria.test(s))
				.sorted((s1, s2) -> s1.getGenusIndex() - s2.getGenusIndex()).toList();

		this.nSpecies = speciesToRetain.size();
		this.indices = IntStream.range(1, nSpecies + 1).toArray();

		// In the following, index 0 is unused
		speciesNames = new String[nSpecies + 1];
		sp64Distributions = new Sp64DistributionSet[getNSpecies() + 1];
		siteIndices = new float[nSpecies + 1];
		dominantHeights = new float[nSpecies + 1];
		ageTotals = new float[nSpecies + 1];
		yearsAtBreastHeight = new float[nSpecies + 1];
		yearsToBreastHeight = new float[nSpecies + 1];
		siteCurveNumbers = new int[nSpecies + 1];
		speciesIndices = new int[nSpecies + 1];
		percentagesOfForestedLand = new float[nSpecies + 1];

		int nUtilizationClasses = UtilizationClass.values().length;

		// In the following, index 0 is used for the default species utilization
		basalAreas = new float[nSpecies + 1][nUtilizationClasses];
		closeUtilizationVolumes = new float[nSpecies + 1][nUtilizationClasses];
		cuVolumesMinusDecay = new float[nSpecies + 1][nUtilizationClasses];
		cuVolumesMinusDecayAndWastage = new float[nSpecies + 1][nUtilizationClasses];
		loreyHeights = new float[nSpecies + 1][2];
		quadMeanDiameters = new float[nSpecies + 1][nUtilizationClasses];
		treesPerHectare = new float[nSpecies + 1][nUtilizationClasses];
		wholeStemVolumes = new float[nSpecies + 1][nUtilizationClasses];

		transferUtilizationSetIntoBank(0, layer);

		int nextSlot = 1;
		for (VdypSpecies s : speciesToRetain) {
			transferSpeciesIntoBank(nextSlot++, s);
		}
	}

	public Bank(Bank source) {

		this.becZone = source.becZone;
		this.layer = source.layer;

		this.nSpecies = source.nSpecies;
		this.indices = copy(source.indices);
		this.speciesNames = copy(source.speciesNames);
		this.speciesIndices = copy(source.speciesIndices);

		this.siteCurveNumbers = copy(source.siteCurveNumbers);
		this.sp64Distributions = copy(source.sp64Distributions);

		this.ageTotals = copy(source.ageTotals);
		this.dominantHeights = copy(source.dominantHeights);
		this.percentagesOfForestedLand = copy(source.percentagesOfForestedLand);
		this.siteIndices = copy(source.siteIndices);
		this.yearsAtBreastHeight = copy(source.yearsAtBreastHeight);
		this.yearsToBreastHeight = copy(source.yearsToBreastHeight);

		this.basalAreas = copy(source.basalAreas);
		this.closeUtilizationVolumes = copy(source.closeUtilizationVolumes);
		this.cuVolumesMinusDecay = copy(source.cuVolumesMinusDecay);
		this.cuVolumesMinusDecayAndWastage = copy(source.cuVolumesMinusDecayAndWastage);
		this.loreyHeights = copy(source.loreyHeights);
		this.quadMeanDiameters = copy(source.quadMeanDiameters);
		this.treesPerHectare = copy(source.treesPerHectare);
		this.wholeStemVolumes = copy(source.wholeStemVolumes);
	}

	public int getNSpecies() {
		return nSpecies;
	}

	int[] getIndices() {
		return indices;
	}

	BecDefinition getBecZone() {
		return becZone;
	}

	/**
	 * Refresh the values in the bank with an updated version (given) of the layer used to create the bank. The
	 * modifications cannot include any changes to the set of species, although the details of those species may of
	 * course change.
	 *
	 * @param layer a (presumably modified) version of the layer.
	 * @throws ProcessingException
	 */
	void refreshBank(VdypLayer layer) throws ProcessingException {

		if (!this.layer.equals(layer)) {
			throw new IllegalArgumentException(
					MessageFormat.format(
							"One cannot refresh a bank from a"
									+ " layer ({0}) different from the one used to create the bank ({1})",
							this.layer, layer
					)
			);
		}

		List<VdypSpecies> species = layer.getSpecies().values().stream()
				.sorted((s1, s2) -> s1.getGenusIndex() - s2.getGenusIndex()).toList();

		transferUtilizationSetIntoBank(0, layer);

		int nextSlot = 1;
		for (VdypSpecies s : species) {
			transferSpeciesIntoBank(nextSlot++, s);
		}
	}

	private void transferSpeciesIntoBank(int index, VdypSpecies species) throws ProcessingException {

		speciesNames[index] = species.getGenus();
		sp64Distributions[index] = species.getSp64DistributionSet();
		speciesIndices[index] = species.getGenusIndex();

		species.getSite().ifPresentOrElse(s -> {
			siteIndices[index] = s.getSiteIndex().orElse(VdypEntity.MISSING_FLOAT_VALUE);
			dominantHeights[index] = s.getHeight().orElse(VdypEntity.MISSING_FLOAT_VALUE);
			ageTotals[index] = s.getAgeTotal().orElse(VdypEntity.MISSING_FLOAT_VALUE);
			yearsToBreastHeight[index] = s.getYearsToBreastHeight().orElse(VdypEntity.MISSING_FLOAT_VALUE);
			if (ageTotals[index] != VdypEntity.MISSING_FLOAT_VALUE
					&& yearsToBreastHeight[index] != VdypEntity.MISSING_FLOAT_VALUE) {
				yearsAtBreastHeight[index] = ageTotals[index] - yearsToBreastHeight[index];
			} else {
				yearsAtBreastHeight[index] = VdypEntity.MISSING_FLOAT_VALUE;
			}
			siteCurveNumbers[index] = s.getSiteCurveNumber().orElse(VdypEntity.MISSING_INTEGER_VALUE);
			// percentForestedLand is output-only and so not assigned here.
		}, () -> {
			siteIndices[index] = VdypEntity.MISSING_FLOAT_VALUE;
			dominantHeights[index] = VdypEntity.MISSING_FLOAT_VALUE;
			ageTotals[index] = VdypEntity.MISSING_FLOAT_VALUE;
			yearsToBreastHeight[index] = VdypEntity.MISSING_FLOAT_VALUE;
			yearsAtBreastHeight[index] = VdypEntity.MISSING_FLOAT_VALUE;
			siteCurveNumbers[index] = VdypEntity.MISSING_INTEGER_VALUE;
		});

		transferUtilizationSetIntoBank(index, species);
	}

	private void transferUtilizationSetIntoBank(int index, VdypUtilizationHolder uh) {

		for (UtilizationClass uc : UtilizationClass.values()) {
			int ucIndex = uc.ordinal();
			basalAreas[index][ucIndex] = uh.getBaseAreaByUtilization().get(uc);
			closeUtilizationVolumes[index][ucIndex] = uh.getCloseUtilizationVolumeByUtilization().get(uc);
			cuVolumesMinusDecay[index][ucIndex] = uh.getCloseUtilizationVolumeNetOfDecayByUtilization().get(uc);
			cuVolumesMinusDecayAndWastage[index][ucIndex] = uh
					.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().get(uc);
			if (ucIndex < 2 /* only uc 0 and 1 have a lorey height */) {
				loreyHeights[index][ucIndex] = uh.getLoreyHeightByUtilization().get(uc);
			}
			quadMeanDiameters[index][ucIndex] = uh.getQuadraticMeanDiameterByUtilization().get(uc);
			treesPerHectare[index][ucIndex] = uh.getTreesPerHectareByUtilization().get(uc);
			wholeStemVolumes[index][ucIndex] = uh.getWholeStemVolumeByUtilization().get(uc);
		}
	}

	/**
	 * This method copies the Bank contents out to the VdypLayer instance used to create it and returns that. It is a
	 * relatively expensive operation and should not be called without due consideration.
	 *
	 * @return as described
	 */
	VdypLayer getLayer() {

		transferUtilizationsFromBank(0, layer);

		for (int i : indices) {
			transferSpeciesFromBank(i, layer.getSpecies().get(speciesNames[i]));
		}

		return layer;
	}

	private VdypSpecies transferSpeciesFromBank(int index, VdypSpecies species) {

		VdypSpecies newSpecies = VdypSpecies.build(speciesBuilder -> {
			speciesBuilder.adapt(species);
			speciesBuilder.percentGenus(this.percentagesOfForestedLand[index]);
			species.getSite().ifPresent(site -> speciesBuilder.addSite(VdypSite.build(siteBuilder -> {
				siteBuilder.adapt(site);
				siteBuilder.ageTotal(this.ageTotals[index]);
				siteBuilder.height(this.dominantHeights[index]);
				siteBuilder.siteCurveNumber(this.siteCurveNumbers[index]);
				siteBuilder.siteGenus(this.speciesNames[index]);
				siteBuilder.siteIndex(this.siteIndices[index]);
				siteBuilder.yearsToBreastHeight(this.yearsToBreastHeight[index]);
			})));
		});

		transferUtilizationsFromBank(index, newSpecies);

		return newSpecies;
	}

	private void transferUtilizationsFromBank(int index, VdypUtilizationHolder uh) {

		for (UtilizationClass uc : UtilizationClass.values()) {
			int ucIndex = uc.ordinal();
			uh.getBaseAreaByUtilization().set(uc, basalAreas[index][ucIndex]);
			uh.getCloseUtilizationVolumeByUtilization().set(uc, closeUtilizationVolumes[index][ucIndex]);
			uh.getCloseUtilizationVolumeNetOfDecayByUtilization().set(uc, cuVolumesMinusDecay[index][ucIndex]);
			uh.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization()
					.set(uc, cuVolumesMinusDecayAndWastage[index][ucIndex]);
			if (ucIndex < 2 /* only uc 0 and 1 have a lorey height */) {
				uh.getLoreyHeightByUtilization().set(uc, loreyHeights[index][ucIndex]);
			}
			uh.getQuadraticMeanDiameterByUtilization().set(uc, quadMeanDiameters[index][ucIndex]);
			uh.getTreesPerHectareByUtilization().set(uc, treesPerHectare[index][ucIndex]);
			uh.getWholeStemVolumeByUtilization().set(uc, wholeStemVolumes[index][ucIndex]);
		}
	}

	public Bank copy() {
		return new Bank(this);
	}

	private String[] copy(String[] a) {
		return Arrays.stream(a).toArray(String[]::new);
	}

	private int[] copy(int[] a) {
		int[] t = new int[a.length];

		System.arraycopy(a, 0, t, 0, a.length);

		return t;
	}

	private float[] copy(float[] a) {
		float[] t = new float[a.length];

		System.arraycopy(a, 0, t, 0, a.length);

		return t;
	}

	private float[][] copy(float[][] a) {
		return Arrays.stream(a).map(float[]::clone).toArray(float[][]::new);
	}

	private Sp64DistributionSet[] copy(Sp64DistributionSet[] sp64Distributions) {
		return Arrays.stream(sp64Distributions).map(s -> s == null ? null : s.copy())
				.toArray(Sp64DistributionSet[]::new);
	}
}