package ca.bc.gov.nrs.vdyp.backend.projection;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.YieldTableGenerationException;
import ca.bc.gov.nrs.vdyp.backend.io.write.FipStartOutputWriter;
import ca.bc.gov.nrs.vdyp.backend.io.write.VdypGrowToYearFileWriter;
import ca.bc.gov.nrs.vdyp.backend.io.write.VriStartOutputWriter;
import ca.bc.gov.nrs.vdyp.backend.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.backend.projection.model.PolygonMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.model.ProjectionParameters;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Vdyp7Constants;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProcessingModeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.backend.utils.ProjectionUtils;
import ca.bc.gov.nrs.vdyp.backend.utils.Utils;
import ca.bc.gov.nrs.vdyp.common.Reference;
import ca.bc.gov.nrs.vdyp.exceptions.BecMissingException;
import ca.bc.gov.nrs.vdyp.exceptions.FailedToGrowYoungStandException;
import ca.bc.gov.nrs.vdyp.exceptions.PreprocessEstimatedBaseAreaLowException;
import ca.bc.gov.nrs.vdyp.exceptions.QuadraticMeanDiameterLowException;
import ca.bc.gov.nrs.vdyp.exceptions.ResultBaseAreaLowException;
import ca.bc.gov.nrs.vdyp.exceptions.StandProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.TotalAgeLowException;
import ca.bc.gov.nrs.vdyp.exceptions.UnsupportedModeException;
import ca.bc.gov.nrs.vdyp.math.VdypMath;

/**
 * This class is the internal representation of a Polygon to be projected.
 */
public class PolygonProjectionRunner {

	private static Logger logger = LoggerFactory.getLogger(PolygonProjectionRunner.class);

	private static final String EXECUTION_FOLDER_TEMPLATE_ZIP_FILE_NAME = "ExecutionFolderTemplate.zip";

	private Polygon polygon;
	private ProjectionContext context;
	private ComponentRunner componentRunner;

	private PolygonProjectionState state;
	private ProjectionParameters projectionParameters;

	/**
	 * Create a runner for the given {@link Polygon}, in the given {@link ProjectionContext}, using the given
	 * {@link RealComponentRunner}.
	 *
	 * @param polygon         the polygon to project
	 * @param context         the context in which the projection is to occur
	 * @param componentRunner the component runner to use
	 */
	private PolygonProjectionRunner(Polygon polygon, ProjectionContext context, ComponentRunner componentRunner) {

		this.polygon = polygon;
		this.context = context;
		this.componentRunner = componentRunner;

		this.projectionParameters = new ProjectionParameters.Builder() //
				.enableBack(context.getParams().containsOption(ExecutionOption.BACK_GROW_ENABLED)) //
				.enableForward(context.getParams().containsOption(ExecutionOption.FORWARD_GROW_ENABLED)) //
				.measurementYear(polygon.getReferenceYear()).standAgeAtMeasurementYear(0).build();

		this.state = new PolygonProjectionState();
	}

	/**
	 * Create a runner for the given {@link Polygon}, in the given {@link ProjectionContext}, using the given
	 * {@link RealComponentRunner}.
	 *
	 * @param polygon         the polygon to project
	 * @param context         the context in which the projection is to occur
	 * @param componentRunner the component runner to use
	 */
	public static PolygonProjectionRunner
			of(Polygon polygon, ProjectionContext context, ComponentRunner componentRunner) {

		return new PolygonProjectionRunner(polygon, context, componentRunner);
	}

	/**
	 * Run the projection
	 *
	 * @throws PolygonExecutionException     if there's an exception caused by the input data during the projection
	 * @throws PolygonExecutionException     if there's an exception caused by the software during the projection
	 * @throws YieldTableGenerationException if there's an exception during yield table generation
	 */
	void project() throws PolygonExecutionException, YieldTableGenerationException {

		// Begin implementation based on code starting at line 2088 (call to "V7Ext_GetPolygonInfo") in vdyp7console.c.
		// Note the funky error handling in this routine: "rtrnCode" is set to SUCCESS and is potentially set to
		// another value only when YldTable_GeneratePolygonYieldTables is called. "v7RtrnCode" is set to the result
		// of all the other routines and a negative result will sometimes inhibit the execution of a follow-on block
		// of code such as "V7Ext_ProjectStandByAge" and "YldTable_GeneratePolygonYieldTables" but not all. It's hard
		// to understand why things are done the way they are.

		buildPolygonProjectionExecutionStructure();

		performInitialProcessing();

		// VRI ADJUST is not supported at this time, so this code doesn't need to be written:
		// defineAdjustmentSeeds(state);

		determineAgeRange();

		performAdjustProcessing();

		performProjection();

		generateYieldTablesForPolygon();
	}

