package ca.bc.gov.nrs.vdyp.fip.model;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.model.Layer;

public class FipLayerPrimary extends FipLayer {

	static final String SITE_CURVE_NUMBER = "SITE_CURVE_NUMBER"; // SCN
	static final String STOCKING_CLASS = "STOCKING_CLASS"; // STK

	// TODO Confirm if these should be required instead of optional if we know it's
	// a Primary layer.
	Optional<Integer> siteCurveNumber = Optional.empty(); // FIPL_1/SCN_L1

	Optional<String> stockingClass = Optional.empty(); // FIPL_1ST/STK_L1

	public FipLayerPrimary(String polygonIdentifier) {
		super(polygonIdentifier, Layer.PRIMARY);
	}

	public Optional<Integer> getSiteCurveNumber() {
		return siteCurveNumber;
	}

	public void setSiteCurveNumber(Optional<Integer> siteCurveNumber) {
		this.siteCurveNumber = siteCurveNumber;
	}

	public Optional<String> getStockingClass() {
		return stockingClass;
	}

	public void setStockingClass(Optional<String> stockingClass) {
		this.stockingClass = stockingClass;
	}

}
