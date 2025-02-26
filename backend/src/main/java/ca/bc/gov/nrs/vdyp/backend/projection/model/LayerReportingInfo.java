package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

public class LayerReportingInfo {

	private Integer sourceLayerID;
	private String layerID;
	private String rank;
	private String nonForestDesc;

	private ProjectionTypeCode processedAsVDYP7Layer;
	private Integer firstYearYieldsDisplayed;

	private boolean isDeadStemLayer;

	private Map<String, SpeciesReportingInfo> species = new HashMap<>();

	private LayerReportingInfo() {
	}

	public Integer getSourceLayerID() {
		return sourceLayerID;
	}

	public String getLayerID() {
		return layerID;
	}

	public String getRank() {
		return rank;
	}

	public String getNonForestDesc() {
		return nonForestDesc;
	}

	public ProjectionTypeCode getProcessedAsVDYP7Layer() {
		return processedAsVDYP7Layer;
	}

	public Integer getFirstYearYieldsDisplayed() {
		return firstYearYieldsDisplayed;
	}

	public boolean isDeadStemLayer() {
		return isDeadStemLayer;
	}

	public Map<String, SpeciesReportingInfo> getSpecies() {
		return species;
	}

	public static class Builder {

		private LayerReportingInfo layerReportingInfo = new LayerReportingInfo();

		public Builder sourceLayerID(Integer sourceLayerID) {
			layerReportingInfo.sourceLayerID = sourceLayerID;
			return this;
		}

		public Builder layerID(String layerID) {
			layerReportingInfo.layerID = layerID;
			return this;
		}

		public Builder rank(String rank) {
			layerReportingInfo.rank = rank;
			return this;
		}

		public Builder nonForestDesc(String nonForestDesc) {
			layerReportingInfo.nonForestDesc = nonForestDesc;
			return this;
		}

		public Builder processedAsVDYP7Layer(ProjectionTypeCode processedAsVDYP7Layer) {
			layerReportingInfo.processedAsVDYP7Layer = processedAsVDYP7Layer;
			return this;
		}

		public Builder firstYearYieldsDisplayed(Integer firstYearYieldsDisplayed) {
			layerReportingInfo.firstYearYieldsDisplayed = firstYearYieldsDisplayed;
			return this;
		}

		public Builder deadStemLayer(boolean deadStemLayer) {
			layerReportingInfo.isDeadStemLayer = deadStemLayer;
			return this;
		}

		public Builder species(Map<String, SpeciesReportingInfo> species) {
			layerReportingInfo.species = species;
			return this;
		}

		public LayerReportingInfo build() {
			return layerReportingInfo;
		}
	}
}
