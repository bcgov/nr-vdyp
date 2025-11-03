package ca.bc.gov.nrs.vdyp.model;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ca.bc.gov.nrs.vdyp.common.Computed;

public abstract class BaseVdypLayer<S extends BaseVdypSpecies<I>, I extends BaseVdypSite> implements Dumpable {

	private final PolygonIdentifier polygonIdentifier;
	private final LayerType layerType;
	private Optional<Integer> inventoryTypeGroup = Optional.empty();

	private LinkedHashMap<String, S> speciesBySp0 = new LinkedHashMap<>();
	private HashMap<Integer, S> speciesByIndex = new HashMap<>();

	protected BaseVdypLayer(
			PolygonIdentifier polygonIdentifier, LayerType layerType, Optional<Integer> inventoryTypeGroup
	) {
		super();
		this.polygonIdentifier = polygonIdentifier;
		this.layerType = layerType;

		this.inventoryTypeGroup = inventoryTypeGroup;
	}

	public PolygonIdentifier getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public LayerType getLayerType() {
		return layerType;
	}

	public LinkedHashMap<String, S> getSpecies() {
		return speciesBySp0;
	}

	public void setSpecies(Map<String, S> species) {
		setSpecies(species.values());
	}

	public void setSpecies(Collection<S> species) {
		this.speciesBySp0.clear();
		this.speciesByIndex.clear();
		species.forEach(spec -> {
			this.speciesBySp0.put(spec.getGenus(), spec);
			this.speciesByIndex.put(spec.getGenusIndex(), spec);
		});
	}

	public S getSpeciesBySp0(String sp0) {
		return speciesBySp0.get(sp0);
	}

	public S getSpeciesByIndex(int index) {
		return speciesByIndex.get(index);
	}

	public LinkedHashMap<String, I> getSites() {
		var result = new LinkedHashMap<String, I>(speciesBySp0.size());
		speciesBySp0.forEach((key, spec) -> spec.getSite().ifPresent(site -> result.put(key, site)));
		return result;
	}

	public List<I> getPriorityOrderedSites() {
		var result = new LinkedList<I>();
		Optional<I> primarySite = getPrimarySite();
		for (var siteEntry : speciesByIndex.values()) {
			I check = siteEntry.getSite().orElse(null);
			if (!Objects.equals(primarySite.orElse(null), check)) {
				result.addLast(check);
			}
		}
		primarySite.ifPresent(result::addFirst);
		return result;
	}

	public Optional<Integer> getInventoryTypeGroup() {
		return inventoryTypeGroup;
	}

	public void setInventoryTypeGroup(Optional<Integer> inventoryTypeGroup) {
		this.inventoryTypeGroup = inventoryTypeGroup;
	}

	public abstract Optional<String> getPrimaryGenus();

	@Computed
	public Optional<S> getPrimarySpeciesRecord() {
		return getPrimaryGenus().map(this.getSpecies()::get);
	}

	@Computed
	public Optional<I> getPrimarySite() {
		return getPrimaryGenus().map(this.getSites()::get);
	}

	@Override
	public String toString() {
		return polygonIdentifier.toStringCompact() + "-" + layerType;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BaseVdypLayer that) {
			// This is the "business key" of a layer.
			return this.polygonIdentifier.equals(that.polygonIdentifier) && this.layerType.equals(that.layerType);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return polygonIdentifier.hashCode() * 17 + layerType.hashCode();
	}

