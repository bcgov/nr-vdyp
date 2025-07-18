package ca.bc.gov.nrs.vdyp.model;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.Computed;
import ca.bc.gov.nrs.vdyp.common.Utils;

public abstract class BaseVdypSite {

	private final PolygonIdentifier polygonIdentifier;
	private final LayerType layerType;
	private final String siteGenus; // FIPL_1A/SITESP0_L1, VRISIA/SITESP0
	private final Optional<Integer> siteCurveNumber; // VRISI/VR_SCN
	private final Optional<Float> siteIndex; // VRISI/VR_SI

	private final Optional<Float> ageTotal; // LVCOM3/AGETOTLV, L1COM3/AGETOTL1, VRISI/VR_TAGE
	private final Optional<Float> height; // LVCOM3/HDLV, L1COM3/HDL1, VRISI/VR_HD
	private final Optional<Float> yearsToBreastHeight; // LVCOM3/YTBHLV, L1COM3/YTBHL1, VRISI/VR_YTBH
	private final Optional<Float> yearsAtBreastHeight; // VRISI/VR_SCN

	protected BaseVdypSite(
			PolygonIdentifier polygonIdentifier, LayerType layerType, String siteGenus,
			Optional<Integer> siteCurveNumber, Optional<Float> siteIndex, Optional<Float> height,
			Optional<Float> ageTotal, Optional<Float> yearsToBreastHeight, Optional<Float> yearsAtBreastHeight
	) {
		super();
		this.polygonIdentifier = polygonIdentifier;
		this.layerType = layerType;
		this.siteGenus = siteGenus;
		this.siteCurveNumber = siteCurveNumber;
		this.siteIndex = siteIndex;
		this.height = height;
		this.ageTotal = ageTotal;
		this.yearsToBreastHeight = yearsToBreastHeight;
		this.yearsAtBreastHeight = yearsAtBreastHeight;
	}

	public PolygonIdentifier getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public LayerType getLayerType() {
		return layerType;
	}

	public String getSiteGenus() {
		return siteGenus;
	}

	public Optional<Integer> getSiteCurveNumber() {
		return siteCurveNumber;
	}

	public Optional<Float> getSiteIndex() {
		return siteIndex;
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

	/**
	 * Computed from ageTotal and yearsToBreastHeight.
	 */
	@Computed
	public Optional<Float> getComputedYearsAtBreastHeight(float offset) {
		return Utils.mapBoth(getYearsToBreastHeight(), getAgeTotal(), (ytbh, at) -> at - ytbh + offset);
	}

	/**
	 * Computed from ageTotal and yearsToBreastHeight. This may differ from yearsAtBreastHeight by up to 0.5 years;
	 */
	@Computed
	public Optional<Float> getComputedYearsAtBreastHeight() {
		return getComputedYearsAtBreastHeight(0f);
	}

	public Optional<Float> getYearsAtBreastHeight() {
		return yearsAtBreastHeight;
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}-{1}-{2}", polygonIdentifier.toStringCompact(), layerType, siteGenus);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BaseVdypSite that) {
			// This is the "business key" of a site.
			return this.polygonIdentifier.equals(that.polygonIdentifier) && this.layerType.equals(that.layerType)
					&& this.siteGenus.equals(that.siteGenus);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (polygonIdentifier.hashCode() * 17 + layerType.hashCode()) * 17 + siteGenus.hashCode();
	}

	public abstract static class Builder<T extends BaseVdypSite> extends ModelClassBuilder<T> {
		protected Optional<PolygonIdentifier> polygonIdentifier = Optional.empty();
		protected Optional<LayerType> layerType = Optional.empty();
		protected Optional<String> siteGenus = Optional.empty();

		protected Optional<Integer> siteCurveNumber = Optional.empty();
		protected Optional<Float> siteIndex = Optional.empty();

		protected Optional<Float> ageTotal = Optional.empty();
		protected Optional<Float> height = Optional.empty();
		protected Optional<Float> yearsToBreastHeight = Optional.empty();
		protected Optional<Float> yearsAtBreastHeight = Optional.empty();

		public Builder<T> polygonIdentifier(PolygonIdentifier polygonIdentifier) {
			this.polygonIdentifier = Optional.of(polygonIdentifier);
			return this;
		}

