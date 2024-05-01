package ca.bc.gov.nrs.vdyp.forward;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.forward.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.forward.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.GenusDefinition;
import ca.bc.gov.nrs.vdyp.model.GenusDistribution;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.SiteCurve;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

public class ForwardProcessingEngine {

	private static final Logger logger = LoggerFactory.getLogger(ForwardProcessor.class);

	private static final float MIN_BASAL_AREA = 0.001f;

	private final ForwardProcessingState fps;
	private final GenusDefinitionMap genusDefinitionMap;
	private final BecLookup becLookup;
	private final Map<String, SiteCurve> siteCurveMap;

	@SuppressWarnings("unchecked")
	public ForwardProcessingEngine(Map<String, Object> controlMap) {

		genusDefinitionMap = new GenusDefinitionMap((List<GenusDefinition>) controlMap.get(ControlKey.SP0_DEF.name()));
		becLookup = (BecLookup) controlMap.get(ControlKey.BEC_DEF.name());
		siteCurveMap = (Map<String, SiteCurve>) controlMap.get(ControlKey.SITE_CURVE_NUMBERS.name());

		fps = new ForwardProcessingState(genusDefinitionMap);
	}

	public GenusDefinitionMap getGenusDefinitionMap() {
		return genusDefinitionMap;
	}

	public BecLookup getBecLookup() {
		return becLookup;
	}

	public Map<String, SiteCurve> getSiteCurveMap() {
		return Collections.unmodifiableMap(siteCurveMap);
	}

	public void processPolygon(VdypPolygon polygon) throws ProcessingException {

		logger.info("Starting processing of polygon {}", polygon.getDescription());

		fps.setStartingState(polygon);

		// All of BANKCHK1 that we need
		validatePolygon(polygon);

		// SCINXSET
		completeSiteCurveMap(polygon.getBiogeoclimaticZone(), LayerType.PRIMARY, 0);

		fps.setActive(LayerType.PRIMARY, 0);
	}

	private void completeSiteCurveMap(BecDefinition becZone, LayerType layerType, int instanceNumber) {

		PolygonProcessingState pps = fps.getBank(layerType, instanceNumber);

		for (int i = 0; i < pps.getNSpecies(); i++) {

			if (pps.siteCurveNumber[i] == VdypEntity.MISSING_INTEGER_VALUE) {

				Optional<SiteIndexEquation> scIndex = Optional.empty();

				Optional<GenusDistribution> sp0Dist = pps.sp64Distribution[i].getSpeciesDistribution(0);

				if (sp0Dist.isPresent()) {
					if (siteCurveMap.size() > 0) {
						SiteCurve sc = siteCurveMap.get(sp0Dist.get().getGenus().getAlias());
						scIndex = Optional.of(sc.getValue(becZone.getRegion()));
					} else {
						scIndex = Optional.of(
								SiteTool.getSICurve(pps.speciesName[i], becZone.getRegion().equals(Region.COASTAL))
						);
					}
				}

				if (scIndex.isEmpty()) {
					if (siteCurveMap.size() > 0) {
						SiteCurve sc = siteCurveMap.get(pps.speciesName[i]);
						scIndex = Optional.of(sc.getValue(becZone.getRegion()));
					} else {
						scIndex = Optional.of(
								SiteTool.getSICurve(pps.speciesName[i], becZone.getRegion().equals(Region.COASTAL))
						);
					}
				}
			}
		}
	}

	private void validatePolygon(VdypPolygon polygon) throws ProcessingException {

		if (polygon.getDescription().getYear() < 1900) {

			throw new ProcessingException(
					MessageFormat.format(
							"Polygon {}'s year value {} is < 1900", 101, polygon.getDescription().getName(), polygon
									.getDescription().getYear()
					)
			);
		}

		// The following is extracted from BANKCHK1, simplified for the parameters METH_CHK = 4,
		// LayerI = 1, and INSTANCE = 1. So IR = 1, which is the first bank, numbered 0.

		// => all that is done is that species with basal area < MIN_BASAL_AREA are removed.

		PolygonProcessingState pps = fps.getBank(LayerType.PRIMARY, 0);

		pps.removeSpecies(i -> pps.basalArea[i][UtilizationClass.ALL.ordinal()] < MIN_BASAL_AREA);

		if (pps.getNSpecies() == 0) {
			throw new ProcessingException(
					MessageFormat.format(
							"Polygon {0} layer 0 has no species with basal area above {1}", polygon.getDescription()
									.getName(), MIN_BASAL_AREA
					)
			);
		}
	}
}