	private void buildPolygonProjectionExecutionStructure() throws PolygonExecutionException {

		try {
			Path executionFolderPath = Path.of(context.getExecutionFolder().toString(), polygon.toString());
			Path executionFolder = Files.createDirectory(executionFolderPath);

			for (ProjectionTypeCode projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

				if (polygon.getLayerByProjectionType(projectionType) != null) {
					ProjectionUtils.logger.debug("Populating execution folder for projectionType {}", projectionType);

					InputStream is = ProjectionUtils.class.getClassLoader()
							.getResourceAsStream(EXECUTION_FOLDER_TEMPLATE_ZIP_FILE_NAME);

					ProjectionUtils.prepareProjectionTypeFolder(is, executionFolder, projectionType.toString());
				}
			}

			state.setExecutionFolder(executionFolder);
		} catch (IOException e) {
			throw new PolygonExecutionException(e);
		}
	}

	/**
	 * <b>V7Ext_PerformInitialProcessing</b>
	 * <p>
	 * Runs the polygon through its initial processing making it capable of being projected to an arbitrary age or year.
	 * <p>
	 * <b>Remarks</b>
	 * <ul>
	 * <li>The polygon is processed through VRISTART or FIPSTART depending on the configuration of the polygon
	 * parameters.
	 * <li>Traps FIPSTART return codes of -4 in addition to -14.
	 * <li>Prevents initial processing if the Projections OK flag has been reset.
	 * </ul>
	 *
	 * @throws PolygonExecutionException to report a range of errors that may be encountered in this step.
	 */
	private void performInitialProcessing() throws PolygonExecutionException {

		logger.info("{}: performing initial processing", polygon);

		ProjectionTypeCode primaryProjectionType;
		try {
			primaryProjectionType = this.determineInitialProcessingModel(state);
		} catch (PolygonValidationException e) {
			throw new PolygonExecutionException(e);
		}

		GrowthModelCode initialGrowthModel = state.getGrowthModel(primaryProjectionType);
		ProcessingModeCode initialProcessingMode = state.getProcessingMode(primaryProjectionType);

		for (ProjectionTypeCode projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

			if (polygon.getLayerByProjectionType(projectionType) == null) {
				logger.debug(
						"{}: no layers defined for projection type {}; skipping initial processing for this type",
						polygon, projectionType
				);
				continue;
			}

			logger.info(
					"{}: layer exists for projection type {} (processing mode {}); {} processing will occur for this type",
					polygon, projectionType, initialProcessingMode, initialGrowthModel
			);

			switch (initialGrowthModel) {
			case FIP: {

				var doRetryUsingVriStart = false;

				createFipInputData(projectionType, initialProcessingMode, state);

				componentRunner.runFipStart(polygon, projectionType, state);

				var oFipResult = state.getProcessingResults(ProjectionStageCode.Initial, projectionType);

				if (!oFipResult.isPresent()) {
					logger.debug(
							"{}: performed FIP Model successfully for projection type {}", polygon, projectionType
					);
				} else {
					var fipResult = oFipResult.get();

					logger.debug(
							"{}: FIP Model failed for projection type {}{}", polygon, projectionType,
							fipResult.getMessage() != null ? ": " + fipResult.getMessage() : ""
					);

					if (fipResult instanceof StandProcessingException spe) {

						var layer = polygon.getLayerByProjectionType(projectionType);

						doRetryUsingVriStart = FipStartProcessingResult.doRetryUsingVriStart(spe);

						if (spe instanceof UnsupportedModeException) {
							polygon.addMessage(
									new PolygonMessage.Builder().polygon(polygon).layer(layer)
											.details(
													ReturnCode.SUCCESS, MessageSeverityCode.WARNING,
													PolygonMessageKind.LOW_SITE, spe.getErrorNumber().get()
											).build()
							);
							break;
						} else if (spe instanceof TotalAgeLowException) {
							polygon.addMessage(
									new PolygonMessage.Builder().polygon(polygon).layer(layer)
											.details(
													ReturnCode.SUCCESS, MessageSeverityCode.WARNING,
													PolygonMessageKind.BREAST_HEIGHT_AGE_TOO_YOUNG, "FipStart",
													spe.getErrorNumber().get()
											).build()
							);
							break;
						} else if (spe instanceof BecMissingException || spe instanceof ResultBaseAreaLowException) {
							polygon.addMessage(
									new PolygonMessage.Builder().polygon(polygon).layer(layer)
											.details(
													ReturnCode.SUCCESS, MessageSeverityCode.WARNING,
													PolygonMessageKind.BREAST_HEIGHT_AGE_TOO_YOUNG, "FIPSTART",
													spe.getErrorNumber().get()
											).build()
							);
							doRetryUsingVriStart = true;
							break;
						} else {
							polygon.addMessage(
									new PolygonMessage.Builder().polygon(polygon).layer(layer).details(
											ReturnCode.ERROR_CORELIBRARYERROR, MessageSeverityCode.ERROR,
											PolygonMessageKind.GENERIC_FIPSTART_ERROR, spe.getErrorNumber().get()
									).build()
							);
							break;
						}
					} else {
						polygon.addMessage(
								new PolygonMessage.Builder().polygon(polygon)
										.details(
												ReturnCode.ERROR_LAYERNOTPROCESSED, MessageSeverityCode.INFORMATION,
												PolygonMessageKind.BAD_STAND_DEFINITION
										).build()
						);
					}
				}

				if (doRetryUsingVriStart) {
					logger.debug("{}: falling through to VRI Model", polygon);
					state.modifyGrowthModel(projectionType, GrowthModelCode.VRI, ProcessingModeCode.VRI_VriYoung);
				} else {
					if (oFipResult.isPresent()) {
						polygon.disableProjectionsOfType(projectionType);
					}
					break;
				}
			}

			case VRI: {

				createVriInputData(projectionType, state);

				componentRunner.runVriStart(polygon, projectionType, state);

				var oVriResult = state.getProcessingResults(ProjectionStageCode.Initial, projectionType);
				if (!oVriResult.isPresent()) {
					logger.debug(
							"{}: performed VRI Model successfully for projection type {}", polygon, projectionType
					);
				} else {
					logger.debug(
							"{}: VRI Model failed for projection type {}: {}", polygon, projectionType,
							oVriResult.toString()
					);

					var vriResult = oVriResult.get();

					logger.debug(
							"{}: VRI Model failed for projection type {}{}", polygon, projectionType,
							vriResult.getMessage() != null ? ": " + vriResult.getMessage() : ""
					);

					polygon.disableProjectionsOfType(projectionType);

					if (vriResult instanceof StandProcessingException spe) {

						var errorNumber = spe.getErrorNumber();
						var layer = polygon.getLayerByProjectionType(projectionType);

						if (vriResult instanceof QuadraticMeanDiameterLowException) {

							polygon.addMessage(
									new PolygonMessage.Builder().layer(layer)
											.details(
													ReturnCode.ERROR_INVALIDSITEINFO, MessageSeverityCode.WARNING,
													PolygonMessageKind.LAYER_DETAILS_MISSING
											).build()
							);
						} else if (vriResult instanceof PreprocessEstimatedBaseAreaLowException) {
							polygon.addMessage(
									new PolygonMessage.Builder().layer(layer)
											.details(
													ReturnCode.SUCCESS, MessageSeverityCode.WARNING,
													PolygonMessageKind.PREDICATED_BASAL_AREA_TOO_SMALL, errorNumber
											).build()
							);
						} else if (vriResult instanceof FailedToGrowYoungStandException) {
							polygon.addMessage(
									new PolygonMessage.Builder().layer(layer).details(
											ReturnCode.SUCCESS, MessageSeverityCode.WARNING,
											PolygonMessageKind.NO_VIABLE_STAND_DESCRIPTION, "VRISTART", errorNumber
									).build()
							);
						} else {
							polygon.addMessage(
									new PolygonMessage.Builder().layer(layer)
											.details(
													ReturnCode.ERROR_CORELIBRARYERROR, MessageSeverityCode.ERROR,
													PolygonMessageKind.GENERIC_VRISTART_ERROR, errorNumber
											).build()
							);
						}
					}
				}
			}
			default: {
				var currentGrowthModel = state.getGrowthModel(projectionType);

				polygon.addMessage(
						new PolygonMessage.Builder().polygon(polygon)
								.details(
										ReturnCode.ERROR_INTERNALERROR, MessageSeverityCode.FATAL_ERROR,
										PolygonMessageKind.UNRECOGNIZED_GROWTH_MODEL, currentGrowthModel
								).build()
				);
			}
			}

		}

	}