	public abstract static class Builder<T extends BaseVdypLayer<S, I>, S extends BaseVdypSpecies<I>, I extends BaseVdypSite, SB extends BaseVdypSpecies.Builder<S, I, IB>, IB extends BaseVdypSite.Builder<I>>
			extends ModelClassBuilder<T> {
		protected Optional<PolygonIdentifier> polygonIdentifier = Optional.empty();
		protected Optional<LayerType> layerType = Optional.empty();

		protected Optional<Integer> inventoryTypeGroup = Optional.empty();

		protected List<S> species = new LinkedList<>();
		protected List<Consumer<SB>> speciesBuilders = new LinkedList<>();
		protected List<Consumer<IB>> siteBuilders = new LinkedList<>();

		public Builder<T, S, I, SB, IB> polygonIdentifier(PolygonIdentifier polygonIdentifier) {
			this.polygonIdentifier = Optional.of(polygonIdentifier);
			return this;
		}

		public Builder<T, S, I, SB, IB> polygonIdentifier(String polygonIdentifier) {
			this.polygonIdentifier = Optional.of(PolygonIdentifier.split(polygonIdentifier));
			return this;
		}

		public Builder<T, S, I, SB, IB> polygonIdentifier(String base, int year) {
			this.polygonIdentifier = Optional.of(new PolygonIdentifier(base, year));
			return this;
		}

		public Builder<T, S, I, SB, IB> layerType(LayerType layer) {
			this.layerType = Optional.of(layer);
			return this;
		}

		public Optional<LayerType> getLayerType() {
			return layerType;
		}

		public Builder<T, S, I, SB, IB> addSpecies(Consumer<SB> config) {
			speciesBuilders.add(config);
			return this;
		}

		public Builder<T, S, I, SB, IB> addSpecies(Collection<S> species) {
			this.species.addAll(species);
			return this;
		}

		public Builder<T, S, I, SB, IB> addSpecies(S species) {
			this.species.add(species);
			return this;
		}

		public Builder<T, S, I, SB, IB> inventoryTypeGroup(int inventoryTypeGroup) {
			return this.inventoryTypeGroup(Optional.of(inventoryTypeGroup));
		}

		public Builder<T, S, I, SB, IB> inventoryTypeGroup(Optional<Integer> inventoryTypeGroup) {
			this.inventoryTypeGroup = inventoryTypeGroup;
			return this;
		}

		public Builder<T, S, I, SB, IB> adapt(BaseVdypLayer<?, ?> source) {
			polygonIdentifier(source.getPolygonIdentifier());
			layerType(source.getLayerType());
			inventoryTypeGroup(source.getInventoryTypeGroup());
			return this;
		}

		public Builder<T, S, I, SB, IB> copy(T source) {
			adapt(source);
			return this;
		}

		public <S2 extends BaseVdypSpecies<I2>, I2 extends BaseVdypSite> Builder<T, S, I, SB, IB>
				adaptSpecies(BaseVdypLayer<S2, ?> source, BiConsumer<SB, S2> config) {
			source.getSpecies().values().forEach(speciesToCopy -> {
				this.addSpecies(builder -> {
					builder.adapt(speciesToCopy);
					builder.polygonIdentifier = Optional.empty();
					builder.layerType = Optional.empty();
					config.accept(builder, speciesToCopy);
				});
			});
			return this;
		}

		public Builder<T, S, I, SB, IB> copySpecies(T source, BiConsumer<SB, S> config) {
			source.getSpecies().values().forEach(speciesToCopy -> {
				this.addSpecies(builder -> {
					builder.copy(speciesToCopy);
					builder.polygonIdentifier = Optional.empty();
					builder.layerType = Optional.empty();
					config.accept(builder, speciesToCopy);
				});
			});
			return this;
		}

		public List<S> getSpecies() {
			if (!speciesBuilders.isEmpty()) {
				throw new IllegalStateException("Tried to get species when there are unbuilt species builders");
			}
			return Collections.unmodifiableList(species);
		}

		@Override
		protected void check(Collection<String> errors) {
			requirePresent(polygonIdentifier, "polygonIdentifier", errors);
			requirePresent(layerType, "layerType", errors);
		}

		protected abstract S buildSpecies(Consumer<SB> config);

		/**
		 * Build any builders for child objects and store the results. This will clear the stored child builders.
		 */
		public void buildChildren() {
			speciesBuilders.stream().map(this::buildSpecies).collect(Collectors.toCollection(() -> species));
			speciesBuilders.clear();
		}

		@Override
		protected void postProcess(T result) {
			super.postProcess(result);
			buildChildren();
			result.setSpecies(species);
		}

		@Override
		protected String getBuilderId() {
			return MessageFormat.format(
					"Layer {0} {1}", //
					polygonIdentifier.map(Object::toString).orElse("N/A"), //
					layerType.map(Object::toString).orElse("N/A") //
			);
		}
	}

	@Override
	public void dumpState(Appendable output, int indent) throws IOException {

		Dumpable.writeHeader(output, indent, this.getClass(), this.toString());

		Dumpable.writeProperty(output, indent + 1, "inventoryTypeGroup", inventoryTypeGroup);

		Dumpable.writeChildren(output, indent + 1, "layers", this.speciesBySp0.values());

	}
}
