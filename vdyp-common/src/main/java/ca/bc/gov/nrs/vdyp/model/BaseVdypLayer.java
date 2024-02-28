package ca.bc.gov.nrs.vdyp.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("java:S107")
public class BaseVdypLayer<S extends BaseVdypSpecies> {

	private final String polygonIdentifier;
	private final LayerType layer;
	private Optional<Float> ageTotal = Optional.empty(); // LVCOM3/AGETOTLV, L1COM3/AGETOTL1
	private Optional<Float> height = Optional.empty(); // LVCOM3/HDLV, L1COM3/HDL1
	private Optional<Float> yearsToBreastHeight = Optional.empty(); // LVCOM3/YTBHLV, L1COM3/YTBHL1
	private LinkedHashMap<String, S> species = new LinkedHashMap<>();
	private Optional<Float> siteIndex = Optional.empty();
	private Optional<Integer> siteCurveNumber = Optional.empty();
	private Optional<Integer> inventoryTypeGroup = Optional.empty();
	private Optional<String> siteGenus = Optional.empty(); // FIPL_1A/SITESP0_L1

	protected BaseVdypLayer(
			String polygonIdentifier, LayerType layer, Optional<Float> ageTotal, Optional<Float> height,
			Optional<Float> yearsToBreastHeight, Optional<Float> siteIndex, Optional<Integer> siteCurveNumber,
			Optional<Integer> inventoryTypeGroup, Optional<String> siteGenus
	) {
		super();
		this.polygonIdentifier = polygonIdentifier;
		this.layer = layer;

		this.ageTotal = ageTotal;
		this.height = height;
		this.yearsToBreastHeight = yearsToBreastHeight;
		this.siteIndex = siteIndex;
		this.siteCurveNumber = siteCurveNumber;
		this.inventoryTypeGroup = inventoryTypeGroup;
		this.siteGenus = siteGenus;
	}

	public String getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public LayerType getLayer() {
		return layer;
	}

	public Optional<Float> getAgeTotal() {
		return ageTotal;
	}

	public Optional<Float> getHeight() {
		return height;
	}

	public Optional<Float> getYearsToBreastHeight() {
		return yearsToBreastHeight;
	}

	public void setAgeTotal(Optional<Float> ageTotal) {
		this.ageTotal = ageTotal;
	}

	public void setHeight(Optional<Float> height) {
		this.height = height;
	}

	public void setYearsToBreastHeight(Optional<Float> yearsToBreastHeight) {
		this.yearsToBreastHeight = yearsToBreastHeight;
	}

	public Map<String, S> getSpecies() {
		return species;
	}

	public void setSpecies(Map<String, S> species) {
		this.species.clear();
		this.species.putAll(species);
	}

	public void setSpecies(Collection<S> species) {
		this.species.clear();
		species.forEach(spec -> this.species.put(spec.getGenus(), spec));
	}

	public Optional<Float> getSiteIndex() {
		return siteIndex;
	}

	public void setSiteIndex(Optional<Float> siteIndex) {
		this.siteIndex = siteIndex;
	}

	public Optional<Integer> getSiteCurveNumber() {
		return siteCurveNumber;
	}

	public void setSiteCurveNumber(Optional<Integer> siteCurveNumber) {
		this.siteCurveNumber = siteCurveNumber;
	}

	public Optional<Integer> getInventoryTypeGroup() {
		return inventoryTypeGroup;
	}

	public void setInventoryTypeGroup(Optional<Integer> inventoryTypeGroup) {
		this.inventoryTypeGroup = inventoryTypeGroup;
	}

	public Optional<String> getSiteGenus() {
		return siteGenus;
	}

	public void setSiteGenus(Optional<String> siteGenus) {
		this.siteGenus = siteGenus;
	}