	/**
	 * <b>V7Ext_InitialProcessingModeToBeUsed</b>
	 * <p>
	 * Determines the processing mode which to the stand will be initially subjected. The results are stored in the
	 * supplied <code>state</code> object.
	 * <p>
	 * <b>Remarks</b>
	 * <ul>
	 * <li>The processing mode is specified according to how the stand is defined. The mode is completely defined by the
	 * stand attributes and is not explicitly chosen by the caller.
	 * <li>The focus of the decision is the species group (sp0).
	 * <li>Changed FIPSTART decision logic to look at Non-Productive status.
	 * <li>Expanded the list of possible FIP non-productive codes to be any value rather than a subset.
	 * <li>Handle sitution where polygon consists solely of a dead layer. Prior to this, if only a dead layer, then the
	 * primary layer could not be found and an error is returned. Determine the processing model to which the stand will
	 * be initially subject.
	 * </ul>
	 *
	 * @param state the initial state of the projection of the polygon to this point.
	 * @return the projection type code
	 * @throws PolygonValidationException if the polygon definition contains errors
	 */
	private ProjectionTypeCode determineInitialProcessingModel(PolygonProjectionState state)
			throws PolygonValidationException {

		var rGrowthModel = new Reference<GrowthModelCode>();
		var rProcessingMode = new Reference<ProcessingModeCode>();
		var rPrimaryLayer = new Reference<Layer>();
		var rProjectionType = new Reference<ProjectionTypeCode>();

		polygon.calculateInitialProcessingModel(rGrowthModel, rProcessingMode, rPrimaryLayer, rProjectionType);

		Validate.isTrue(
				rGrowthModel.isPresent() && rProcessingMode.isPresent(),
				"PolygonProjectionRunner.determineInitialProcessingModel: at least one of rGrowthModel and rProcessingMode is not present"
		);

		if (!rPrimaryLayer.isPresent()) {
			throw new PolygonValidationException(
					new ValidationMessage(ValidationMessageKind.PRIMARY_LAYER_NOT_FOUND, polygon)
			);
		}

		var primaryLayer = rPrimaryLayer.get();
		var projectionType = rProjectionType.get();

		// Check if the stand is non-productive.
		if (polygon.getNonProductiveDescriptor() != null) {
			if (primaryLayer.getSp0sAsSupplied().size() > 0) {
				logger.debug(
						"{}: stand labelled with Non-Productive Code {}, but also contains a stand description.",
						polygon, polygon.getNonProductiveDescriptor()
				);
			} else {
				polygon.disableProjectionsOfType(primaryLayer.getAssignedProjectionType());

				polygon.addMessage(
						new PolygonMessage.Builder().layer(primaryLayer)
								.details(
										ReturnCode.ERROR_POLYGONNONPRODUCTIVE, MessageSeverityCode.ERROR,
										PolygonMessageKind.LAYER_NOT_COMPLETELY_DEFINED
								).build()
				);
			}
		}

		Stand leadingSp0 = primaryLayer.determineLeadingSp0(0 /* leading */);
		if (leadingSp0 == null) {
			polygon.disableProjectionsOfType(primaryLayer.getAssignedProjectionType());

			polygon.addMessage(
					new PolygonMessage.Builder().layer(primaryLayer)
							.details(
									ReturnCode.ERROR_SPECIESNOTFOUND, MessageSeverityCode.ERROR,
									PolygonMessageKind.NO_LEADING_SPECIES
							).build()
			);
		} else {
			logger.debug(
					"{} - {}: primary layer determined to be {} (percentage {})", polygon, projectionType, primaryLayer,
					leadingSp0.getSpeciesGroup().getSpeciesPercent()
			);
		}

		state.setGrowthModel(projectionType, rGrowthModel.get(), rProcessingMode.get());
		logger.trace(
				"Polygon {}: growth model {}; processing mode {}", this, rGrowthModel.get(), rProcessingMode.get()
		);

		return projectionType;
	}

