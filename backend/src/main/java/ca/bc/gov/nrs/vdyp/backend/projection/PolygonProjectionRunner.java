package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionInternalExecutionException;
import ca.bc.gov.nrs.vdyp.backend.io.write.FipStartOutputWriter;
import ca.bc.gov.nrs.vdyp.backend.io.write.VdypGrowToYearFileWriter;
import ca.bc.gov.nrs.vdyp.backend.io.write.VriStartOutputWriter;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.ProjectionParameters;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProcessingModeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.utils.ProjectionUtils;
import ca.bc.gov.nrs.vdyp.common.Reference;
import ca.bc.gov.nrs.vdyp.math.VdypMath;

/**
 * This class is the internal representation of a Polygon to be projected.
 */
public class PolygonProjectionRunner {

	static Logger logger = LoggerFactory.getLogger(PolygonProjectionRunner.class);

	private Polygon polygon;
	private ProjectionContext context;
	private IComponentRunner componentRunner;

	private PolygonProjectionState state;
	private ProjectionParameters projectionParameters;

	/**
	 * Create a runner for the given {@link Polygon}, in the given {@link ProjectionContext}, using the given
	 * {@link ComponentRunner}.
	 *
	 * @param polygon         the polygon to project
	 * @param context         the context in which the projection is to occur
	 * @param componentRunner the component runner to use
	 */
	public PolygonProjectionRunner(Polygon polygon, ProjectionContext context, IComponentRunner componentRunner) {

		this.polygon = polygon;
		this.context = context;
		this.componentRunner = componentRunner;

		this.projectionParameters = new ProjectionParameters.Builder() //
				.enableBack(context.getValidatedParams().containsOption(ExecutionOption.BACK_GROW_ENABLED)) //
				.enableForward(context.getValidatedParams().containsOption(ExecutionOption.FORWARD_GROW_ENABLED)) //
				.measurementYear(polygon.getReferenceYear()).standAgeAtMeasurementYear(0).build();

		this.state = new PolygonProjectionState();
	}

	/**
	 * Run the projection
	 *
	 * @throws PolygonExecutionException            if there's an exception caused by the input data during the
	 *                                              projection
	 * @throws ProjectionInternalExecutionException if there's an exception caused by the software during the projection
	 */
	void project() throws PolygonExecutionException, ProjectionInternalExecutionException {

		// Begin implementation based on code starting at line 2088 (call to "V7Ext_GetPolygonInfo") in vdyp7console.c.
		// Note the funky error handling in this routine: "rtrnCode" is set to SUCCESS and is potentially set to
		// another value only when YldTable_GeneratePolygonYieldTables is called. "v7RtrnCode" is set to the result
		// of all the other routines and a negative result will sometimes inhibit the execution of a follow-on block
		// of code such as "V7Ext_ProjectStandByAge" and "YldTable_GeneratePolygonYieldTables" but not all. It's hard
		// to understand why things are done the way they are.

		buildProjectionExecutionStructure();

		performInitialProcessing();

		// VRI ADJUST is not supported at this time, so this code doesn't need to be written:
		// defineAdjustmentSeeds(state);

		determineAgeRange();

		performAdjustProcessing();

		performProjection();

		generateYieldTables(state);
	}

