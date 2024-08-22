package ca.bc.gov.nrs.vdyp.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.common.Computed;
import ca.bc.gov.nrs.vdyp.io.parse.coe.GenusDefinitionParser;

public abstract class BaseVdypSpecies<I extends BaseVdypSite> {
	private final PolygonIdentifier polygonIdentifier; // FIP_P/POLYDESC

	// This is also represents the distinction between data stored in
	// FIPL_1(A) and FIP_V(A). Where VDYP7 stores both and looks at certain values
	// to determine if a layer is "present". VDYP8 stores them in a map keyed by
	// this value.
	private final LayerType layerType;

	private final String genus; // FIPSA/SP0V

	/** the species index within species definition file (e.g. SPODEF_v0.dat) */
	private final int genusIndex; // BANK1/ISPB, L1COM1/ISPL1, etc.

	private float percentGenus; // FIPS/PCTVOLV L1COM1/PCTL1

	// This is computed from percentGenus, but VDYP7 computes it in a way that might
	// lead to a slight difference so it's stored separately and can be modified.
	@Computed
	private float fractionGenus; // FRBASP0/FR

	private Sp64DistributionSet sp64DistributionSet;

	private Optional<I> site;

	protected BaseVdypSpecies(
			PolygonIdentifier polygonIdentifier, LayerType layerType, String genus, int genusIndex, float percentGenus,
			Sp64DistributionSet sp64DistributionSet, Optional<I> site
	) {
		this.polygonIdentifier = polygonIdentifier;
		this.layerType = layerType;
		this.genus = genus;
		this.setPercentGenus(percentGenus);
		this.site = site;
		this.genusIndex = genusIndex;
		this.sp64DistributionSet = sp64DistributionSet;
	}

	public PolygonIdentifier getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public LayerType getLayerType() {
		return layerType;
	}

	public float getPercentGenus() {
		return percentGenus;
	}

	@Computed
	public float getFractionGenus() {
		return fractionGenus;
	}

	public void setPercentGenus(float percentGenus) {
		this.percentGenus = percentGenus;
		this.fractionGenus = percentGenus / 100f;
	}

	@Computed
	public void setFractionGenus(float fractionGenus) {
		this.fractionGenus = fractionGenus;
	}

	public Sp64DistributionSet getSp64DistributionSet() {
		return sp64DistributionSet;
	}

	public void setSp64DistributionSet(Sp64DistributionSet sp64DistributionSet) {
		this.sp64DistributionSet = sp64DistributionSet;
	}

	/**
	 * Construct the species sp64DistributionSet from the given list of species percentages. The maxIndex of the Set is
	 * taken to be the size of the map, and the index of each sp64Distribution is set to be its position in the list
	 * ordered by decreasing percentage.
	 *
	 * @param speciesPercentages the source of the construction
	 */
	public void setSpeciesPercentages(Map<String, Float> speciesPercentages) {

		// build a list of Sp64Distributions, all with index 0. The indicies will be assigned below.

		List<Sp64Distribution> unindexedSp64Distributions = new ArrayList<Sp64Distribution>();
		for (Map.Entry<String, Float> e : speciesPercentages.entrySet()) {
			unindexedSp64Distributions.add(new Sp64Distribution(0, e.getKey(), e.getValue()));
		}

		// sort the unindexed distributions in order of decreasing percentage
		unindexedSp64Distributions = unindexedSp64Distributions.stream()
				.sorted((o1, o2) -> o2.getPercentage().compareTo(o1.getPercentage())).toList();

		// and assign them an index, starting with one.
		int index = 1;
		List<Sp64Distribution> sp64Distributions = new ArrayList<Sp64Distribution>();
		for (Sp64Distribution uiSp64Distribution : unindexedSp64Distributions) {
			sp64Distributions.add(
					new Sp64Distribution(
							index++, uiSp64Distribution.getGenusAlias(), uiSp64Distribution.getPercentage()
					)
			);
		}

		this.sp64DistributionSet = new Sp64DistributionSet(sp64Distributions.size(), sp64Distributions);
	}

	public String getGenus() {
		return genus;
	}

	public int getGenusIndex() {
		return genusIndex;
	}

	public Optional<I> getSite() {
		return site;
	}