		public Builder<T> polygonIdentifier(String polygonIdentifier) {
			this.polygonIdentifier = Optional.of(PolygonIdentifier.split(polygonIdentifier));
			return this;
		}

		public Builder<T> polygonIdentifier(String base, int year) {
			this.polygonIdentifier = Optional.of(new PolygonIdentifier(base, year));
			return this;
		}

		public Builder<T> layerType(LayerType layerType) {
			this.layerType = Optional.of(layerType);
			return this;
		}

		public Builder<T> siteGenus(String siteGenus) {
			this.siteGenus = Optional.of(siteGenus);
			return this;
		}

		public Builder<T> siteIndex(float siteIndex) {
			return this.siteIndex(Optional.of(siteIndex));
		}

		public Builder<T> siteCurveNumber(int siteCurveNumber) {
			return this.siteCurveNumber(Optional.of(siteCurveNumber));
		}

		public Builder<T> siteIndex(Optional<Float> siteIndex) {
			this.siteIndex = siteIndex;
			return this;
		}

		public Builder<T> siteCurveNumber(Optional<Integer> siteCurveNumber) {
			this.siteCurveNumber = siteCurveNumber;
			return this;
		}

		public Builder<T> siteGenus(Optional<String> siteGenus) {
			this.siteGenus = siteGenus;
			return this;
		}

		public Builder<T> ageTotal(Optional<Float> ageTotal) {
			this.ageTotal = ageTotal;
			return this;
		}

		public Builder<T> height(Optional<Float> height) {
			this.height = height;
			return this;
		}

		public Builder<T> yearsToBreastHeight(Optional<Float> yearsToBreastHeight) {
			this.yearsToBreastHeight = yearsToBreastHeight;
			return this;
		}

		public Builder<T> yearsAtBreastHeight(Optional<Float> yearsAtBreastHeight) {
			this.yearsAtBreastHeight = yearsAtBreastHeight;
			return this;
		}

		/**
		 * Compute yearsAtBreastHeight from yearsAtBreastHeight and ageTotal
		 *
		 * @return
		 */
		public Builder<T> yearsAtBreastHeightAuto() {
			return yearsAtBreastHeightAuto(0.0f);
		}

		/**
		 * Compute yearsAtBreastHeight from yearsAtBreastHeight and ageTotal
		 *
		 * @return
		 */
		public Builder<T> yearsAtBreastHeightAuto(float offset) {
			this.yearsAtBreastHeight = Utils
					.mapBoth(ageTotal, yearsToBreastHeight, (total, ytbh) -> total - ytbh + offset);
			return this;
		}

		public Builder<T> ageTotal(float ageTotal) {
			return ageTotal(Optional.of(ageTotal));
		}

		public Builder<T> height(float height) {
			return height(Optional.of(height));
		}

		public Builder<T> yearsToBreastHeight(float yearsToBreastHeight) {
			return yearsToBreastHeight(Optional.of(yearsToBreastHeight));
		}

		public Builder<T> yearsAtBreastHeight(float yearsAtBreastHeight) {
			return yearsAtBreastHeight(Optional.of(yearsAtBreastHeight));
		}

		public Builder<T> adapt(BaseVdypSite source) {
			polygonIdentifier(source.getPolygonIdentifier());
			layerType(source.getLayerType());
			ageTotal(source.getAgeTotal());
			yearsToBreastHeight(source.getYearsToBreastHeight());
			yearsAtBreastHeight(source.getYearsAtBreastHeight());
			height(source.getHeight());
			siteIndex(source.getSiteIndex());
			siteCurveNumber(source.getSiteCurveNumber());
			siteGenus(source.getSiteGenus());
			return this;
		}

		public Builder<T> copy(T source) {
			return adapt(source);
		}

		@Override
		protected void check(Collection<String> errors) {
			requirePresent(polygonIdentifier, "polygonIdentifier", errors);
			requirePresent(layerType, "layerType", errors);
			requirePresent(siteGenus, "siteGenus", errors);
		}

		@Override
		protected String getBuilderId() {
			return MessageFormat.format(
					"Site {0} {1} {2}", //
					polygonIdentifier.map(Object::toString).orElse("N/A"), //
					layerType.map(Object::toString).orElse("N/A"), //
					siteGenus.map(Object::toString).orElse("N/A")//
			);
		}

	}

}
