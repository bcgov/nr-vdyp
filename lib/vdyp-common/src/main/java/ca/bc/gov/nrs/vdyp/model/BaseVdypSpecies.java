package ca.bc.gov.nrs.vdyp.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.application.InitializationIncompleteException;
import ca.bc.gov.nrs.vdyp.common.Computed;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.io.parse.coe.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.model.builders.ModelClassBuilder;
import ca.bc.gov.nrs.vdyp.model.builders.SpeciesGroupIdentifiedBuilder;

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

	private Optional<Float> percentGenus = Optional.empty(); // FIPS/PCTVOLV L1COM1/PCTL1

	// This is computed from percentGenus, but VDYP7 computes it in a way that might
	// lead to a slight difference so it's stored separately and can be modified.
	@Computed
	private Optional<Float> fractionGenus = Optional.empty(); // FRBASP0/FR

	private Sp64DistributionSet sp64DistributionSet;

	private Optional<I> site;

	protected BaseVdypSpecies(
			PolygonIdentifier polygonIdentifier, LayerType layerType, String genus, int genusIndex,
			Optional<Float> percentGenus, Sp64DistributionSet sp64DistributionSet, Optional<I> site
	) {
		this.polygonIdentifier = polygonIdentifier;
		this.layerType = layerType;
		this.genus = genus;
		percentGenus.ifPresent(p -> this.setPercentGenus(p));
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
		return percentGenus.orElseThrow(
				() -> new InitializationIncompleteException(MessageFormat.format("Species {0}, percentGenus", this))
		);
	}

	public void setPercentGenus(float value) {
		// Note - it is permitted to set percentGenus more than once.
		this.percentGenus = Optional.of(value);
		setFractionGenus(percentGenus.get() / 100f);
	}

	@Computed
	public float getFractionGenus() {
		return fractionGenus.orElseThrow(
				() -> new InitializationIncompleteException(MessageFormat.format("Species {0}, fractionGenus", this))
		);
	}

	@Computed
	public void setFractionGenus(float value) {
		// Note - it is permitted to set fractionGenus more than once.
		this.fractionGenus = Optional.of(value);
	}

	public Sp64DistributionSet getSp64DistributionSet() {
		return sp64DistributionSet;
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}-{1}-{2}", polygonIdentifier.toStringCompact(), layerType, genus);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BaseVdypSpecies that) {
			// This is the "business key" of a species.
			return this.polygonIdentifier.equals(that.polygonIdentifier) && this.layerType.equals(that.layerType)
					&& this.genus.equals(that.genus);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (polygonIdentifier.hashCode() * 17 + layerType.hashCode()) * 17 + genus.hashCode();
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
			extends ModelClassBuilder<T> implements SpeciesGroupIdentifiedBuilder {
		protected Optional<PolygonIdentifier> polygonIdentifier = Optional.empty();
		protected Optional<LayerType> layerType = Optional.empty();
		protected Optional<String> genus = Optional.empty();
		protected Optional<Integer> genusIndex = Optional.empty();
		protected Optional<Float> percentGenus = Optional.empty();
		protected Optional<Float> fractionGenus = Optional.empty();
		protected Sp64DistributionSet sp64DistributionSet = new Sp64DistributionSet();
		protected Optional<Consumer<IB>> siteBuilder = Optional.empty();
		protected Optional<I> site = Optional.empty();
		protected Optional<GenusDefinitionMap> speciesGroupMap = Optional.empty();

		@Override
		public void polygonIdentifier(PolygonIdentifier polygonIdentifier) {
			this.polygonIdentifier = Optional.of(polygonIdentifier);
		}

		@Override
		public void layerType(LayerType layer) {
			this.layerType = Optional.of(layer);
		}

		public void controlMap(Map<String, Object> controlMap) {
			this.speciesGroupMap = Optional.of(GenusDefinitionParser.getSpecies(controlMap));
		}

		@Override
		public void genus(String genus) {
			this.genus = Optional.of(genus);
		}

		public void genus(int index) {
			this.genusIndex = Optional.of(index);
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
			this.sp64DistributionSet = new Sp64DistributionSet(sp64DistributionList);
			return this;
		}

		public Builder<T, I, IB> sp64DistributionSet(Sp64DistributionSet sp64DistributionSet) {
			this.sp64DistributionSet = sp64DistributionSet;
			return this;
		}

		public Builder<T, I, IB> copy(T source) {
			return this.adapt(source);
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
			var newSp64DistributionList = new ArrayList<>(this.sp64DistributionSet.getSp64DistributionList());
			newSp64DistributionList.add(new Sp64Distribution(newSp64DistributionList.size() + 1, sp64Alias, f));
			sp64DistributionSet = new Sp64DistributionSet(newSp64DistributionList);
		}

		@Override
		protected void check(Collection<String> errors) {
			requirePresent(polygonIdentifier, "polygonIdentifier", errors);
			requirePresent(layerType, "layerType", errors);
			requirePresent(genus, "genus", errors);
			requirePresent(genusIndex, "genusIndex", errors);

			// percentGenus is not required on build because the Forward
			// input data format does not include it in the species data files.
			// requirePresent(percentGenus, "percentGenus", errors);
		}

		@Override
		protected void postProcess(T result) {
			super.postProcess(result);
			this.fractionGenus.ifPresent(result::setFractionGenus);
		}

		@Override
		protected void preProcess() {
			super.preProcess();

			// If we have a map between alias and index, fill in the one from the other if necessary.
			speciesGroupMap.ifPresent(map -> {
				GenusDefinition def = genus.map(map::getByAlias).or(() -> genusIndex.map(map::getByIndex)).get();

				genus = Optional.of(genus.orElse(def.getAlias()));
				genusIndex = Optional.of(genusIndex.orElse(def.getIndex()));

			});

			// Build the site if present
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

		public Builder<T, I, IB> adapt(BaseVdypSpecies<?> source) {
			polygonIdentifier(source.getPolygonIdentifier());
			layerType(source.getLayerType());
			genus(source.getGenus());
			genus(source.getGenusIndex());
			percentGenus(source.getPercentGenus());

			fractionGenus(source.getFractionGenus());

			sp64DistributionList(source.getSp64DistributionSet().getSp64DistributionList());

			return this;
		}

		public <I2 extends BaseVdypSite> Builder<T, I, IB> adaptSite(I2 source, BiConsumer<IB, I2> config) {
			this.addSite(builder -> {
				builder.adapt(source);
				builder.polygonIdentifier = Optional.empty();
				builder.layerType = Optional.empty();
				config.accept(builder, source);
			});
			return this;
		}

		public <S2 extends BaseVdypSpecies<I2>, I2 extends BaseVdypSite> Builder<T, I, IB>
				adaptSiteFrom(S2 specToCopy, BiConsumer<IB, I2> config) {
			specToCopy.getSite().ifPresent(source -> this.adaptSite(source, config));
			return this;
		}

		public Builder<T, I, IB> copySite(I source, BiConsumer<IB, I> config) {
			this.addSite(builder -> {
				builder.copy(source);
				builder.siteGenus = Optional.empty();
				builder.polygonIdentifier = Optional.empty();
				builder.layerType = Optional.empty();
				config.accept(builder, source);
			});
			return this;
		}

		public Builder<T, I, IB> copySiteFrom(T specToCopy, BiConsumer<IB, I> config) {
			specToCopy.getSite().ifPresent(source -> this.copySite(source, config));
			return this;
		}

		public Builder<T, I, IB> copySp64DistributionFrom(T specToCopy) {
			this.sp64DistributionSet(specToCopy.getSp64DistributionSet());
			return this;
		}

		protected abstract I buildSite(Consumer<IB> config);
	}

}