	private void createFipInputData(
			ProjectionTypeCode projectionType, ProcessingModeCode processingMode, PolygonProjectionState state
	) throws PolygonExecutionException {

		Path stepExecutionFolder = Path.of(state.getExecutionFolder().toString(), projectionType.toString());

		FileOutputStream polygonOutputStream = null;
		FileOutputStream layersOutputStream = null;
		FileOutputStream speciesOutputStream = null;
		try {
			Path polygonFile = Path.of(stepExecutionFolder.toString(), "fip_p01.dat");
			polygonOutputStream = new FileOutputStream(polygonFile.toFile());

			Path layersFile = Path.of(stepExecutionFolder.toString(), "fip_l01.dat");
			layersOutputStream = new FileOutputStream(layersFile.toFile());

			Path speciesFile = Path.of(stepExecutionFolder.toString(), "fip_ls01.dat");
			speciesOutputStream = new FileOutputStream(speciesFile.toFile());

			try (
					var outputWriter = new FipStartOutputWriter(
							polygonOutputStream, layersOutputStream, speciesOutputStream
					)
			) {
				outputWriter.writePolygon(polygon, projectionType, processingMode, state);
				outputWriter.writePolygonLayer(polygon.getLayerByProjectionType(projectionType));
			}
		} catch (Exception e) {
			throw new PolygonExecutionException(
					MessageFormat.format(
							"{0}: encountered {1} while running creating FIP input data{2}", polygon,
							e.getClass().getName(), e.getMessage() != null ? "; reason: " + e.getMessage() : ""
					)
			);
		} finally {
			Utils.close(speciesOutputStream, "PolygonProjectionRunner.speciesOutputStream");
			Utils.close(layersOutputStream, "PolygonProjectionRunner.layersOutputStream");
			Utils.close(polygonOutputStream, "PolygonProjectionRunner.polygonOutputStream");
		}
	}