	protected abstract static class Builder<T extends BaseVdypLayer<S>, S extends BaseVdypSpecies>
			extends ModelClassBuilder<T> {
		protected Optional<String> polygonIdentifier = Optional.empty();
		protected Optional<LayerType> layer = Optional.empty();
		protected Optional<Float> ageTotal = Optional.empty();
		protected Optional<Float> height = Optional.empty();
		protected Optional<Float> yearsToBreastHeight = Optional.empty();

		protected Optional<Float> siteIndex = Optional.empty();
		protected Optional<Integer> siteCurveNumber = Optional.empty();
		protected Optional<Integer> inventoryTypeGroup = Optional.empty();
		protected Optional<String> siteGenus = Optional.empty();

		protected List<S> species = new LinkedList<>();

		public Builder<T, S> polygonIdentifier(String polygonIdentifier) {
			this.polygonIdentifier = Optional.of(polygonIdentifier);
			return this;
		}

		public Builder<T, S> layerType(LayerType layer) {
			this.layer = Optional.of(layer);
			return this;
		}

		public Builder<T, S> ageTotal(float ageTotal) {
			return this.ageTotal(Optional.of(ageTotal));
		}

		public Builder<T, S> ageTotal(Optional<Float> ageTotal) {
			this.ageTotal = ageTotal;
			return this;
		}

		public Builder<T, S> height(float height) {
			return this.height(Optional.of(height));
		}

		public Builder<T, S> height(Optional<Float> height) {
			this.height = height;
			return this;
		}

		public Builder<T, S> yearsToBreastHeight(float yearsToBreastHeight) {
			return this.yearsToBreastHeight(Optional.of(yearsToBreastHeight));
		}

		public Builder<T, S> yearsToBreastHeight(Optional<Float> yearsToBreastHeight) {
			this.yearsToBreastHeight = yearsToBreastHeight;
			return this;
		}

		public Builder<T, S> addSpecies(S spec) {
			this.species.add(spec);
			return this;
		}

		public Builder<T, S> addSpecies(Collection<S> spec) {
			this.species.addAll(spec);
			return this;
		}

		public Builder<T, S> siteIndex(float siteIndex) {
			return this.siteIndex(Optional.of(siteIndex));
		}

		public Builder<T, S> siteCurveNumber(int siteCurveNumber) {
			return this.siteCurveNumber(Optional.of(siteCurveNumber));
		}

		public Builder<T, S> inventoryTypeGroup(int inventoryTypeGroup) {
			return this.inventoryTypeGroup(Optional.of(inventoryTypeGroup));
		}

		public Builder<T, S> siteGenus(String siteGenus) {
			return this.siteGenus(Optional.of(siteGenus));
		}

		public Builder<T, S> siteIndex(Optional<Float> siteIndex) {
			this.siteIndex = siteIndex;
			return this;
		}

		public Builder<T, S> siteCurveNumber(Optional<Integer> siteCurveNumber) {
			this.siteCurveNumber = siteCurveNumber;
			return this;
		}

		public Builder<T, S> inventoryTypeGroup(Optional<Integer> inventoryTypeGroup) {
			this.inventoryTypeGroup = inventoryTypeGroup;
			return this;
		}

		public Builder<T, S> siteGenus(Optional<String> siteGenus) {
			this.siteGenus = siteGenus;
			return this;
		}

		public Builder<T, S> copy(BaseVdypLayer<?> toCopy) {
			polygonIdentifier(toCopy.getPolygonIdentifier());
			layerType(toCopy.getLayer());
			ageTotal(toCopy.getAgeTotal());
			yearsToBreastHeight(toCopy.getYearsToBreastHeight());
			height(toCopy.getHeight());
			siteIndex(toCopy.getSiteIndex());
			siteCurveNumber(toCopy.getSiteCurveNumber());
			inventoryTypeGroup(toCopy.getInventoryTypeGroup());
			siteGenus(toCopy.getSiteGenus());
			return this;
		}

		@Override
		protected void check(Collection<String> errors) {
			requirePresent(polygonIdentifier, "polygonIdentifier", errors);
			requirePresent(layer, "layer", errors);
		}

		@Override
		protected void postProcess(T result) {
			super.postProcess(result);

			// Add species
			for (S spec : species) {
				result.getSpecies().put(spec.getGenus(), spec);
			}
		}

	}

}
