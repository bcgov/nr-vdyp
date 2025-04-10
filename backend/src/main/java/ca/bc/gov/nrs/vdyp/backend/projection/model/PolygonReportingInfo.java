package ca.bc.gov.nrs.vdyp.backend.projection.model;

import java.util.HashMap;
import java.util.Map;

public class PolygonReportingInfo {

	private long featureId;
	private long polygonNumber;

	private String district;
	private String mapSheet;
	private String mapQuad;
	private String mapSubQuad;
	private String nonProdDescriptor;
	private Integer referenceYear;

	private Map<String /* layer id */, LayerReportingInfo> layers = new HashMap<>();

	private PolygonReportingInfo() {
	}

	public long getPolygonNumber() {
		return polygonNumber;
	}

	public String getDistrict() {
		return district;
	}

	public String getMapSheet() {
		return mapSheet;
	}

	public String getMapQuad() {
		return mapQuad;
	}

	public String getMapSubQuad() {
		return mapSubQuad;
	}

	public long getFeatureId() {
		return featureId;
	}

	public String getNonProdDescriptor() {
		return nonProdDescriptor;
	}

	public Integer getReferenceYear() {
		return referenceYear;
	}

	public Map<String, LayerReportingInfo> getLayerReportingInfos() {
		return layers;
	}

	public static class Builder {
		private PolygonReportingInfo polygonReportingInfo = new PolygonReportingInfo();

		public Builder polygonNumber(Long polygonNumber) {
			polygonReportingInfo.polygonNumber = polygonNumber;
			return this;
		}

		public Builder district(String district) {
			polygonReportingInfo.district = district;
			return this;
		}

		public Builder mapSheet(String mapSheet) {
			polygonReportingInfo.mapSheet = mapSheet;
			return this;
		}

		public Builder mapQuad(String mapQuad) {
			polygonReportingInfo.mapQuad = mapQuad;
			return this;
		}

		public Builder mapSubQuad(String mapSubQuad) {
			polygonReportingInfo.mapSubQuad = mapSubQuad;
			return this;
		}

		public Builder featureId(long featureId) {
			polygonReportingInfo.featureId = featureId;
			return this;
		}

		public Builder nonProdDescriptor(String nonProdDescriptor) {
			polygonReportingInfo.nonProdDescriptor = nonProdDescriptor;
			return this;
		}

		public Builder referenceYear(Integer referenceYear) {
			polygonReportingInfo.referenceYear = referenceYear;
			return this;
		}

		public Builder layers(Map<String, LayerReportingInfo> layers) {
			polygonReportingInfo.layers = layers;
			return this;
		}

		public PolygonReportingInfo build() {
			return polygonReportingInfo;
		}
	}
}