	private void createVriInputData(ProjectionTypeCode projectionTypeCode, PolygonProjectionState state)
			throws PolygonExecutionException {

		Path stepExecutionFolder = Path.of(state.getExecutionFolder().toString(), projectionTypeCode.toString());

		FileOutputStream polygonOutputStream = null;
		FileOutputStream layersOutputStream = null;
		FileOutputStream speciesOutputStream = null;
		FileOutputStream siteIndexOutputStream = null;

		try {
			Path polygonFile = Path.of(stepExecutionFolder.toString(), "virnp01.dat");
			polygonOutputStream = new FileOutputStream(polygonFile.toFile());

			Path layersFile = Path.of(stepExecutionFolder.toString(), "vrinl01.dat");
			layersOutputStream = new FileOutputStream(layersFile.toFile());

			Path speciesFile = Path.of(stepExecutionFolder.toString(), "vrinsp01.dat");
			speciesOutputStream = new FileOutputStream(speciesFile.toFile());

			Path siteIndexFile = Path.of(stepExecutionFolder.toString(), "vrinsi01.dat");
			siteIndexOutputStream = new FileOutputStream(siteIndexFile.toFile());

			try (
					var outputWriter = new VriStartOutputWriter(
							polygonOutputStream, layersOutputStream, speciesOutputStream, siteIndexOutputStream
					)
			) {
				outputWriter.writePolygon(polygon, projectionTypeCode, state);
				outputWriter.writePolygonLayer(polygon.getLayerByProjectionType(projectionTypeCode));
			}
		} catch (Exception e) {
			throw new PolygonExecutionException(
					MessageFormat.format(
							"{0}: encountered {1} while running creating VRI input data{2}", polygon,
							e.getClass().getName(), e.getMessage() != null ? "; reason: " + e.getMessage() : ""
					), e
			);
		} finally {
			Utils.close(siteIndexOutputStream, "PolygonProjectionRunner.siteIndexOutputStream");
			Utils.close(speciesOutputStream, "PolygonProjectionRunner.speciesOutputStream");
			Utils.close(layersOutputStream, "PolygonProjectionRunner.layersOutputStream");
			Utils.close(polygonOutputStream, "PolygonProjectionRunner.polygonOutputStream");
		}
	}

