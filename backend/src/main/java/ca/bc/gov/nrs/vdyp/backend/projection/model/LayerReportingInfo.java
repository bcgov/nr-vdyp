package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

public class LayerReportingInfo {

	private Layer layer;

	private Integer sourceLayerID;
	private Boolean isDeadStemLayer;

	private List<SpeciesReportingInfo> orderedSpecies;

	private LayerReportingInfo() {
	}

	public Integer getSourceLayerID() {
		return sourceLayerID;
	}

	public Layer getLayer() {
		return layer;
	}

	public String getLayerID() {
		return layer.getLayerId();
	}

	public String getRank() {
		return layer.getRankCode();
	}

	public String getNonForestDesc() {
		return layer.getNonForestDescriptor();
	}

	public ProjectionTypeCode getProcessedAsVDYP7Layer() {
		return layer.getVdyp7LayerCode();
	}

	public Boolean isDeadStemLayer() {
		return isDeadStemLayer;
	}

	/**
	 * Record the child {@link SpeciesReportingInfo} elements of this. We assert that this method has not been called
	 * before because as the code is written if it is called a second time it's a mistake.
	 *
	 * @param sris the child SpeciesReportingInfo elements. This list is sorted here by the default sort order.
	 */
	public void setSpeciesReportingInfos(List<SpeciesReportingInfo> sris) {
		Validate.isTrue(
				orderedSpecies == null, "LayerReportingInfo.setSpeciesReportingInfos: orderedSpecies must be null"
		);
		Collections.sort(sris);
		orderedSpecies = sris;
	}

	/**
	 * Return the sorted list of SpeciesReportingInfo children. It is asserted that this list has already been set.
	 *
	 * @return as described
	 */
	public List<SpeciesReportingInfo> getOrderedSpecies() {
		Validate.notNull(orderedSpecies, "LayerReportingInfo.getOrderedSpecies: orderedSpecies must be not null");
		return orderedSpecies;
	}

	public static class Builder {

		private LayerReportingInfo lri = new LayerReportingInfo();

		public Builder layer(Layer layer) {

			lri.layer = layer;

			if (layer.getVdyp7LayerCode() != null) {
				lri.isDeadStemLayer = layer.getVdyp7LayerCode() == ProjectionTypeCode.DEAD;
			}

			return this;
		}

		public Builder sourceLayerID(Integer sourceLayerID) {
			lri.sourceLayerID = sourceLayerID;
			return this;
		}

		public LayerReportingInfo build() {
			return lri;
		}
	}
}