	private void performInitialProcessing() throws PolygonExecutionException {

		try {
			this.determineInitialProcessingModel(state);
		} catch (PolygonValidationException e) {
			// Convert any validation exceptions into execution exceptions at this point.
			throw new PolygonExecutionException(e);
		}

		for (ProjectionTypeCode projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

			if (polygon.getLayerByProjectionType(projectionType) == null) {
				logger.debug(
						"{}: no layers defined for projection type {}; skipping initial processing for this type",
						polygon, projectionType
				);
				continue;
			}

			logger.debug(
					"{}: layer exists for projection type {} (growth model {}; processing mode {}); initial processing will occur for this type",
					polygon, projectionType, state.getGrowthModel(projectionType),
					state.getProcessingMode(projectionType)
			);

			switch (state.getGrowthModel(projectionType)) {
			case FIP: {

				createFipInputData(projectionType, state);

				componentRunner.runFipStart(polygon, projectionType, state);
				logger.debug(
						"{}: performed FIP Model; FIPSTART return code: {}", polygon,
						state.getProcessingResultsCode(ProjectionStageCode.Initial, projectionType).getResultCode()
				);

				var key = new PolygonProjectionState.ModelReturnCodeKey(ProjectionStageCode.Initial, projectionType);
				if (state.getInitialModelReturnCode().get(key).equals(ComponentReturnCodes.FIP_RETRY_USING_VRI_START)) {
					logger.debug("{}: falling through to VRI Model", polygon);
					state.modifyGrowthModel(projectionType, GrowthModelCode.VRI, ProcessingModeCode.VRI_VriYoung);
				} else {
					break;
				}
			}

			case VRI: {

				createVriInputData(projectionType, state);

				componentRunner.runVriStart(polygon, projectionType, state);
				logger.debug(
						"{}: performed VRI Model; VRISTART return code: {}", polygon,
						state.getProcessingResultsCode(ProjectionStageCode.Initial, projectionType).getResultCode()
				);

				break;
			}

			default:
				logger.error("{}: unrecognized growth model {}", polygon, state.getGrowthModel(projectionType));

				context.addMessage(
						"Attempt to process an unrecognized growth model {0}", state.getGrowthModel(projectionType)
				);
			}
		}
	}

	private void createFipInputData(ProjectionTypeCode projectionType, PolygonProjectionState state)
			throws PolygonExecutionException {

		Path executionFolder = Path.of(state.getExecutionFolder().toString(), projectionType.toString());

		FileOutputStream polygonOutputStream = null;
		FileOutputStream layersOutputStream = null;
		FileOutputStream speciesOutputStream = null;
		try {
			Path polygonFile = Path.of(executionFolder.toString(), "fip_p01.dat");
			polygonOutputStream = new FileOutputStream(polygonFile.toFile());

			Path layersFile = Path.of(executionFolder.toString(), "fip_l01.dat");
			layersOutputStream = new FileOutputStream(layersFile.toFile());

			Path speciesFile = Path.of(executionFolder.toString(), "fip_ls01.dat");
			speciesOutputStream = new FileOutputStream(speciesFile.toFile());

			try (
					var outputWriter = new FipStartOutputWriter(
							polygonOutputStream, layersOutputStream, speciesOutputStream
					)
			) {
				outputWriter.writePolygon(polygon, projectionType, state);
				outputWriter.writePolygonLayer(polygon.getLayerByProjectionType(projectionType));
			}
		} catch (IOException e) {
			throw new PolygonExecutionException(
					MessageFormat.format("{0}: encountered exception while running createFipInputData", polygon), e
			);
		} finally {
			close(speciesOutputStream);
			close(layersOutputStream);
			close(polygonOutputStream);
		}
	}