	private void performAdjustProcessing() throws PolygonExecutionException {

		logger.info("{}: performing ADJUST", polygon);

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

	private void performProjection() throws PolygonExecutionException {

		logger.info("{}: performing FORWARD/BACK", polygon);

		if (polygon.getReferenceYear() == null) {
			throw new IllegalStateException(MessageFormat.format("{0}: reference year is null", polygon));
		}

		var measurementYear = polygon.getReferenceYear().intValue();

		for (ProjectionTypeCode projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

			if (!polygon.doAllowProjectionOfType(projectionType)) {
				logger.warn("{}: projections of type {} for this polygon are not allowed", polygon, projectionType);

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

			Double primaryLayerAge = null;

			if (polygon.getPrimaryLayer() != null) {
				primaryLayerAge = polygon.getPrimaryLayer().determineLayerAgeAtYear(polygon.getReferenceYear());
			}

			switch (projectionType) {
			case PRIMARY:
				// do nothing - state contains the correct start and end ages already.
				break;
			case DEAD: {
				if (layer.getAgeAtDeath() != null) {
					state.updateProjectionRange(projectionType, layer.getAgeAtDeath(), layer.getAgeAtDeath());
				} else {
					var startAge = layer.determineLayerAgeAtYear(polygon.getReferenceYear());
					state.updateProjectionRange(projectionType, startAge, startAge);
				}
				break;
			}
			default: {
				if (primaryLayerAge == null) {
					throw new PolygonExecutionException(
							MessageFormat.format(
									"{0}: unable to project layer {1} since the age of the polygon's primary layer can't"
											+ " be calculated at year {2}{3}",
									polygon, layer, polygon.getReferenceYear(),
									polygon.getPrimaryLayer() == null ? ". Reason: the polygon has no primary layer"
											: ""
							)
					);
				}

				var referenceAge = layer.determineLayerAgeAtYear(polygon.getReferenceYear());
				var startAge = state.getStartAge(projectionType) + referenceAge - primaryLayerAge;
				var endAge = state.getEndAge(projectionType) + referenceAge - primaryLayerAge;
				state.updateProjectionRange(projectionType, startAge, endAge);
				break;
			}
			}

			// Determine the stand age, based on the leading site Sp0.
			var leadingSp0 = layer.determineLeadingSp0(0);
			if (leadingSp0 == null) {
				throw new PolygonExecutionException(
						MessageFormat.format(
								"{0}: unable to project since layer {1} has no leading Sp0 at year {2}", polygon, layer,
								polygon.getReferenceYear()
						)
				);
			}

			var doAllowForward = projectionParameters.isForwardEnabled();
			var doAllowBack = projectionParameters.isBackEnabled();
			if (state.getGrowthModel(projectionType) == GrowthModelCode.VRI
					&& state.getProcessingMode(projectionType) == ProcessingModeCode.VRI_VriYoung) {
				doAllowBack = false;
			}

			// Determine the range of years over which the projection(s) are to occur.

			var standAge = leadingSp0.getSpeciesGroup().getTotalAge();

			int startYear = adjustMeasurementYearAsNecessary(
					measurementYear, state.getStartAge(projectionType), standAge
			);
			int endYear = adjustMeasurementYearAsNecessary(measurementYear, state.getEndAge(projectionType), standAge);

			// Impose a limit of 400 years of projection, as is done in VDYP7.

			int yearsToGrowForward = VdypMath.clamp(endYear - measurementYear, 0, 400);
			int yearsToGrowBack = VdypMath.clamp(measurementYear - startYear, 0, 400);

			if (yearsToGrowBack == 0 && yearsToGrowForward == 0) {
				// vdyp7vdypmodel.for lines 278 - 282
				doAllowForward = true;
				yearsToGrowForward = 1;
			}

			int projectionStartYear = measurementYear - yearsToGrowBack;
			int firstRequestedYear = startYear;

			context.recordProjectionDetails(polygon, projectionType, projectionStartYear, firstRequestedYear);

			Path executionFolder = Path.of(state.getExecutionFolder().toString(), projectionType.toString());

			if (doAllowForward) {
				if (yearsToGrowForward > 0) {

					// TODO: there is logic in VDYP7Console that will try the projection a second time
					// if the first try failed AND adjustments were supplied that time - the second time,
					// no adjustments are supplied. Since we aren't using ADJUST at this time, there's no
					// need to implement this logic.

					generateYearToGrowFile(executionFolder, measurementYear, yearsToGrowForward);

					componentRunner.runForward(polygon, projectionType, state);

					logger.debug(
							"{}: performed Forward; result: {}", layer,
							state.getProcessingResults(ProjectionStageCode.Forward, projectionType)
									.map(e -> e.getMessage() != null ? e.getMessage() : e.getClass().getName())
									.orElse("success")
					);
				} else {
					logger.info("{}: forward projection skipped because yearsToGrowForward is 0", layer);
				}
			} else {
				logger.info("{}: forward projection required but disabled", layer);
			}

			if (doAllowBack) {
				if (yearsToGrowBack > 0) {

					// BACK read the backwards growth target value from entry 101 in the
					// control file. Adjust the control file to contain this value.
					rewriteTargetYearToBackControlFile(context.getExecutionFolder(), yearsToGrowBack, projectionType);

					componentRunner.runBack(polygon, projectionType, state);

					logger.debug(
							"{}: performed Back; result: {}", layer,
							state.getProcessingResults(ProjectionStageCode.Forward, projectionType)
									.map(e -> e.getMessage() != null ? e.getMessage() : e.getClass().getName())
									.orElse("success")
					);
				} else {
					logger.info("{}: backwards projection skipped because yearsToGrowBack is 0", layer);
				}
			} else {
				logger.info("{}: backwards projection required but disabled", layer);
			}
		}
	}

	private void determineAgeRange() {

		Integer measurementYear = polygon.getMeasurementYear();
		Double standAgeAtMeasurementYear = polygon.determineStandAgeAtYear(measurementYear);
		if (standAgeAtMeasurementYear != null) {
			standAgeAtMeasurementYear = Double.valueOf(Math.round(standAgeAtMeasurementYear));
		}

		logger.debug(
				"{}: determined age is {} at measurement year {}", polygon, standAgeAtMeasurementYear, measurementYear
		);

		// Determine the year range of the projection.

		Double startAge = calculateStartAge();

		Double endAge = calculateEndAge();

		// We need to make an allowance for the reference year and the current year. We need to
		// check if they extend the range of years to be projected.

		if (context.getParams().getSelectedExecutionOptions()
				.contains(ExecutionOption.DO_FORCE_REFERENCE_YEAR_INCLUSION_IN_YIELD_TABLES)) {

			if (startAge == null || standAgeAtMeasurementYear == null || standAgeAtMeasurementYear < startAge) {
				startAge = standAgeAtMeasurementYear;
			}

			if (endAge == null || standAgeAtMeasurementYear == null || standAgeAtMeasurementYear > endAge) {
				endAge = standAgeAtMeasurementYear;
			}

			logger.debug("{}: start and end age after considering reference year: {}, {}", polygon, startAge, endAge);
		}

		if (context.getParams().getSelectedExecutionOptions()
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

		if (context.getParams().getYearForcedIntoYieldTable() != null) {

			Double standAgeAtGivenYear = polygon
					.determineStandAgeAtYear(context.getParams().getYearForcedIntoYieldTable());
			if (startAge == null || standAgeAtGivenYear < startAge) {
				startAge = standAgeAtGivenYear;
			}

			if (endAge == null || standAgeAtGivenYear > endAge) {
				endAge = standAgeAtGivenYear;
			}

			logger.debug("{}: start and end age after considering special year: {}, {}", polygon, startAge, endAge);
		}

		state.setProjectionRange(startAge, endAge);
	}

	/**
	 * Calculate the starting age over which the yield table is to be produced.
	 * <p>
	 * This will be the minimum of the supplied AgeStart value and the age of the layer at the given YearStart, if both
	 * are supplied. If only the latter has a value, it is used. Otherwise, AgeStart is used (even if null.)
	 *
	 * @return as described
	 * @throws PolygonValidationException
	 */
	private Double calculateStartAge() {

		Double calculatedAge = null;

		Double ageAtYear = null;
		if (context.getParams().getYearStart() != null) {
			Layer layer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
			if (layer != null) {
				ageAtYear = layer.determineLayerAgeAtYear(context.getParams().getYearStart());
			}
		}

		Integer suppliedAgeStart = context.getParams().getAgeStart();
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
	 * This will be the minimum of the supplied AgeEnd value and the age of the layer at the given YearEnd, if both are
	 * supplied. If only the latter has a value, it is used. Otherwise, AgeEnd is used (even if null.)
	 *
	 * @return as described
	 */
	private Double calculateEndAge() {

		Double calculatedAge = null;

		Double ageAtYearEnd = null;
		if (context.getParams().getYearEnd() != null) {
			Layer layer = polygon.findPrimaryLayerByProjectionType(ProjectionTypeCode.UNKNOWN);
			if (layer != null) {
				ageAtYearEnd = layer.determineLayerAgeAtYear(context.getParams().getYearEnd());
			}
		}

		Integer suppliedAgeEnd = context.getParams().getAgeEnd();
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

	private int adjustMeasurementYearAsNecessary(int year, double suppliedAge, double standAge) {

		var suppliedAgeStandAgeDiff = suppliedAge - standAge;
		var areFractionalAges = Math.round(suppliedAgeStandAgeDiff) != suppliedAgeStandAgeDiff;
		if (suppliedAgeStandAgeDiff >= 0) {
			year += (int) Math.round(areFractionalAges ? suppliedAgeStandAgeDiff - 0.5 : suppliedAgeStandAgeDiff);
		} else {
			year += (int) Math.round(areFractionalAges ? suppliedAgeStandAgeDiff + 0.5 : suppliedAgeStandAgeDiff);
		}

		return year;
	}

	private void generateYearToGrowFile(Path executionFolder, int measurementYear, int yearsToGrow)
			throws PolygonExecutionException {
		try {
			Path growToYearFile = Path.of(executionFolder.toString(), "vin_y1.dat");
			FileOutputStream growToYearOutputStream = new FileOutputStream(growToYearFile.toFile());
			try (var yearToGrowWriter = new VdypGrowToYearFileWriter(growToYearOutputStream)) {

				yearToGrowWriter.writePolygon(polygon, measurementYear + yearsToGrow);
			}
		} catch (IOException e) {
			throw new PolygonExecutionException(e);
		}
	}

	static void rewriteTargetYearToBackControlFile(
			Path executionFolder, int yearsToGrowBack, ProjectionTypeCode projectionType
	) throws PolygonExecutionException {

		Path controlFilePath = Path
				.of(executionFolder.toString(), projectionType.toString(), Vdyp7Constants.BACK_CONTROL_FILE_NAME);
		Path tempControlFilePath = Path.of(controlFilePath.toString() + ".tmp");

		try {
			var controlFileContents = Files.readString(controlFilePath);
			var newControlFileContents = controlFileContents.replace("%YR%", String.format("%4d", yearsToGrowBack));
			Files.write(tempControlFilePath, newControlFileContents.getBytes());
			Files.delete(controlFilePath);
			Files.move(tempControlFilePath, controlFilePath);
		} catch (IOException e) {
			throw new PolygonExecutionException(e);
		}
	}

	private void generateYieldTablesForPolygon() throws YieldTableGenerationException {

		logger.info("{}: performing Yield Table generation", polygon);

		componentRunner.generateYieldTables(context, polygon, state);
	}
}