	public abstract static class Builder<T extends BaseVdypSpecies<I>, I extends BaseVdypSite, IB extends BaseVdypSite.Builder<I>>
			extends ModelClassBuilder<T> {
		protected Optional<PolygonIdentifier> polygonIdentifier = Optional.empty();
		protected Optional<LayerType> layerType = Optional.empty();
		protected Optional<String> genus = Optional.empty();
		protected Optional<Integer> genusIndex = Optional.empty();
		protected Optional<Float> percentGenus = Optional.empty();
		protected Optional<Float> fractionGenus = Optional.empty();
		protected List<Sp64Distribution> sp64DistributionList = new ArrayList<>();
		protected Optional<Consumer<IB>> siteBuilder = Optional.empty();
		protected Optional<I> site = Optional.empty();

		public Builder<T, I, IB> polygonIdentifier(PolygonIdentifier polygonIdentifier) {
			this.polygonIdentifier = Optional.of(polygonIdentifier);
			return this;
		}

		public Builder<T, I, IB> polygonIdentifier(String polygonIdentifier) {
			this.polygonIdentifier = Optional.of(PolygonIdentifier.split(polygonIdentifier));
			return this;
		}

		public Builder<T, I, IB> polygonIdentifier(String base, int year) {
			this.polygonIdentifier = Optional.of(new PolygonIdentifier(base, year));
			return this;
		}

		public Builder<T, I, IB> layerType(LayerType layer) {
			this.layerType = Optional.of(layer);
			return this;
		}

		/**
		 * Set both the genus, and at the same time calculates genusIndex from the given controlMap.
		 *
		 * @param genus      the species genus
		 * @param controlMap the control map defining the configuration
		 * @return this builder
		 */
		public Builder<T, I, IB> genus(String genus, Map<String, Object> controlMap) {
			this.genus = Optional.of(genus);
			this.genusIndex = Optional.of(GenusDefinitionParser.getIndex(genus, controlMap));
			return this;
		}

		/**
		 * Set both the genus and its index. It is the responsibility of the caller to ensure that the index is correct
		 * for the given genus. Use of this method is appropriate only when logic dictates the given genusIndex is
		 * correct or in those unit tests where correctness isn't critical.
		 *
		 * @param genus      the species genus
		 * @param genusIndex the index of the genus in the configuration (control map entry 10)
		 * @return this builder
		 */
		public Builder<T, I, IB> genus(String genus, int genusIndex) {
			this.genus = Optional.of(genus);
			this.genusIndex = Optional.of(genusIndex);
			return this;
		}

		public Builder<T, I, IB> percentGenus(float percentGenus) {
			this.percentGenus = Optional.of(percentGenus);
			return this;
		}

		protected Builder<T, I, IB> fractionGenus(float fractionGenus) {
			this.fractionGenus = Optional.of(fractionGenus);
			return this;
		}

		public Builder<T, I, IB> sp64DistributionList(List<Sp64Distribution> sp64DistributionList) {
			this.sp64DistributionList = sp64DistributionList;
			return this;
		}

		public Builder<T, I, IB> copy(T toCopy) {
			return this.adapt(toCopy);
		}

		public Builder<T, I, IB> addSite(Consumer<IB> config) {
			this.siteBuilder = Optional.of(config);
			this.site = Optional.empty();
			return this;
		}

		public Builder<T, I, IB> addSite(I site) {
			addSite(Optional.of(site));
			return this;
		}

		public Builder<T, I, IB> addSite(Optional<I> site) {
			this.site = site;
			this.siteBuilder = Optional.empty();
			return this;
		}

		public void addSp64Distribution(String sp64Alias, float f) {
			int index = sp64DistributionList.size() + 1;
			sp64DistributionList.add(new Sp64Distribution(index, sp64Alias, f));
		}

		@Override
		protected void check(Collection<String> errors) {
			requirePresent(polygonIdentifier, "polygonIdentifier", errors);
			requirePresent(layerType, "layerType", errors);
			requirePresent(genus, "genus", errors);
			requirePresent(genusIndex, "genusIndex", errors);
			requirePresent(percentGenus, "percentGenus", errors);
		}

		@Override
		protected void postProcess(T result) {
			super.postProcess(result);
			this.fractionGenus.ifPresent(result::setFractionGenus);
		}

		@Override
		protected void preProcess() {
			super.preProcess();
			site = siteBuilder.map(this::buildSite).or(() -> site);
		}

		@Override
		protected String getBuilderId() {
			return MessageFormat.format(
					"Species {0} {1} {2}", //
					polygonIdentifier.map(Object::toString).orElse("N/A"), //
					layerType.map(Object::toString).orElse("N/A"), //
					genus.map(Object::toString).orElse("N/A")//
			);
		}

		public Builder<T, I, IB> adapt(BaseVdypSpecies<?> toCopy) {
			polygonIdentifier(toCopy.getPolygonIdentifier());
			layerType(toCopy.getLayerType());
			genus(toCopy.getGenus(), toCopy.getGenusIndex());
			percentGenus(toCopy.getPercentGenus());

			fractionGenus(toCopy.getFractionGenus());

			sp64DistributionList(toCopy.getSp64DistributionSet().getSp64DistributionList());

			return this;
		}

		public <I2 extends BaseVdypSite> Builder<T, I, IB> adaptSite(I2 toCopy, BiConsumer<IB, I2> config) {
			this.addSite(builder -> {
				builder.adapt(toCopy);
				builder.polygonIdentifier = Optional.empty();
				builder.layerType = Optional.empty();
				config.accept(builder, toCopy);
			});
			return this;
		}

		public <S2 extends BaseVdypSpecies<I2>, I2 extends BaseVdypSite> Builder<T, I, IB>
				adaptSiteFrom(S2 specToCopy, BiConsumer<IB, I2> config) {
			specToCopy.getSite().ifPresent(toCopy -> this.adaptSite(toCopy, config));
			return this;
		}

		public Builder<T, I, IB> copySite(I toCopy, BiConsumer<IB, I> config) {
			this.addSite(builder -> {
				builder.copy(toCopy);
				builder.siteGenus = Optional.empty();
				builder.polygonIdentifier = Optional.empty();
				builder.layerType = Optional.empty();
				config.accept(builder, toCopy);
			});
			return this;
		}

		public Builder<T, I, IB> copySiteFrom(T specToCopy, BiConsumer<IB, I> config) {
			specToCopy.getSite().ifPresent(toCopy -> this.copySite(toCopy, config));
			return this;
		}

		protected abstract I buildSite(Consumer<IB> config);
	}

}