	private void createVriInputData(ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {
		Path executionFolder = Path.of(state.getExecutionFolder().toString(), projectionTypeCode.toString());

		FileOutputStream polygonOutputStream = null;
		FileOutputStream layersOutputStream = null;
		FileOutputStream speciesOutputStream = null;
		FileOutputStream siteIndexOutputStream = null;

		try {
			Path polygonFile = Path.of(executionFolder.toString(), "virnp01.dat");
			polygonOutputStream = new FileOutputStream(polygonFile.toFile());

			Path layersFile = Path.of(executionFolder.toString(), "vrinl01.dat");
			layersOutputStream = new FileOutputStream(layersFile.toFile());

			Path speciesFile = Path.of(executionFolder.toString(), "vrinsp01.dat");
			speciesOutputStream = new FileOutputStream(speciesFile.toFile());

			Path siteIndexFile = Path.of(executionFolder.toString(), "vrinsi01.dat");
			siteIndexOutputStream = new FileOutputStream(siteIndexFile.toFile());

			try (
					var outputWriter = new VriStartOutputWriter(
							polygonOutputStream, layersOutputStream, speciesOutputStream, siteIndexOutputStream
					)
			) {
				outputWriter.writePolygon(polygon, projectionTypeCode, state);
				outputWriter.writePolygonLayer(polygon.getLayerByProjectionType(projectionTypeCode));
			}
		} catch (IOException e) {
			throw new PolygonExecutionException(
					MessageFormat.format("{0}: encountered exception while running createVriInputData", polygon), e
			);
		} finally {
			close(siteIndexOutputStream);
			close(speciesOutputStream);
			close(layersOutputStream);
			close(polygonOutputStream);
		}
	}
	
	private void close(OutputStream os) {
		if (os != null) {
			try {
				os.close();
			} catch (IOException e) {
				throw new RuntimeException("Failed to close a given output stream");
			}
		}
 	}

	private void performAdjustProcessing() throws PolygonExecutionException {

		for (ProjectionTypeCode projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

			if (polygon.getLayerByProjectionType(projectionType) == null) {
				continue;
			}

			logger.debug(
					"{}: layer exists for projection type {} (growth model {}; processing mode {}); ADJUST will occur for this type",
					polygon, projectionType, state.getGrowthModel(projectionType),
					state.getProcessingMode(projectionType)
			);

			componentRunner.runAdjust(polygon, projectionType, state);
		}
	}

	private void performProjection() throws PolygonExecutionException, ProjectionInternalExecutionException {

		var measurementYear = polygon.getReferenceYear();

		for (ProjectionTypeCode projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

			if (! polygon.doAllowProjectionOfType(projectionType)) {
				logger.debug("{}: projections of type {} for this polygon are not allowed", polygon, projectionType);

				continue;
			}

			var layer = polygon.getLayerByProjectionType(projectionType);
			if (layer == null) {
				// There is no layer for this projection type; go to next projection type.
				continue;
			}

			logger.debug(
					"{}: layer exists for projection type {} (growth model {}; processing mode {}); unless disabled, projection will occur for this type",
					polygon, projectionType, state.getGrowthModel(projectionType),
					state.getProcessingMode(projectionType)
			);

			var primaryLayerAge = polygon.getPrimaryLayer().determineLayerAgeAtYear(polygon.getReferenceYear());

			switch (projectionType) {
			case PRIMARY:
				// do nothing - state contains the correct start and end ages already.
				break;
			case DEAD: {
				if (layer.getAgeAtDeath() != null) {
					state.updateProjectionRange(layer.getAgeAtDeath(), layer.getAgeAtDeath());
				} else {
					var startAge = layer.determineLayerAgeAtYear(polygon.getReferenceYear());
					state.updateProjectionRange(startAge, startAge);
				}
				break;
			}
			default: {
				var referenceAge = layer.determineLayerAgeAtYear(polygon.getReferenceYear());
				var startAge = state.getStartAge() + referenceAge - primaryLayerAge;
				var endAge = state.getEndAge() + referenceAge - primaryLayerAge;
				state.updateProjectionRange(startAge, endAge);
				break;
			}
			}

			// Determine the stand age, based on the leading site Sp0.
			var standAge = layer.determineLeadingSp0(0).getSpeciesGroup().getTotalAge();

			int startYear = adjustMeasurementYearAsNecessary(measurementYear, state.getStartAge(), standAge);
			int endYear = adjustMeasurementYearAsNecessary(measurementYear, state.getEndAge(), standAge);

			var doAllowForward = projectionParameters.isForwardEnabled();
			var doAllowBack = projectionParameters.isBackEnabled();
			if (state.getGrowthModel(projectionType) == GrowthModelCode.VRI
					&& state.getProcessingMode(projectionType) == ProcessingModeCode.VRI_VriYoung) {
				doAllowBack = false;
			}

			// Impose a limit of 400 years of projection, as is done in VDYP7.

			Path executionFolder = Path.of(state.getExecutionFolder().toString(), projectionType.toString());

			int yearsToGrowForward = VdypMath.clamp(endYear - measurementYear, 0, 400);
			if (yearsToGrowForward > 0) {
				if (doAllowForward) {

					// TODO: there is logic in VDYP7Console that will try the projection a second time
					// if the first try failed AND adjustments were supplied that time - the second time,
					// no adjustments are supplied. Since we aren't using ADJUST at this time, there's no
					// need to implement this logic.

					try {
						Path growToYearFile = Path.of(executionFolder.toString(), "vin_y1.dat");
						FileOutputStream growToYearOutputStream = new FileOutputStream(growToYearFile.toFile());
						try (var yearToGrowWriter = new VdypGrowToYearFileWriter(growToYearOutputStream)) {

							yearToGrowWriter.writePolygon(polygon, measurementYear + yearsToGrowForward);
						}
					} catch (IOException e) {
						throw new ProjectionInternalExecutionException(e);
					}

					componentRunner.runForward(polygon, projectionType, state);

					logger.debug(
							"{}: performed Forward; return code: {}", polygon,
							state.getProcessingResultsCode(ProjectionStageCode.Forward, projectionType).getResultCode()
					);
				} else {
					logger.info("{}: forward projection required but disabled", polygon);
				}
			}

			int yearsToGrowBack = VdypMath.clamp(measurementYear - startYear, 0, 400);
			if (yearsToGrowBack > 0) {
				if (doAllowBack) {
					try {
						Path growToYearFile = Path.of(executionFolder.toString(), "vin_y1.dat");
						FileOutputStream growToYearOutputStream = new FileOutputStream(growToYearFile.toFile());
						try (var yearToGrowWriter = new VdypGrowToYearFileWriter(growToYearOutputStream)) {

							yearToGrowWriter.writePolygon(polygon, measurementYear + yearsToGrowBack);
						}
					} catch (IOException e) {
						throw new ProjectionInternalExecutionException(e);
					}

					componentRunner.runBack(polygon, projectionType, state);

					logger.debug(
							"{}: performed Back; return code: {}", polygon,
							state.getProcessingResultsCode(ProjectionStageCode.Back, projectionType).getResultCode()
					);
				} else {
					logger.info("{}: backwards projection required but disabled", polygon);

				}
			}
		}
	}

	private int adjustMeasurementYearAsNecessary(int measurementYear, double suppliedAge, double standAge) {

		var suppliedAgeStandAgeDiff = suppliedAge - standAge;
		var areFractionalAges = Math.round(suppliedAgeStandAgeDiff) == suppliedAgeStandAgeDiff;
		if (suppliedAgeStandAgeDiff >= 0) {
			measurementYear += (int) Math
					.round(areFractionalAges ? suppliedAgeStandAgeDiff - 0.5 : suppliedAgeStandAgeDiff);
		} else {
			measurementYear += (int) Math
					.round(areFractionalAges ? suppliedAgeStandAgeDiff + 0.5 : suppliedAgeStandAgeDiff);
		}
		
		return measurementYear;
	}
	
	/**
	 * Determine the processing model to which the stand will be initially subject.
	 *
	 * @return as described
	 * @throws PolygonValidationException if the polygon definition contains errors
	 */
	private void determineInitialProcessingModel(PolygonProjectionState state) throws PolygonValidationException {

		var rGrowthModel = new Reference<GrowthModelCode>();
		var rProcessingMode = new Reference<ProcessingModeCode>();
		var rPrimaryLayer = new Reference<Layer>();

		polygon.calculateInitialProcessingModel(rGrowthModel, rProcessingMode, rPrimaryLayer);

		if (!rPrimaryLayer.isPresent()) {
			throw new PolygonValidationException(
					new ValidationMessage(ValidationMessageKind.PRIMARY_LAYER_NOT_FOUND, this)
			);
		}

		Layer selectedPrimaryLayer = rPrimaryLayer.get();
		assert rGrowthModel.isPresent() && rProcessingMode.isPresent();

		for (ProjectionTypeCode pt : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

			// Iterate until we find a projection type with a primary layer; that will determine
			// the (initial) model for all projection types.

			// Check if the stand is non-productive.
			if (polygon.getNonProductiveDescriptor() != null) {
				if (rPrimaryLayer.get().getSp0sAsSupplied().size() > 0) {
					logger.debug(
							"{}: stand labelled with Non Productive Code {}, but also contains a stand description.",
							polygon, polygon.getNonProductiveDescriptor()
					);
				} else {
					polygon.disableProjectionsOfType(pt);

					context.addMessage(
							"Layer {0} is not completely, or is not consistently, defined", selectedPrimaryLayer
					);
				}
			}

			Stand leadingSp0 = selectedPrimaryLayer.determineLeadingSp0(0 /* leading */);
			if (leadingSp0 == null) {
				polygon.disableProjectionsOfType(pt);
				context.addMessage("Unable to locate a leading species for primary layer {}", selectedPrimaryLayer);
			} else {
				logger.debug(
						"{} - {}: primary layer determined to be {} (percentage {})", polygon, pt, selectedPrimaryLayer,
						leadingSp0.getSpeciesGroup().getSpeciesPercent()
				);
			}

			state.setGrowthModel(pt, rGrowthModel.get(), rProcessingMode.get());
			logger.trace(
					"Polygon {}: growth model {}; processing mode {}", this, rGrowthModel.get(), rProcessingMode.get()
			);
		}
	}

	private void generateYieldTables(PolygonProjectionState state) {
		// TODO Auto-generated method stub

	}

	private void determineAgeRange() throws PolygonExecutionException {

		try {
			Integer measurementYear = polygon.getMeasurementYear();
			double standAgeAtMeasurementYear = Math.round(polygon.determineStandAgeAtYear(measurementYear));

			logger.debug(
					"{}: determined age is {} at measurement year {}", polygon, standAgeAtMeasurementYear,
					measurementYear
			);

			// Determine the year range of the projection.

			Double startAge = calculateStartAge();

			Double endAge = calculateEndAge();

			// We need to make an allowance for the reference year and the current year. We need to
			// check if they extend the range of years to be projected.

			if (context.getValidatedParams().getSelectedExecutionOptions()
					.contains(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)) {

				if (startAge == null || standAgeAtMeasurementYear < startAge) {
					startAge = standAgeAtMeasurementYear;
				}

				if (endAge == null || standAgeAtMeasurementYear > endAge) {
					endAge = standAgeAtMeasurementYear;
				}

				logger.debug(
						"{}: start and end age after considering reference year: {}, {}", polygon, startAge, endAge
				);
			}

			if (context.getValidatedParams().getSelectedExecutionOptions()
					.contains(ExecutionOption.DO_FORCE_CURRENT_YEAR_INCLUSION_IN_YIELD_TABLES)) {

				Double standAgeAtCurrentYear = polygon.determineStandAgeAtYear(LocalDate.now().getYear());
				if (startAge == null || standAgeAtCurrentYear < startAge) {
					startAge = standAgeAtCurrentYear;
				}

				if (endAge == null || standAgeAtCurrentYear > endAge) {
					endAge = standAgeAtCurrentYear;
				}

				logger.debug("{}: start and end age after considering current year: {}, {}", polygon, startAge, endAge);
			}

			if (context.getValidatedParams().getForceYear() != null) {

				Double standAgeAtGivenYear = polygon
						.determineStandAgeAtYear(context.getValidatedParams().getForceYear());
				if (startAge == null || standAgeAtGivenYear < startAge) {
					startAge = standAgeAtGivenYear;
				}

				if (endAge == null || standAgeAtGivenYear > endAge) {
					endAge = standAgeAtGivenYear;
				}

				logger.debug("{}: start and end age after considering special year: {}, {}", polygon, startAge, endAge);
			}

			state.setProjectionRange(startAge, endAge);

		} catch (PolygonValidationException pve) {
			throw new PolygonExecutionException(pve);
		}
	}

	/**
	 * Calculate the starting age over which the yield table is to be produced.
	 * <p>
	 * This will be the minimum of the supplied AgeStart value and the age of
	 * the layer at the given YearStart, if both are supplied. If only the 
	 * latter has a value, it is used. Otherwise, AgeStart is used (even if
	 * null.)
	 * 
	 * @return as described
	 * @throws PolygonValidationException
	 */
	private Double calculateStartAge() throws PolygonValidationException {
		
		Double calculatedAge = null;
		
		Double ageAtYear = null;
		if (context.getValidatedParams().getYearStart() != null) {
			Layer layer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
			ageAtYear = layer.determineLayerAgeAtYear(context.getValidatedParams().getYearStart());
		}

		Integer suppliedAgeStart = context.getValidatedParams().getAgeStart();
		if (ageAtYear != null && suppliedAgeStart != null) {
			calculatedAge = Math.min(ageAtYear, suppliedAgeStart);
		} else if (ageAtYear != null) {
			calculatedAge = ageAtYear;
		} else if (suppliedAgeStart != null) {
			calculatedAge = Double.valueOf(suppliedAgeStart);
		}

		logger.debug("{}: starting age of yield table has been determined to be {}", polygon, calculatedAge);
		
		return calculatedAge;
	}
	
	/**
	 * Calculate the ending age over which the yield table is to be produced.
	 * <p>
	 * This will be the minimum of the supplied AgeEnd value and the age of
	 * the layer at the given YearEnd, if both are supplied. If only the 
	 * latter has a value, it is used. Otherwise, AgeEnd is used (even if
	 * null.)
	 * 
	 * @return as described
	 * @throws PolygonValidationException
	 */
	private Double calculateEndAge() throws PolygonValidationException {
		
		Double calculatedAge = null;
		
		Double ageAtYearEnd = null;
		if (context.getValidatedParams().getYearEnd() != null) {
			Layer layer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
			ageAtYearEnd = layer.determineLayerAgeAtYear(context.getValidatedParams().getYearEnd());
		}

		Integer suppliedAgeEnd = context.getValidatedParams().getAgeEnd();
		if (ageAtYearEnd != null && suppliedAgeEnd != null) {
			calculatedAge = Math.min(ageAtYearEnd, suppliedAgeEnd);
		} else if (ageAtYearEnd != null) {
			calculatedAge = ageAtYearEnd;
		} else if (suppliedAgeEnd != null) {
			calculatedAge = Double.valueOf(suppliedAgeEnd);
		}

		logger.debug("{}: ending age of yield table has been determined to be {}", polygon, calculatedAge);
		
		return calculatedAge;
	}
	
	private void buildProjectionExecutionStructure() throws ProjectionInternalExecutionException {

		URL rootUrl = ProjectionUtils.class.getClassLoader().getResource("ca/bc/gov/nrs/vdyp/template");
		try {
			String jobName = toString() + '-'
					+ LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(":", "-");

			Path rootFolder = Path.of(rootUrl.toURI());
			Path executionFolder = Files.createTempDirectory(rootFolder, jobName + '-');

			for (ProjectionTypeCode projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

				if (polygon.getLayerByProjectionType(projectionType) != null) {
					ProjectionUtils.logger.debug("Populating execution folder for projectionType {}", projectionType);
					ProjectionUtils.prepareProjectionTypeFolder(
							rootFolder, executionFolder, projectionType.toString(), "FIPSTART.CTR", "VRISTART.CTR",
							"VRIADJST.CTR", "VDYP.CTR", "VDYPBACK.CTR"
					);
				}
			}

			state.setExecutionFolder(executionFolder);
		} catch (IOException | URISyntaxException e) {
			throw new ProjectionInternalExecutionException(e);
		}
	}
}
