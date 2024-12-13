package ca.bc.gov.nrs.vdyp.backend.projection.input;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.ProjectionParameters;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.CfsEcoZone;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.NonVegetationType;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.OtherVegetationType;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.PolygonProcessingState;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionType;
import ca.bc.gov.nrs.vdyp.si32.bec.BecZone;
import ca.bc.gov.nrs.vdyp.si32.bec.BecZoneMethods;
import ca.bc.gov.nrs.vdyp.si32.enumerations.SpeciesRegion;

public class HcsvPolygonStream extends AbstractPolygonStream {

	private CsvStreamIterator<HcsvPolygonRecordBean> polygonRecordIterator;
	private CsvStreamIterator<HcsvLayerRecordBean> layerRecordIterator;
	private HcsvLayerRecordBean nextLayerRecord;
	private HcsvPolygonRecordBean nextPolygonRecord;

	private boolean streamsStarted;

	public HcsvPolygonStream(ProjectionState state, InputStream polygonStream, InputStream layersStream) {

		super(state);

		var polygonRecordIterator = HcsvPolygonRecordBean.createHcsvPolygonStream(polygonStream).iterator();
		polygonRecordIterator = new CsvStreamIterator<>(polygonRecordIterator);
		var layerRecordIterator = HcsvLayerRecordBean.createHcsvLayerStream(layersStream).iterator();
		layerRecordIterator = new CsvStreamIterator<>(layerRecordIterator);

		streamsStarted = false;
	}

	@Override
	public Polygon getNextPolygon() throws PolygonValidationException {

		if (streamsStarted == false) {

			if (polygonRecordIterator.hasNext()) {
				nextPolygonRecord = polygonRecordIterator.next();
			}

			if (nextPolygonRecord != null) {
				while (layerRecordIterator.hasNext()) {
					nextLayerRecord = layerRecordIterator.next().doPostBuildAdjustments();
					if (nextLayerRecord.getLayerFeatureId() >= nextPolygonRecord.getPolyFeatureId()) {
						break;
					}
				}
			}

			streamsStarted = true;
		}

		if (nextPolygonRecord == null) {
			throw new IllegalStateException("Attempt to read past end of polygon input stream");
		}

		var polygon = buildPolygon();

		if (polygonRecordIterator.hasNext()) {
			nextPolygonRecord = polygonRecordIterator.next();
		}

		return polygon;
	}

	@Override
	public boolean hasNextPolygon() {
		return nextPolygonRecord != null;
	}

	private Polygon buildPolygon() {

		ProjectionParameters projectionParameters = new ProjectionParameters.Builder() //
				.enableBack(state.getValidatedParams().containsOption(ExecutionOption.BACK_GROW_ENABLED)) //
				.enableForward(state.getValidatedParams().containsOption(ExecutionOption.FORWARD_GROW_ENABLED)) //
				.measurementYear(nextPolygonRecord.getReferenceYear()).standAgeAtMeasurementYear(0).build();

		var nonVegetationMap = buildNonVegetationMap();
		var otherVegetationMap = buildOtherVegetationMap();

		BecZone bec = BecZoneMethods.becZoneToIndex(nextPolygonRecord.getBecZoneCode());

		return new Polygon.Builder() //
				.becZone(bec.getText()) //
				.cfsEcoZone(CfsEcoZone.fromCode(nextPolygonRecord.getCfsEcoZoneCode())) //
				.coastal(bec.getSpeciesRegion() == SpeciesRegion.COAST) //
				.currentProcessingState(PolygonProcessingState.POLYGON_DEFINED) //
				.deadLayer(null) //
				.district(null /* not available in HCSV input */) //
				.doAllowProjection(false) //
				.doAllowProjectionOfType(initializeProjectionMap(true)) //
				.firstYearValidYields(initializeProjectionMap(-9999)) //
				.inventoryStandard(InventoryStandard.getFromCode(nextPolygonRecord.getInventoryStandardCode())) //
				.mapSheet(nextPolygonRecord.getMapId()) //
				.nonProductiveDescriptor(nextPolygonRecord.getNonProductiveDescriptorCode()).nonVegetationTypes(null) //
				.otherVegetationTypes(otherVegetationMap) //
				.percentStockable(nextPolygonRecord.getPercentStockable()) //
				.percentStockableDead(nextPolygonRecord.getPercentDead()) //
				.polygonNumber(nextPolygonRecord.getPolygonNumber()).projectionParameters(projectionParameters) //
				.referenceYear(nextPolygonRecord.getReferenceYear()).build();
	}

	private Map<OtherVegetationType, Integer> buildOtherVegetationMap() {
		Map<OtherVegetationType, Integer> map = new HashMap<>();

		map.put(OtherVegetationType.Bryoid, nextPolygonRecord.getBryoidCoverPercent());
		map.put(OtherVegetationType.Herb, nextPolygonRecord.getHerbCoverPercent());
		map.put(OtherVegetationType.Shrub, nextPolygonRecord.getShrubCrownClosure());

		return map;
	}

	private Map<NonVegetationType, Integer> buildNonVegetationMap() {
		Map<NonVegetationType, Integer> map = new HashMap<>();

		map.put(NonVegetationType.BurnedArea, 0);
		map.put(NonVegetationType.ExposedSoil, 0);
		map.put(NonVegetationType.Other, 0);
		map.put(NonVegetationType.Rock, 0);
		map.put(NonVegetationType.Snow, 0);
		map.put(NonVegetationType.Water, 0);

		return map;
	}

	private <T> Map<ProjectionType, T> initializeProjectionMap(T value) {
		Map<ProjectionType, T> map = new HashMap<>();
		for (ProjectionType t : ProjectionType.values()) {
			map.put(t, value);
		}
		return map;
	}
}
