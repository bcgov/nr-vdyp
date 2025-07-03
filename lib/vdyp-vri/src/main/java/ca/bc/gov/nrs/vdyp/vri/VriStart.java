package ca.bc.gov.nrs.vdyp.vri;

import static ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter.quadMeanDiameter;
import static ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter.treesPerHectare;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.pow;
import static java.lang.Math.max;

import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.ResultWithStatus;
import ca.bc.gov.nrs.vdyp.common.ResultWithStatus.BasicStatus;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common.ValueOrMarker;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationProcessingException;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.common_calculators.SiteIndex2Height;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CommonCalculatorException;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.exceptions.BaseAreaLowException;
import ca.bc.gov.nrs.vdyp.exceptions.BreastHeightAgeLowException;
import ca.bc.gov.nrs.vdyp.exceptions.CrownClosureLowException;
import ca.bc.gov.nrs.vdyp.exceptions.FailedToGrowYoungStandException;
import ca.bc.gov.nrs.vdyp.exceptions.FatalProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.HeightLowException;
import ca.bc.gov.nrs.vdyp.exceptions.LayerMissingException;
import ca.bc.gov.nrs.vdyp.exceptions.LayerMissingValuesRequiredForMode;
import ca.bc.gov.nrs.vdyp.exceptions.PreprocessEstimatedBaseAreaLowException;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.RuntimeProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.RuntimeStandProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.SiteIndexLowException;
import ca.bc.gov.nrs.vdyp.exceptions.StandProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.TotalAgeLowException;
import ca.bc.gov.nrs.vdyp.exceptions.TreesPerHectareLowException;
import ca.bc.gov.nrs.vdyp.exceptions.YearsToBreastHeightLowException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSite;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies.Builder;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.CompatibilityVariableMode;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.ModelClassBuilder;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VolumeComputeMode;
import ca.bc.gov.nrs.vdyp.vri.model.VriLayer;
import ca.bc.gov.nrs.vdyp.vri.model.VriPolygon;
import ca.bc.gov.nrs.vdyp.vri.model.VriSite;
import ca.bc.gov.nrs.vdyp.vri.model.VriSpecies;

public class VriStart extends VdypStartApplication<VriPolygon, VriLayer, VriSpecies, VriSite> implements Closeable {

	private static final String SPECIAL_PROCESSING_LOG_TEMPLATE = "Doing special processing for mode {}";
	static final float FRACTION_AVAILABLE_N = 0.85f; // PCTFLAND_N;

	static final Logger log = LoggerFactory.getLogger(VriStart.class);

	static final float EMPOC = 0.85f;

	static final float VETERAN_MIN_DQ = UtilizationClass.OVER225.lowBound;
	static final float VETERAN_MIN_HL = UtilizationClass.OVER225.lowBound; // Seems odd that that the min height is the
																			// same as the min diameter

	public static void main(final String... args) {

		try (var app = new VriStart();) {
			try {
				app.doMain(args);
			} catch (VdypApplicationInitializationException e) {
				System.exit(CONFIG_LOAD_ERROR);
			} catch (VdypApplicationProcessingException e) {
				System.exit(PROCESSING_ERROR);
			}
		}
	}

	// VRI_SUB
	// TODO Fortran takes a vector of flags (FIPPASS) controlling which stages are
	// implemented. FIPSTART always uses the same vector so far now that's not
	// implemented.
	@Override
	public void process() throws ProcessingException {
		try (
				var polyStream = this.<VriPolygon>getStreamingParser(ControlKey.VRI_INPUT_YIELD_POLY);
				var layerStream = this.<Map<LayerType, VriLayer.Builder>>getStreamingParser(
						ControlKey.VRI_INPUT_YIELD_LAYER
				);
				var speciesStream = this
						.<Collection<VriSpecies>>getStreamingParser(ControlKey.VRI_INPUT_YIELD_SPEC_DIST);
				var siteStream = this.<Collection<VriSite>>getStreamingParser(ControlKey.VRI_INPUT_YIELD_HEIGHT_AGE_SI);
		) {
			var combinedStream = new CombinedPolygonStream<VriPolygon>() {

				@Override
				public boolean hasNext() throws IOException, ResourceParseException {
					return polyStream.hasNext();
				}

				@Override
				public VriPolygon next() throws ProcessingException, IOException, ResourceParseException {
					return getPolygon(polyStream, layerStream, speciesStream, siteStream);
				}

			};

			handleProcessing(combinedStream);
		} catch (IOException | ResourceParseException ex) {
			throw new FatalProcessingException("Error while reading or writing data.", ex);
		}
	}

	VriPolygon getPolygon(
			StreamingParser<VriPolygon> polyStream, StreamingParser<Map<LayerType, VriLayer.Builder>> layerStream,
			StreamingParser<Collection<VriSpecies>> speciesStream, StreamingParser<Collection<VriSite>> siteStream
	) throws ProcessingException, IOException, ResourceParseException {

		log.trace("Getting polygon");
		var polygon = polyStream.next();

		BecDefinition bec = polygon.getBiogeoclimaticZone();

		log.trace("Getting species for polygon {}", polygon.getPolygonIdentifier());

		log.trace("Getting sites for polygon {}", polygon.getPolygonIdentifier());
		Collection<VriSite> sites;
		try {
			sites = new LinkedList<>(siteStream.next());
		} catch (NoSuchElementException ex) {
			throw fatalError("Sites file has fewer records than polygon file.", ex);
		}

		Collection<VriSpecies> species;
		try {
			species = speciesStream.next();
		} catch (NoSuchElementException ex) {
			throw fatalError("Species file has fewer records than polygon file.", ex);
		}

		Map<LayerType, VriLayer.Builder> layersBuilders;
		try {
			layersBuilders = layerStream.next();
		} catch (NoSuchElementException ex) {
			throw fatalError("Layers file has fewer records than polygon file.", ex);
		}

		for (final var spec : species) {
			var layerBuilder = layersBuilders.get(spec.getLayerType());

			var foundSite = sites.stream().filter(site -> site.getSiteGenus().equals(spec.getGenus())).findFirst();
			final var specWithSite = VriSpecies.build(builder -> {
				builder.copy(spec);
				builder.addSite(foundSite);
			});
			foundSite.ifPresent(sites::remove);

			// Validate that species belong to the correct polygon
			if (!specWithSite.getPolygonIdentifier().equals(polygon.getPolygonIdentifier())) {
				throw fatalError(
						"Record in species file contains species for polygon {0} when expecting one for {1}.",
						specWithSite.getPolygonIdentifier(), polygon.getPolygonIdentifier()
				);
			}
			if (Objects.isNull(layerBuilder)) {
				throw fatalError(
						"Species entry references layer {0} of polygon {1} but it is not present.",
						specWithSite.getLayerType(), polygon.getPolygonIdentifier()
				);
			}
			layerBuilder.addSpecies(specWithSite);
		}
		if (!sites.isEmpty()) {
			var specNames = sites.stream().map(site -> site.getSiteGenus()).collect(Collectors.joining(", "));
			var layerType = sites.iterator().next().getLayerType();
			throw fatalError(
					"Site entries reference species {0} of layer {1} of polygon {2} but they are not present.",
					specNames, layerType, polygon.getPolygonIdentifier()
			);
		}

		Map<LayerType, VriLayer> layers = getLayersForPolygon(polygon, bec, layersBuilders);

		// Validate that layers belong to the correct polygon
		// I don't think the preceding checks actually allow this to fail but it was in VDYP7 and I'm not entirely sure
		// so i'll leave it in.
		for (var layer : layers.values()) {
			if (!layer.getPolygonIdentifier().equals(polygon.getPolygonIdentifier())) {
				throw fatalError(
						"Record in layer file contains layer for polygon {0} when expecting one for {1}.",
						layer.getPolygonIdentifier(), polygon.getPolygonIdentifier()
				);
			}
		}

		polygon.setLayers(layers);

		return polygon;

	}

	private Map<LayerType, VriLayer>
			getLayersForPolygon(VriPolygon polygon, BecDefinition bec, Map<LayerType, VriLayer.Builder> layersBuilders)
					throws ProcessingException {
		log.trace("Getting layers for polygon {}", polygon.getPolygonIdentifier());
		Map<LayerType, VriLayer> layers;
		try {

			// Do some additional processing then build the layers.
			layers = layersBuilders.values().stream().map(builder -> {

				var layerType = builder.getLayerType().get();

				builder.buildChildren(); // Make sure all children are built before getting them.
				var layerSpecies = builder.getSpecies();

				if (layerType == LayerType.PRIMARY) {
					builder.percentAvailable(polygon.getPercentAvailable().orElse(1f));
				}
				try {
					if (!layerSpecies.isEmpty()) {
						// Finding Primary Species and ITG was in VRI_CHK but they have been moved here so that
						// validation does not alter the data structure.
						var primarySpecs = this.findPrimarySpecies(layerSpecies);
						int itg;
						itg = findItg(primarySpecs);

						builder.inventoryTypeGroup(itg);

						builder.primaryGenus(primarySpecs.get(0).getGenus());

						if (layerType == LayerType.PRIMARY) {
							modifyPrimaryLayerBuild(bec, builder, primarySpecs, itg);
						}
					}
					if (layerType == LayerType.VETERAN) {
						modifyVeteranLayerBuild(layersBuilders, builder);
					}
				} catch (StandProcessingException ex) {
					throw new RuntimeStandProcessingException(ex);
				}
				return builder;
			}).map(VriLayer.Builder::build).collect(Collectors.toUnmodifiableMap(VriLayer::getLayerType, x -> x));

		} catch (RuntimeProcessingException ex) {
			throw ex.unwrap();
		}
		return layers;
	}

	private void modifyVeteranLayerBuild(
			Map<LayerType, VriLayer.Builder> layersBuilders, ca.bc.gov.nrs.vdyp.vri.model.VriLayer.Builder builder
	) throws CrownClosureLowException {
		// This was being done in VRI_CHK but I moved it here to when the object is
		// being built instead.
		if (builder.getBaseArea().map(x -> x <= 0f).orElse(true)
				|| builder.getTreesPerHectare().map(x -> x <= 0f).orElse(true)) {
			// BA or TPH missing from Veteran layer.

			builder.treesPerHectare(0f);

			float crownClosure = builder.getCrownClosure().filter(x -> x > 0f).orElseThrow(
					() -> new CrownClosureLowException(LayerType.VETERAN, builder.getCrownClosure(), Optional.of(0f))

			);
			// If the primary layer base area is positive, multiply that by veteran crown
			// closure, otherwise just use half the veteran crown closure.
			builder.baseArea(
					layersBuilders.get(LayerType.PRIMARY).getBaseArea().filter(x -> x > 0f)
							.map(pba -> crownClosure / 100f * pba).orElse(crownClosure / 2f)
			);
		}
	}

	void modifyPrimaryLayerBuild(
			BecDefinition bec, ca.bc.gov.nrs.vdyp.vri.model.VriLayer.Builder builder, List<VriSpecies> primarySpecs,
			int itg
	) {
		// This was being done in VRI_CHK but I moved it here to when the object is
		// being built instead.
		if (builder.getBaseArea()
				.flatMap(
						ba -> builder.getTreesPerHectare()
								.map(tph -> quadMeanDiameter(ba, tph) < UtilizationClass.U75TO125.lowBound)
				).orElse(false)) {
			builder.baseArea(Optional.empty());
			builder.treesPerHectare(Optional.empty());
		}

		if (primarySpecs.size() > 1) {
			builder.secondaryGenus(primarySpecs.get(1).getGenus());
		}

		builder.empiricalRelationshipParameterIndex(
				findEmpiricalRelationshipParameterIndex(primarySpecs.get(0).getGenus(), bec, itg)
		);
	}

	static final EnumSet<PolygonMode> ACCEPTABLE_MODES = EnumSet.of(PolygonMode.START, PolygonMode.YOUNG);

	@Override
	protected Optional<VdypPolygon> processPolygon(int polygonsRead, VriPolygon polygon) throws ProcessingException {
		log.atInfo().setMessage("Read polygon {}, preparing to process").addArgument(polygon.getPolygonIdentifier())
				.log();
		var mode = polygon.getMode().orElse(PolygonMode.START);

		if (mode == PolygonMode.DONT_PROCESS) {
			log.atInfo().setMessage("Skipping polygon with mode {}").addArgument(mode).log();
			return Optional.empty();
		}

		log.atInfo().setMessage("Checking validity of polygon {}:{}").addArgument(polygonsRead)
				.addArgument(polygon.getPolygonIdentifier()).log();

		mode = checkPolygon(polygon);

		final VriPolygon preProcessedPolygon;
		switch (mode) {
		case YOUNG:
			log.atTrace().setMessage(SPECIAL_PROCESSING_LOG_TEMPLATE).addArgument(mode).log();
			preProcessedPolygon = processBatn(processYoung(polygon));
			break;
		case BATC:
			log.atTrace().setMessage(SPECIAL_PROCESSING_LOG_TEMPLATE).addArgument(mode).log();
			preProcessedPolygon = processBatc(polygon);
			break;
		case BATN:
			log.atTrace().setMessage(SPECIAL_PROCESSING_LOG_TEMPLATE).addArgument(mode).log();
			preProcessedPolygon = processBatn(polygon);
			break;
		default:
			log.atTrace().setMessage("No special processing for mode {}").addArgument(mode).log();
			preProcessedPolygon = polygon;
			break;
		}

		AtomicInteger veteranWarnings = new AtomicInteger(0);
		try {
			var inputTph = new AtomicReference<Float>();
			var result = Optional.of(VdypPolygon.build(pBuilder -> {
				pBuilder.adapt(preProcessedPolygon, x -> x.orElse(0f));

				pBuilder.addLayer(lBuilder -> {
					try {
						lBuilder.adapt(preProcessedPolygon.getLayers().get(LayerType.PRIMARY));
						processPrimaryLayer(preProcessedPolygon, lBuilder);
					} catch (ProcessingException e) {
						throw new RuntimeProcessingException(e);
					}
				});
				if (preProcessedPolygon.getLayers().containsKey(LayerType.VETERAN)) {
					pBuilder.addLayer(lBuilder -> {
						try {
							final ResultWithStatus<Float, BasicStatus> veteranResult = processVeteranLayer(
									preProcessedPolygon, lBuilder
							);
							if (!veteranResult.status().isOK()) {
								veteranWarnings.getAndIncrement();
							}
							inputTph.set(veteranResult.value());
						} catch (ProcessingException e) {
							throw new RuntimeProcessingException(e);
						}
					});
				}

			}));
			if (result.isPresent()) {
				postProcessPolygon(polygon, inputTph.get(), result.get());
			}
			return result;
		} catch (RuntimeProcessingException e) {
			throw e.unwrap();
		}
	}

	/**
	 * Additional steps after primary processing is done. Validates result and fills in some utilization component
	 * values
	 *
	 * @param sourcePoly
	 * @param inputTph
	 * @param resultPoly
	 * @throws ProcessingException
	 */
	void postProcessPolygon(VriPolygon sourcePoly, Float inputTph, VdypPolygon resultPoly) throws ProcessingException {

		var resultPrimaryLayer = resultPoly.getLayers().get(LayerType.PRIMARY);
		var resultVeteranLayer = resultPoly.getLayers().get(LayerType.VETERAN);
		var bec = sourcePoly.getBiogeoclimaticZone();

		getDqBySpecies(resultPrimaryLayer, bec.getRegion());

		estimateSmallComponents(sourcePoly, resultPrimaryLayer);

		computeUtilizationComponentsPrimary(
				bec, resultPrimaryLayer, VolumeComputeMode.BY_UTIL_WITH_WHOLE_STEM_BY_SPEC,
				CompatibilityVariableMode.NONE
		);

		if (resultVeteranLayer != null) {
			// YUCV
			computeUtilizationComponentsVeteran(resultVeteranLayer, bec);

			var input = inputTph;
			var computed = resultVeteranLayer.getTreesPerHectareByUtilization().getAll();
			if (FloatMath.abs(input - computed) / input > 0.0005) {
				throw fatalError(
						"Computed tree density sum {0} trees/ha did not match input {1} trees/ha", computed, input
				);
			}

		}

	}

	void processPrimaryLayer(VriPolygon polygon, VdypLayer.Builder lBuilder) throws FatalProcessingException {
		var primaryLayer = polygon.getLayers().get(LayerType.PRIMARY);
		var bec = polygon.getBiogeoclimaticZone();

		// BA_L1
		float primaryBaseArea = requirePositive(primaryLayer.getBaseArea(), "Primary layer base area");

		// TPH_L1
		var primaryLayerDensity = requirePositive(primaryLayer.getTreesPerHectare(), "Primary layer trees per hectare");

		var primarySiteIn = require(primaryLayer.getPrimarySite(), "Primary site for primary layer");

		var primarySpeciesPercent = require(primaryLayer.getPrimarySpeciesRecord(), "Primary species for primary layer")
				.getFractionGenus();

		// TPH_L1

		// TPHsp before the individual species loop, then the fortran variable gets re-used, see speciesDensity below
		var primarySpeciesDensity = primarySpeciesPercent * primaryLayerDensity;

		// HDL1 or HT_L1
		float leadHeight = requirePositive(primarySiteIn.getHeight(), "Height for primary layer");

		// HLPL1
		// EMP050 Method 1
		var primaryHeight = estimationMethods.primaryHeightFromLeadHeight(
				leadHeight, primarySiteIn.getSiteGenus(), bec.getRegion(), primarySpeciesDensity
		);

		float layerQuadMeanDiameter = quadMeanDiameter(primaryBaseArea, primaryLayerDensity);
		lBuilder.quadraticMeanDiameterByUtilization(layerQuadMeanDiameter);
		lBuilder.baseAreaByUtilization(primaryBaseArea);
		lBuilder.treesPerHectareByUtilization(primaryLayerDensity);
		lBuilder.empiricalRelationshipParameterIndex(primaryLayer.getEmpiricalRelationshipParameterIndex());

		lBuilder.adaptSpecies(primaryLayer, (sBuilder, vriSpec) -> {
			var vriSite = vriSpec.getSite();

			applyGroups(bec, vriSpec.getGenus(), sBuilder);

			float fraction = primaryLayer.getSpecies().size() == 1 ? 1 : vriSpec.getFractionGenus();

			float specBaseArea = primaryBaseArea * fraction;
			sBuilder.baseArea(specBaseArea);

			if (vriSite.map(site -> site == primarySiteIn).orElse(false)) {
				sBuilder.loreyHeight(primaryHeight);
				sBuilder.adaptSite(vriSite.get(), (iBuilder, vriSite2) -> {
					vriSite2.getHeight().ifPresent(iBuilder::height);
				});
			} else {
				var loreyHeight = vriSite
						.flatMap(site -> site.getHeight().filter(x -> getDebugModes().getValue(2) != 1).map(height -> {
							// DQsp
							float speciesQuadMeanDiameter = Math.max(
									UtilizationClass.U75TO125.lowBound, height / leadHeight * layerQuadMeanDiameter
							);

							// TPHsp inside the individual species loop, see primarySpeciesDensity above
							float speciesDensity = treesPerHectare(specBaseArea, speciesQuadMeanDiameter);

							// EMP050
							return (float) estimationMethods.primaryHeightFromLeadHeight(
									site.getHeight().get(), site.getSiteGenus(), bec.getRegion(), speciesDensity
							);
						})).orElseGet(() -> {
							try {
								// EMP053
								return estimationMethods.estimateNonPrimaryLoreyHeight(
										vriSpec.getGenus(), primarySiteIn.getSiteGenus(), bec, leadHeight, primaryHeight
								);
							} catch (ProcessingException e) {
								throw new RuntimeProcessingException(e);
							}
						});

				float maxHeight = estimationMethods.getLimitsForHeightAndDiameter(vriSpec.getGenus(), bec.getRegion())
						.loreyHeightMaximum();
				loreyHeight = Math.min(loreyHeight, maxHeight);
				sBuilder.loreyHeight(loreyHeight);
			}
			vriSite.ifPresent(site -> sBuilder.adaptSite(site, (iBuilder, vriSite2) -> {
				vriSite2.getHeight().ifPresent(iBuilder::height);
			}));
			this.applyGroups(bec, vriSpec.getGenus(), sBuilder);
		});

		lBuilder.buildChildren();

		var species = lBuilder.getSpecies();

		float sumBaseAreaLoreyHeight = 0;
		// find aggregate lorey height
		if (species.size() == 1) {
			sumBaseAreaLoreyHeight = primaryBaseArea;
		} else {
			for (var spec : species) {
				float specBaseArea = spec.getBaseAreaByUtilization().getAll();
				float specHeight = spec.getLoreyHeightByUtilization().getAll();
				spec.getBaseAreaByUtilization().setAll(specBaseArea);
				sumBaseAreaLoreyHeight += specBaseArea * specHeight;
			}
		}

		lBuilder.loreyHeightByUtilization(sumBaseAreaLoreyHeight / primaryBaseArea);

	}

	// ROOTV01
	void getDqBySpecies(VdypLayer layer, Region region) throws FatalProcessingException {

		// DQ_TOT
		float quadMeanDiameterTotal = layer.getQuadraticMeanDiameterByUtilization().getAll();
		// BA_TOT
		float baseAreaTotal = layer.getBaseAreaByUtilization().getAll();
		// TPH_TOT
		float treeDensityTotal = treesPerHectare(baseAreaTotal, quadMeanDiameterTotal);

		float loreyHeightTotal = layer.getLoreyHeightByUtilization().getAll();

		// DQV
		Map<String, Float> initialDqEstimate = new LinkedHashMap<>(layer.getSpecies().size());
		// BAV
		Map<String, Float> baseAreaPerSpecies = new LinkedHashMap<>(layer.getSpecies().size());
		// DQMIN
		Map<String, Float> minPerSpecies = new LinkedHashMap<>(layer.getSpecies().size());
		// DQMAX
		Map<String, Float> maxPerSpecies = new LinkedHashMap<>(layer.getSpecies().size());
		// DQFINAL
		Map<String, Float> resultsPerSpecies = new LinkedHashMap<>(layer.getSpecies().size());

		getDqBySpeciesInitial(
				// In
				layer, region, quadMeanDiameterTotal, baseAreaTotal, treeDensityTotal, loreyHeightTotal,
				// Out
				initialDqEstimate, baseAreaPerSpecies, minPerSpecies, maxPerSpecies
		);

		resultsPerSpecies.putAll(initialDqEstimate);

		findRootForQuadMeanDiameterFractionalError(
				-0.6f, 0.5f, resultsPerSpecies, initialDqEstimate, baseAreaPerSpecies, minPerSpecies, maxPerSpecies,
				treeDensityTotal
		);

		applyDqBySpecies(layer, baseAreaTotal, baseAreaPerSpecies, resultsPerSpecies);
	}

	void applyDqBySpecies(
			VdypLayer layer, float baseAreaTotal, Map<String, Float> baseAreaPerSpecies,
			Map<String, Float> resultsPerSpecies
	) {
		float quadMeanDiameterTotal;
		float treeDensityTotal;
		treeDensityTotal = 0;
		for (var spec : layer.getSpecies().values()) {
			float specDq = resultsPerSpecies.get(spec.getGenus());
			float specBa = baseAreaPerSpecies.get(spec.getGenus());
			float specTph = treesPerHectare(specBa, specDq);
			treeDensityTotal += specTph;
			spec.getQuadraticMeanDiameterByUtilization().setAll(specDq);
			spec.getTreesPerHectareByUtilization().setAll(specTph);
		}
		quadMeanDiameterTotal = quadMeanDiameter(baseAreaTotal, treeDensityTotal);
		layer.getTreesPerHectareByUtilization().setAll(treeDensityTotal);
		layer.getQuadraticMeanDiameterByUtilization().setAll(quadMeanDiameterTotal);
	}

	void getDqBySpeciesInitial(
			VdypLayer layer, Region region, float quadMeanDiameterTotal, float baseAreaTotal, float treeDensityTotal,
			float loreyHeightTotal, Map<String, Float> initialDqEstimate, Map<String, Float> baseAreaPerSpecies,
			Map<String, Float> minPerSpecies, Map<String, Float> maxPerSpecies
	) throws FatalProcessingException {
		for (var spec : layer.getSpecies().values()) {
			// EMP060
			float specDq = estimationMethods.estimateQuadMeanDiameterForSpecies(
					spec, layer.getSpecies(), region, quadMeanDiameterTotal, baseAreaTotal, treeDensityTotal,
					loreyHeightTotal
			);

			var limits = getLimitsForSpecies(spec, region);

			float min = Math.max(
					7.6f, limits.minQuadMeanDiameterLoreyHeightRatio() * spec.getLoreyHeightByUtilization().getAll()
			);
			float loreyHeightToUse = Math.max(spec.getLoreyHeightByUtilization().getAll(), 7.0f);
			float max = Math.min(
					limits.quadMeanDiameterMaximum(), limits.maxQuadMeanDiameterLoreyHeightRatio() * loreyHeightToUse
			);
			max = Math.max(7.75f, max);

			minPerSpecies.put(spec.getGenus(), min);
			maxPerSpecies.put(spec.getGenus(), max);

			specDq = FloatMath.clamp(specDq, Math.max(min, 7.75f), max);

			initialDqEstimate.put(spec.getGenus(), specDq);

			baseAreaPerSpecies.put(spec.getGenus(), spec.getBaseAreaByUtilization().getAll());
		}
	}

	protected ComponentSizeLimits getLimitsForSpecies(VdypSpecies spec, Region region) {
		// TODO for JPROGRAM = 7 implement this differently, see ROOTV01 L91-L99

		// EMP061
		return estimationMethods.getLimitsForHeightAndDiameter(spec.getGenus(), region);
	}

	float quadMeanDiameterFractionalError(
			double x, Map<String, Float> finalDiameters, Map<String, Float> initial, Map<String, Float> baseArea,
			Map<String, Float> min, Map<String, Float> max, float totalTreeDensity
	) {
		finalDiameters.clear();

		float xToUse = FloatMath.clamp((float) x, -10, 10);

		double tphSum = initial.entrySet().stream().mapToDouble(spec -> {
			float speciesFinal = quadMeanDiameterSpeciesAdjust(
					xToUse, spec.getValue(), min.get(spec.getKey()), max.get(spec.getKey())
			);
			finalDiameters.put(spec.getKey(), speciesFinal);
			return treesPerHectare(baseArea.get(spec.getKey()), speciesFinal);
		}).sum();

		return (float) ( (tphSum - totalTreeDensity) / totalTreeDensity);
	}

	float quadMeanDiameterSpeciesAdjust(float x, float initialDq, float min, float max) {
		return FloatMath.clamp(
				UtilizationClass.U75TO125.lowBound
						+ (initialDq - UtilizationClass.U75TO125.lowBound) * FloatMath.exp(x),
				min, max
		);
	}

	private record VeteranResult(float treesPerHectare, boolean lowDq) {
	}

	private ResultWithStatus<Float, ResultWithStatus.BasicStatus>
			processVeteranLayer(VriPolygon polygon, VdypLayer.Builder lBuilder) throws FatalProcessingException {
		var veteranLayer = polygon.getLayers().get(LayerType.VETERAN);
		lBuilder.layerType(LayerType.VETERAN);

		lBuilder.adapt(veteranLayer);

		var bec = polygon.getBiogeoclimaticZone();

		var primarySite = veteranLayer.getPrimarySite();
		var primarySpecies = veteranLayer.getPrimarySpeciesRecord();

		float ageTotal = primarySite.flatMap(VriSite::getAgeTotal).map(at -> at + veteranLayer.getAgeIncrease())
				.orElse(0f); // AGETOTLV
		float yearsToBreastHeight = primarySite.flatMap(VriSite::getYearsToBreastHeight).orElse(0f); // YTBHLV
		float dominantHeight = primarySite.flatMap(VriSite::getHeight).orElse(0f); // HDLV

		float baseArea = requirePositive(veteranLayer.getBaseArea(), "Base area for veteran layer");
		float treesPerHectare = requirePositive(veteranLayer.getTreesPerHectare(), "Tree density for veteran layer");

		boolean lowDq = false;

		{
			var enforcedTph = enforceMinimumDiameter(baseArea, treesPerHectare);
			treesPerHectare = enforcedTph.treesPerHectare();
			lowDq = enforcedTph.lowDq();
		}

		float inputTreesPerHectare = treesPerHectare; // TPHInput

		lBuilder.adaptSpecies(veteranLayer, (sBuilder, spec) -> {
			applyGroups(bec, spec.getGenus(), sBuilder);
			if (primarySpecies.map(spec::equals).orElse(false)) {
				sBuilder.addSite(iBuilder -> {
					iBuilder.adapt(primarySite.get());
					iBuilder.ageTotal(ageTotal);
					iBuilder.yearsToBreastHeight(yearsToBreastHeight);
				});
			}
			float specHeight = spec.getSite().flatMap(VriSite::getHeight).filter(x -> x > 0).orElse(dominantHeight);
			float specBaseArea = spec.getFractionGenus() * baseArea;
			float specQuadMeanDiameter = max(
					estimateVeteranQuadMeanDiameter(spec.getGenus(), bec, specHeight), VETERAN_MIN_DQ
			);
			float specTreeDensity = BaseAreaTreeDensityDiameter.treesPerHectare(specBaseArea, specQuadMeanDiameter);
			sBuilder.loreyHeight(specHeight);
			sBuilder.baseArea(specBaseArea);
			sBuilder.quadMeanDiameter(specQuadMeanDiameter);
			sBuilder.treesPerHectare(specTreeDensity);
		});

		lBuilder.buildChildren();
		var specList = lBuilder.getSpecies();

		// Really crude reconciliation to the specified TPH (if any)
		// Could improve with code similar to ROOTV01
		if (treesPerHectare > 0) {
			float tphSum = 0;
			for (var spec : specList) {
				tphSum += spec.getTreesPerHectareByUtilization().getAll();
			}
			float k = treesPerHectare / tphSum;
			for (var spec : specList) {
				float specBaseArea = spec.getBaseAreaByUtilization().getLarge();
				float specTreesPerHectare = spec.getTreesPerHectareByUtilization().getLarge() * k;
				float specQuadMeanDiameter = BaseAreaTreeDensityDiameter
						.quadMeanDiameter(specBaseArea, specTreesPerHectare);
				if (specQuadMeanDiameter < VETERAN_MIN_DQ) {
					specQuadMeanDiameter = VETERAN_MIN_DQ;
					specTreesPerHectare = BaseAreaTreeDensityDiameter
							.treesPerHectare(specBaseArea, specQuadMeanDiameter);
				}
				spec.setTreesPerHectareByUtilization(Utils.utilizationVector(specTreesPerHectare));
				spec.setQuadraticMeanDiameterByUtilization(Utils.utilizationVector(specQuadMeanDiameter));
			}

		}

		// Sum BA and TPH
		float tphSum = 0; // TPH(0,4)
		for (var spec : specList) {
			tphSum += spec.getTreesPerHectareByUtilization().getAll();
		}

		if (polygon.getMode().filter(mode -> mode == PolygonMode.BATC).isPresent()) {
			// for mode 4 (with CC) input TPH was NOT available.
			inputTreesPerHectare = tphSum;
			treesPerHectare = tphSum;
		}

		return new ResultWithStatus<>(
				inputTreesPerHectare, lowDq ? ResultWithStatus.BasicStatus.OK : ResultWithStatus.BasicStatus.WARNING
		);
	}

	static final String MINIMUM_DIAMETER_BASE_MESSAGE = "Quadratic mean diameter {0} cm was lower than {1} cm";
	static final String MINIMUM_DIAMETER_WARN_MESSAGE = MINIMUM_DIAMETER_BASE_MESSAGE
			+ ", raising tree density to {2} trees/ha";
	static final String MINIMUM_DIAMETER_FAIL_MESSAGE = MINIMUM_DIAMETER_BASE_MESSAGE + ".";

	private VeteranResult enforceMinimumDiameter(Float baseArea, Float treesPerHectare)
			throws FatalProcessingException {
		final float quadMeanDiameter = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea, treesPerHectare);
		boolean lowDq = false;
		if (quadMeanDiameter < VETERAN_MIN_DQ) {
			if (this.getId() == VdypApplicationIdentifier.VRI_START && this.getDebugModes().getValue(1) == 2) {
				throw new FatalProcessingException(
						MessageFormat.format(MINIMUM_DIAMETER_FAIL_MESSAGE, quadMeanDiameter, VETERAN_MIN_DQ)
				);
			}
			treesPerHectare = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea, VETERAN_MIN_DQ);
			log.atWarn().setMessage(MINIMUM_DIAMETER_WARN_MESSAGE).addArgument(treesPerHectare);
			lowDq = true;
		}
		return new VeteranResult(treesPerHectare, lowDq);
	}

	// EMP097 TODO move to EstimationMethods and this should probably be used in FipStart

	float estimateVeteranQuadMeanDiameter(String sp0, BecDefinition bec, float loreyHeight) {
		var vetDqMap = Utils.<MatrixMap2<String, Region, Coefficients>>expectParsedControl(
				controlMap, ControlKey.VETERAN_LAYER_DQ, MatrixMap2.class
		);
		var coe = vetDqMap.get(sp0, bec.getRegion());
		var a0 = coe.getCoe(1);
		var a1 = coe.getCoe(2);
		var a2 = coe.getCoe(3);

		return a0 + a1 * pow(loreyHeight, a2);
	}

	// VRI_CHK
	PolygonMode checkPolygon(VriPolygon polygon) throws ProcessingException {

		BecDefinition bec = polygon.getBiogeoclimaticZone();

		if (!polygon.getLayers().containsKey(LayerType.PRIMARY)) {
			throw new LayerMissingException(LayerType.PRIMARY);
		}

		// At this point the Fortran implementation nulled the BA and TPH of Primary
		// layers if the BA and TPH were present and resulted in a DQ <7.5
		// I did that in getPolygon instead of here.

		for (var layer : polygon.getLayers().values()) {
			checkLayer(polygon, layer);
		}

		PolygonMode mode = checkPolygonForMode(polygon, bec);

		Map<String, Float> minMap = Utils.expectParsedControl(controlMap, ControlKey.MINIMA, Map.class);

		float veteranMinHeight = minMap.get(VriControlParser.MINIMUM_VETERAN_HEIGHT);

		VriLayer veteranLayer = polygon.getLayers().get(LayerType.VETERAN);
		if (veteranLayer != null) {
			throwIfPresent(
					HeightLowException.check(
							LayerType.VETERAN, veteranLayer.getPrimarySite().flatMap(VriSite::getHeight),
							veteranMinHeight
					)
			);
		}

		return mode;
	}

	private void checkLayer(VriPolygon polygon, VriLayer layer) throws StandProcessingException {
		if (layer.getSpecies().isEmpty())
			return;
		if (layer.getLayerType() == LayerType.PRIMARY)
			this.getPercentTotal(layer); // Validate that percent total is close to 100%

		// VDYP7 figured out primary species and ITG group here but that has moved to getLayersForPolygon
		List<String> missingValues = new ArrayList<>();
		final Optional<PolygonMode> mode = polygon.getMode();

		for (var site : layer.getPriorityOrderedSites()) {
			var ageTotal = site.getAgeTotal();
			var yearsToBreastHeight = site.getYearsToBreastHeight();
			var height = site.getHeight();

			// VDYP7 handles these with a different error code from the other low value/null checks so they get a
			// different exception
			missingValues.clear();
			if (mode.map(PolygonMode.YOUNG::equals).orElse(false) && layer.getLayerType() == LayerType.PRIMARY) {
				if (ageTotal.map(x -> x <= 0f).orElse(true)) {
					missingValues.add(AGE_TOTAL_PROPERTY_NAME);
				}
				if (yearsToBreastHeight.map(x -> x <= 0f).orElse(true)) {
					missingValues.add(YEARS_TO_BREAST_HEIGHT_PROPERTY_NAME);
				}
			} else {
				if (height.map(x -> x <= 0f).orElse(true)) {
					missingValues.add(HEIGHT_PROPERTY_NAME);
				}
			}
			if (missingValues.isEmpty()) {
				layer.setPrimaryGenusForCalculation(Optional.of(site.getSiteGenus()));
				break;
			}
		}
		if (!missingValues.isEmpty()) {
			throw new LayerMissingValuesRequiredForMode(layer.getLayerType(), mode, missingValues);
		}
	}

	static final String SITE_INDEX_PROPERTY_NAME = "Site index";
	static final String AGE_TOTAL_PROPERTY_NAME = "Age total";
	static final String BREAST_HEIGHT_AGE_PROPERTY_NAME = "Breast height age";
	static final String YEARS_TO_BREAST_HEIGHT_PROPERTY_NAME = "Years to breast height";
	static final String HEIGHT_PROPERTY_NAME = "Height";
	static final String BASE_AREA_PROPERTY_NAME = "Base area";
	static final String TREES_PER_HECTARE_PROPERTY_NAME = "Trees per hectare";
	static final String CROWN_CLOSURE_PROPERTY_NAME = "Crown closure";

	protected PolygonMode checkPolygonForMode(VriPolygon polygon, BecDefinition bec) throws ProcessingException {
		VriLayer primaryLayer = polygon.getLayers().get(LayerType.PRIMARY);
		Optional<VriSite> primarySite = primaryLayer.getPrimarySite();
		var ageTotal = primarySite.flatMap(VriSite::getAgeTotal);
		var height = primarySite.flatMap(VriSite::getHeight);
		var siteIndex = primarySite.flatMap(VriSite::getSiteIndex);
		var yearsToBreastHeight = primarySite.flatMap(VriSite::getYearsToBreastHeight);
		var baseArea = primaryLayer.getBaseArea();
		var treesPerHectare = primaryLayer.getTreesPerHectare();
		var crownClosure = primaryLayer.getCrownClosure();
		var percentForest = polygon.getPercentAvailable();

		try {
			PolygonMode mode = polygon.getMode().orElseGet(() -> {
				try {
					return findDefaultPolygonMode(
							ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest,
							primaryLayer.getSpecies().values(), bec,
							primaryLayer.getEmpiricalRelationshipParameterIndex()
					);
				} catch (FatalProcessingException e) {
					throw new RuntimeProcessingException(e);
				}
			});
			polygon.setMode(Optional.of(mode));
			Optional<Float> primaryBreastHeightAge = Utils.mapBoth(
					primaryLayer.getPrimarySite().flatMap(VriSite::getAgeTotal),
					primaryLayer.getPrimarySite().flatMap(VriSite::getYearsToBreastHeight), (at, ytbh) -> at - ytbh
			);
			log.atDebug().setMessage("Polygon mode {} checks").addArgument(mode).log();
			switch (mode) {

			case START:
				throwIfPresent(SiteIndexLowException.check(LayerType.PRIMARY, siteIndex, 0f));
				throwIfPresent(TotalAgeLowException.check(LayerType.PRIMARY, ageTotal, 0f));
				throwIfPresent(BreastHeightAgeLowException.check(LayerType.PRIMARY, primaryBreastHeightAge, 0f));
				throwIfPresent(HeightLowException.check(LayerType.PRIMARY, height, 4.5f));
				throwIfPresent(BaseAreaLowException.check(LayerType.PRIMARY, baseArea, 0f));
				throwIfPresent(TreesPerHectareLowException.check(LayerType.PRIMARY, treesPerHectare, 0f));
				break;

			case YOUNG:
				throwIfPresent(SiteIndexLowException.check(LayerType.PRIMARY, siteIndex, 0f));
				throwIfPresent(TotalAgeLowException.check(LayerType.PRIMARY, ageTotal, 0f));
				throwIfPresent(YearsToBreastHeightLowException.check(LayerType.PRIMARY, yearsToBreastHeight, 0f));
				break;

			case BATN:
				throwIfPresent(SiteIndexLowException.check(LayerType.PRIMARY, siteIndex, 0f));
				throwIfPresent(TotalAgeLowException.check(LayerType.PRIMARY, ageTotal, 0f));
				throwIfPresent(BreastHeightAgeLowException.check(LayerType.PRIMARY, primaryBreastHeightAge, 0f));
				throwIfPresent(HeightLowException.check(LayerType.PRIMARY, height, 1.3f));
				break;

			case BATC:
				throwIfPresent(SiteIndexLowException.check(LayerType.PRIMARY, siteIndex, 0f));
				throwIfPresent(TotalAgeLowException.check(LayerType.PRIMARY, ageTotal, 0f));
				throwIfPresent(BreastHeightAgeLowException.check(LayerType.PRIMARY, primaryBreastHeightAge, 0f));
				throwIfPresent(HeightLowException.check(LayerType.PRIMARY, height, 1.3f));
				throwIfPresent(CrownClosureLowException.check(LayerType.PRIMARY, Optional.of(crownClosure), 0f));
				break;

			case DONT_PROCESS:
				log.atDebug().setMessage("Skipping validation for ignored polygon");
				// Do Nothing
				break;
			}
			return mode;
		} catch (RuntimeProcessingException e) {
			throw new FatalProcessingException(e);
		}
	}

	// UPPERGEN Method 1
	Coefficients upperBounds(int baseAreaGroup) {
		var upperBoundsMap = Utils
				.<Map<Integer, Coefficients>>expectParsedControl(controlMap, ControlKey.BA_DQ_UPPER_BOUNDS, Map.class);
		return Utils.<Coefficients>optSafe(upperBoundsMap.get(baseAreaGroup)).orElseThrow(
				() -> new IllegalStateException("Could not find limits for base area group " + baseAreaGroup)
		);
	}

	float upperBoundsBaseArea(int baseAreaGroup) {
		return upperBounds(baseAreaGroup).getCoe(1);
	}

	float upperBoundsQuadMeanDiameter(int baseAreaGroup) {
		return upperBounds(baseAreaGroup).getCoe(2);
	}

	// EMP106
	float estimateBaseAreaYield(
			float dominantHeight, float breastHeightAge, Optional<Float> baseAreaOverstory, boolean fullOccupancy,
			Collection<? extends BaseVdypSpecies<? extends BaseVdypSite>> species, BecDefinition bec, int baseAreaGroup
	) throws FatalProcessingException {
		var coe = estimateBaseAreaYieldCoefficients(species, bec);

		float upperBoundBaseArea = upperBoundsBaseArea(baseAreaGroup);

		/*
		 * The original Fortran had the following comment and a commented out modification to upperBoundsBaseArea
		 * (BATOP98). I have included them here.
		 */

		/*
		 * And one POSSIBLY one last vestage of grouping by ITG That limit applies to full occupancy and Empirical
		 * occupancy. They were derived as the 98th percentile of Empirical stocking, though adjusted PSP's were
		 * included. If the ouput of this routine is bumped up from empirical to full, MIGHT adjust this limit DOWN
		 * here, so that at end, it is correct. Tentatively decide NOT to do this.
		 */

		// if (fullOccupancy)
		// upperBoundsBaseArea *= EMPOC;

		float ageToUse = breastHeightAge;

		// TODO getDebugMode(2)==1

		if (ageToUse <= 0f) {
			throw fatalError("Age was not positive");
		}

		float trAge = FloatMath.log(ageToUse);

		float a00 = Math.max(coe.getCoe(0) + coe.getCoe(1) * trAge, 0f);
		float ap = Math.max(coe.getCoe(3) + coe.getCoe(4) * trAge, 0f);

		float bap;
		if (dominantHeight <= coe.getCoe(2)) {
			bap = 0f;
		} else {
			bap = a00 * FloatMath.pow(dominantHeight - coe.getCoe(2), ap)
					* FloatMath.exp(coe.getCoe(5) * dominantHeight + coe.getCoe(6) * baseAreaOverstory.orElse(0f));
			bap = Math.min(bap, upperBoundBaseArea);
		}

		if (fullOccupancy)
			bap /= EMPOC;

		return bap;
	}

	Coefficients estimateBaseAreaYieldCoefficients(
			Collection<? extends BaseVdypSpecies<? extends BaseVdypSite>> species, BecDefinition bec
	) {
		var coe = sumCoefficientsWeightedBySpeciesAndDecayBec(species, bec, ControlKey.BA_YIELD, 7);

		// TODO confirm going over 0.5 should drop to 0 as this seems odd.
		coe.scalarInPlace(5, x -> x > 0.0f ? 0f : x);
		return coe;
	}

	Coefficients sumCoefficientsWeightedBySpeciesAndDecayBec(
			Collection<? extends BaseVdypSpecies<? extends BaseVdypSite>> species, BecDefinition bec, ControlKey key,
			int size
	) {
		var coeMap = Utils
				.<MatrixMap2<String, String, Coefficients>>expectParsedControl(controlMap, key, MatrixMap2.class);

		final String decayBecAlias = bec.getDecayBec().getAlias();

		return weightedCoefficientSum(
				size, 0, //
				species, //
				BaseVdypSpecies::getFractionGenus, // Weight by fraction
				spec -> coeMap.get(decayBecAlias, spec.getGenus())
		);
	}

	// EMP107
	/**
	 *
	 * @param dominantHeight  Dominant height (m)
	 * @param breastHeightAge breast height age
	 * @param veteranBaseArea Basal area of overstory (>= 0)
	 * @param species         Species for the layer
	 * @param bec             BEC of the polygon
	 * @param baseAreaGroup   Index of the base area group
	 * @return DQ of primary layer (w DBH >= 7.5)
	 * @throws FatalProcessingException
	 */
	float estimateQuadMeanDiameterYield(
			float dominantHeight, float breastHeightAge, Optional<Float> veteranBaseArea,
			Collection<? extends BaseVdypSpecies<? extends BaseVdypSite>> species, BecDefinition bec, int baseAreaGroup
	) throws FatalProcessingException {
		final var coe = sumCoefficientsWeightedBySpeciesAndDecayBec(species, bec, ControlKey.DQ_YIELD, 6);

		// TODO handle getDebugMode(2) case
		final float ageUse = breastHeightAge;

		final float upperBoundsQuadMeanDiameter = upperBoundsQuadMeanDiameter(baseAreaGroup);

		if (ageUse <= 0f) {
			throw fatalError("Primary breast height age must be positive but was {0,number}", ageUse);
		}

		final float trAge = FloatMath.log(ageUse);

		final float c0 = coe.getCoe(0);
		final float c1 = Math.max(coe.getCoe(1) + coe.getCoe(2) * trAge, 0f);
		final float c2 = Math.max(coe.getCoe(3) + coe.getCoe(4) * trAge, 0f);

		return FloatMath.clamp(c0 + c1 * FloatMath.pow(dominantHeight - 5f, c2), 7.6f, upperBoundsQuadMeanDiameter);

	}

	PolygonMode findDefaultPolygonMode(
			Optional<Float> ageTotal, Optional<Float> yearsToBreastHeight, Optional<Float> height,
			Optional<Float> baseArea, Optional<Float> treesPerHectare, Optional<Float> percentForest,
			Collection<VriSpecies> species, BecDefinition bec, Optional<Integer> baseAreaGroup
	) throws FatalProcessingException {
		Optional<Float> ageBH = ageTotal.map(at -> at - yearsToBreastHeight.orElse(3f));

		float bap;
		if (ageBH.map(abh -> abh >= 1).orElse(false)) {
			bap = this.estimateBaseAreaYield(
					height.get(), ageBH.get(), Optional.empty(), false, species, bec, baseAreaGroup.get()
			);
		} else {
			bap = 0;
		}

		var mode = PolygonMode.START;

		Map<String, Float> minMap = Utils.expectParsedControl(controlMap, ControlKey.MINIMA, Map.class);

		float minHeight = minMap.get(BaseControlParser.MINIMUM_HEIGHT);
		float minBA = minMap.get(BaseControlParser.MINIMUM_BASE_AREA);
		float minPredictedBA = minMap.get(BaseControlParser.MINIMUM_PREDICTED_BASE_AREA);

		if (height.map(h -> h < minHeight).orElse(true)) {
			mode = PolygonMode.YOUNG;

			log.atDebug().setMessage("Mode {} because Height {} is below minimum {}.").addArgument(mode)
					.addArgument(height).addArgument(minHeight).log();
		} else if (bap < minPredictedBA) {
			mode = PolygonMode.YOUNG;

			log.atDebug().setMessage("Mode {} because predicted Base Area {} is below minimum {}.").addArgument(mode)
					.addArgument(bap).addArgument(minBA).log();
		} else if (baseArea.map(x -> x == 0).orElse(true) || treesPerHectare.map(x -> x == 0).orElse(true)) {
			mode = PolygonMode.YOUNG;

			log.atDebug().setMessage("Mode {} because given Base Area and Trees Per Hectare were not specified or zero")
					.addArgument(mode).log();
		} else {
			var ration = Utils.mapBoth(baseArea, percentForest, (ba, pf) -> ba * (100f / pf));

			if (ration.map(r -> r < minBA).orElse(false)) {
				mode = PolygonMode.YOUNG;
				log.atDebug().setMessage(
						"Mode {} because ration ({}) of given Base Area ({}) to Percent Forest Land ({}) was below minimum {}"
				).addArgument(mode).addArgument(ration).addArgument(baseArea).addArgument(percentForest)
						.addArgument(minBA).log();

			}
		}
		log.atDebug().setMessage("Defaulting to mode {}.").addArgument(mode).log();

		return mode;
	}

	VdypPolygon createVdypPolygon(VriPolygon sourcePolygon, Map<LayerType, VdypLayer> processedLayers) {

		// TODO expand this

		var vdypPolygon = VdypPolygon.build(builder -> builder.adapt(sourcePolygon, x -> x.get()));
		vdypPolygon.setLayers(processedLayers);
		return vdypPolygon;
	}

	@Override
	public VdypApplicationIdentifier getId() {
		return VdypApplicationIdentifier.VRI_START;
	}

	@Override
	protected BaseControlParser getControlFileParser() {
		return new VriControlParser();
	}

	@Override
	protected VriSpecies copySpecies(VriSpecies toCopy, Consumer<Builder<VriSpecies, VriSite, ?>> config) {
		return VriSpecies.build(builder -> builder.copy(toCopy));
	}

	static record Increase(float dominantHeight, float ageIncrease) {
	}

	VriPolygon processYoung(VriPolygon poly) throws FatalProcessingException, FailedToGrowYoungStandException {

		PolygonIdentifier polygonIdentifier = poly.getPolygonIdentifier();
		int year = polygonIdentifier.getYear();

		if (year < 1900) {
			throw fatalError("Year for YOUNG stand should be at least 1900 but was {0,number,####}", year);
		}

		var bec = poly.getBiogeoclimaticZone();

		var primaryLayer = poly.getLayers().get(LayerType.PRIMARY);
		var primarySite = primaryLayer.getPrimarySite().orElseThrow();
		try {
			SiteIndexEquation siteCurve = getSiteCurveNumber(bec, primarySite);

			float primaryAgeTotal = primarySite.getAgeTotal().orElseThrow(); // AGETOT_L1
			float primaryYearsToBreastHeight = primarySite.getYearsToBreastHeight().orElseThrow(); // YTBH_L1

			float primaryBreastHeightAge0 = primaryAgeTotal - primaryYearsToBreastHeight; // AGEBH0

			float siteIndex = primarySite.getSiteIndex().orElseThrow(); // SID
			float yeastToBreastHeight = primaryYearsToBreastHeight; // YTBHD

			Map<String, Float> minimaMap = Utils.expectParsedControl(controlMap, ControlKey.MINIMA, Map.class);

			float minimumPredictedBaseArea = minimaMap.get(BaseControlParser.MINIMUM_PREDICTED_BASE_AREA); // VMINBAeqn
			float minimumHeight = minimaMap.get(BaseControlParser.MINIMUM_HEIGHT); // VMINH

			// Find an increase that puts stand into suitable condition with EMP106
			// predicting reasonable BA

			float baseAreaTarget = minimumPredictedBaseArea; // BATARGET
			float heightTarget = minimumHeight; // HTARGET
			float ageTarget = 5f; // AGETARGET

			// If PCTFLAND is very low, INCREASE the target BA, so as to avoid rounding
			// problems on output. But Target never increased by more than a factor of 4.
			// Before Jan 2008, this all started at PCTFLAND < 50.

			float percentAvailable = poly.getPercentAvailable().filter(x -> x >= 0f).orElse(85.0f); // PCT

			if (percentAvailable < 10f) {
				float factor = Math.min(10f / percentAvailable, 4f);
				baseAreaTarget *= factor;
			}

			float dominantHeight0 = 0f; // HD0

			int moreYears = Math.max(80, (int) (130 - primaryAgeTotal));

			float primaryHeight = primarySite.getHeight().orElseThrow(); // HT_L1

			final Increase inc = findIncreaseForYoungMode(
					bec, primaryLayer, siteCurve, primaryBreastHeightAge0, siteIndex, yeastToBreastHeight,
					baseAreaTarget, heightTarget, ageTarget, dominantHeight0, moreYears, primaryHeight
			);

			return VriPolygon.build(pBuilder -> {
				pBuilder.copy(poly);
				pBuilder.polygonIdentifier(polygonIdentifier.forYear(year + (int) inc.ageIncrease));
				pBuilder.mode(PolygonMode.YOUNG);
				pBuilder.copyLayers(poly, (lBuilder, layer) -> {

					lBuilder.copySpecies(layer, (sBuilder, species) -> {
						sBuilder.copySiteFrom(species, (iBuilder, site) -> {
							if (layer.getLayerType() == LayerType.PRIMARY
									&& primaryLayer.getPrimaryGenus().map(site.getSiteGenus()::equals).orElse(false)) {
								iBuilder.height(inc.dominantHeight);
							} else {
								iBuilder.height(Optional.empty());
							}

							site.getAgeTotal().map(x -> x + inc.ageIncrease).ifPresentOrElse(ageTotal -> {
								iBuilder.ageTotal(ageTotal);
								iBuilder.breastHeightAge(
										site.getYearsToBreastHeight()//
												.map(ytbh -> ageTotal - ytbh)
												.or(() -> site.getBreastHeightAge().map(bha -> bha + inc.ageIncrease))
								);
							}, () -> iBuilder
									.breastHeightAge(site.getBreastHeightAge().map(bha -> bha + inc.ageIncrease))
							);

						});
						lBuilder.ageIncrease(inc.ageIncrease());
					});
				});
			});

		} catch (RuntimeStandProcessingException e) {
			throw new FatalProcessingException(e);
		}
	}

	private Increase findIncreaseForYoungMode(
			BecDefinition bec, VriLayer primaryLayer, SiteIndexEquation siteCurve, float primaryBreastHeightAge0,
			float siteIndex, float yeastToBreastHeight, float baseAreaTarget, float heightTarget, float ageTarget,
			float dominantHeight0, int moreYears, float primaryHeight
	) throws FatalProcessingException, FailedToGrowYoungStandException {
		float dominantHeight;
		float ageIncrease;
		for (int increase = 0; increase <= moreYears; increase++) {
			float primaryBreastHeightAge = primaryBreastHeightAge0 + increase; // AGEBH

			if (primaryBreastHeightAge > 1f) {

				float ageD = primaryBreastHeightAge; // AGED

				float dominantHeightD;
				try {
					dominantHeightD = (float) SiteIndex2Height.indexToHeight(
							siteCurve, ageD, SiteIndexAgeType.SI_AT_BREAST, siteIndex, ageD, yeastToBreastHeight
					);
				} catch (CommonCalculatorException e) {
					throw new FatalProcessingException(e);
				} // HDD

				if (increase == 0) {
					dominantHeight0 = dominantHeightD;
				}
				dominantHeight = dominantHeightD; // HD
				if (primaryHeight > 0f && dominantHeight0 > 0f) {
					dominantHeight = primaryHeight + (dominantHeight - dominantHeight0);
				}

				// check empirical BA assuming BAV = 0

				float predictedBaseArea = estimateBaseAreaYield(
						dominantHeight, primaryBreastHeightAge, Optional.empty(), false,
						primaryLayer.getSpecies().values(), bec,
						primaryLayer.getEmpiricalRelationshipParameterIndex().orElseThrow()
				); // BAP

				// Calculate the full occupancy BA Hence the BA we will test is the Full
				// occupanct BA

				predictedBaseArea /= FRACTION_AVAILABLE_N;

				if (dominantHeight >= heightTarget && primaryBreastHeightAge >= ageTarget
						&& predictedBaseArea >= baseAreaTarget) {
					ageIncrease = increase;
					return new Increase(dominantHeight, ageIncrease);
				}
			}
		}
		throw new FailedToGrowYoungStandException();

	}

	static final <T, B extends ModelClassBuilder<T>> BiConsumer<B, T> noChange() {
		return (builder, toCopy) -> {
			/* Do Nothing */
		};
	}

	static final BiConsumer<VriSpecies.Builder, VriSpecies> noChangeSpecies() {
		return (builder, toCopy) -> {
			toCopy.getSite().ifPresent(site -> builder.copySite(site, noChange()));
		};
	}

	VriPolygon processBatc(VriPolygon poly) throws FatalProcessingException, PreprocessEstimatedBaseAreaLowException {

		try {
			VriLayer primaryLayer = getPrimaryLayer(poly);
			Optional<VriLayer> veteranLayer = getVeteranLayer(poly);
			var bec = poly.getBiogeoclimaticZone();

			//
			final float percentForestLand = poly.getPercentAvailable().orElseGet(() -> {
				try {
					return this.estimatePercentForestLand(poly, veteranLayer, primaryLayer);
				} catch (ProcessingException ex) {
					throw new RuntimeProcessingException(ex);
				}
			}); // PCTFLAND

			final float primaryBreastHeightAge = getLayerBreastHeightAge(primaryLayer).orElseThrow();

			// EMP040
			final float initialPrimaryBaseArea = this
					.estimatePrimaryBaseArea(primaryLayer, bec, poly.getYieldFactor(), primaryBreastHeightAge, 0.0f);

			final Optional<Float> veteranBaseArea = veteranLayer.map(VriLayer::getCrownClosure) // BAV
					.map(ccV -> ccV * initialPrimaryBaseArea / primaryLayer.getCrownClosure());

			final float primaryBaseArea = this.estimatePrimaryBaseArea(
					primaryLayer, bec, poly.getYieldFactor(), primaryBreastHeightAge, veteranBaseArea.orElse(0.0f) // BAP
			);

			final float primaryQuadMeanDiameter = this.estimatePrimaryQuadMeanDiameter(
					primaryLayer, bec, primaryBreastHeightAge, veteranBaseArea.orElse(0f)
			);

			return VriPolygon.build(pBuilder -> {
				pBuilder.copy(poly);

				pBuilder.addLayer(lBuilder -> {
					lBuilder.copy(primaryLayer);
					lBuilder.baseArea(primaryBaseArea * (percentForestLand / 100));
					lBuilder.treesPerHectare(treesPerHectare(primaryBaseArea, primaryQuadMeanDiameter));
					lBuilder.copySpecies(primaryLayer, noChangeSpecies());
				});
				veteranLayer.ifPresent(vLayer -> pBuilder.addLayer(lBuilder -> {
					lBuilder.copy(vLayer);
					lBuilder.baseArea(veteranBaseArea);

					lBuilder.copySpecies(primaryLayer, noChangeSpecies());
				}));

			});

		} catch (RuntimeProcessingException | LayerMissingException ex) {
			throw new FatalProcessingException(ex);
		} catch (BaseAreaLowException e) {
			throw new PreprocessEstimatedBaseAreaLowException(e);
		}
	}

	VriPolygon processBatn(VriPolygon poly) throws FatalProcessingException, PreprocessEstimatedBaseAreaLowException {

		final VriLayer primaryLayer = poly.getLayers().get(LayerType.PRIMARY);
		final VriSite primarySite = primaryLayer.getPrimarySite()
				.orElseThrow(() -> new FatalProcessingException("Primary layer does not have a primary site"));
		final Optional<VriLayer> veteranLayer = Utils.optSafe(poly.getLayers().get(LayerType.VETERAN));
		BecDefinition bec = poly.getBiogeoclimaticZone();

		final float primaryHeight = primarySite.getHeight()
				.orElseThrow(() -> new FatalProcessingException("Primary site does not have a height"));
		final float primaryBreastHeightAge = primarySite.getBreastHeightAge()
				.orElseThrow(() -> new FatalProcessingException("Primary site does not have a breast height age"));
		final Optional<Float> veteranBaseArea = veteranLayer.flatMap(VriLayer::getBaseArea);

		final int primaryEmpiricalRelationshipParameterIndex = primaryLayer.getEmpiricalRelationshipParameterIndex()
				.orElseThrow(
						() -> new FatalProcessingException(
								"Primary layer does not have an empirical relationship parameter index"
						)
				);

		float primaryBaseAreaEstimated = estimateBaseAreaYield(
				primaryHeight, primaryBreastHeightAge, veteranBaseArea, false, primaryLayer.getSpecies().values(), bec,
				primaryEmpiricalRelationshipParameterIndex
		);

		// EMP107
		float normativeQuadMeanDiameter = estimateQuadMeanDiameterYield(
				primaryHeight, primaryBreastHeightAge, veteranBaseArea, primaryLayer.getSpecies().values(), bec,
				primaryEmpiricalRelationshipParameterIndex
		);

		final float normativePercentAvailable = 85f;

		final float primaryBaseAreaFinal = primaryBaseAreaEstimated * (100 / normativePercentAvailable);

		final float primaryTreesPerHectare = treesPerHectare(primaryBaseAreaFinal, normativeQuadMeanDiameter);

		throwIfPresent(
				PreprocessEstimatedBaseAreaLowException
						.check(LayerType.PRIMARY, Optional.of(primaryBaseAreaFinal), 0.5f)
		);

		return VriPolygon.build(pBuilder -> {
			pBuilder.copy(poly);

			pBuilder.addLayer(lBuilder -> {
				lBuilder.copy(primaryLayer);
				lBuilder.baseArea(primaryBaseAreaFinal);
				lBuilder.treesPerHectare(primaryTreesPerHectare);
				lBuilder.copySpecies(primaryLayer, noChangeSpecies());
			});
			veteranLayer.ifPresent(vLayer -> pBuilder.addLayer(lBuilder -> {
				lBuilder.copy(vLayer);
				lBuilder.baseArea(veteranBaseArea);

				lBuilder.copySpecies(primaryLayer, noChangeSpecies());
			}));

		});

	}

	@Override
	protected ValueOrMarker<Float, Boolean>
			isVeteranForEstimatePercentForestLand(VriPolygon polygon, Optional<VriLayer> vetLayer) {
		return FLOAT_OR_BOOL.marker(vetLayer.isPresent());
	}

	/**
	 * Returns the siteCurveNumber for the first of the given ids that has one.
	 *
	 * @param region
	 * @param ids
	 * @return
	 * @throws StandProcessingException if no entry for any of the given species IDs is present.
	 * @throws FatalProcessingException
	 */
	SiteIndexEquation findSiteCurveNumber(Region region, String... ids) throws FatalProcessingException {
		var scnMap = Utils.<MatrixMap2<String, Region, SiteIndexEquation>>expectParsedControl(
				controlMap, ControlKey.SITE_CURVE_NUMBERS, MatrixMap2.class
		);

		for (String id : ids) {
			if (scnMap.hasM(id, region))
				return scnMap.get(id, region);
		}
		throw new FatalProcessingException(
				"Could not find Site Curve Number for inst of the following species: " + String.join(", ", ids)
		);
	}

	SiteIndexEquation getSiteCurveNumber(BecDefinition bec, VriSite primarySite) throws FatalProcessingException {
		try {
			return primarySite.getSiteCurveNumber() //
					.map(SiteIndexEquation::getByIndex)//
					.orElseGet(() -> {
						try {
							return this.findSiteCurveNumber(
									bec.getRegion(), primarySite.getSiteSpecies(), primarySite.getSiteGenus()
							);
						} catch (FatalProcessingException e) {
							throw new RuntimeProcessingException(e);
						}
					});
		} catch (RuntimeProcessingException ex) {
			throw new FatalProcessingException(ex);
		}
	}

	@Override
	protected Optional<VriSite> getPrimarySite(VriLayer layer) {
		return layer.getPrimarySite();
	}

	@Override
	protected float getYieldFactor(VriPolygon polygon) {
		return polygon.getYieldFactor();
	}

	float findRootForQuadMeanDiameterFractionalError(
			float min, float max, Map<String, Float> resultPerSpecies, Map<String, Float> initialDqs,
			Map<String, Float> baseAreas, Map<String, Float> minDq, Map<String, Float> maxDq, float tph
	) throws FatalProcessingException {

		// Note, this function has side effects in that it modifies resultPerSpecies. This is intentional, the goal is
		// to apply adjustment factor x to the values in initialDqs until the combination of their values has minimal
		// error then use those adjusted values.

		// Keeping track of the recent X values tied can be used to make some sort of guess if it doesn't converge.
		double[] lastXes = new double[2];
		double[] lastFs = new double[2];

		final double tol = 0.00001;

		UnivariateFunction errorFunc = x -> {
			lastXes[1] = lastXes[0];
			lastXes[0] = x;
			lastFs[1] = lastFs[0];
			lastFs[0] = this
					.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);
			return lastFs[0];
		};

		debugModeExpandRootSearchWindow(
				Optional.of(getDebugModes().getValue(9)).filter(x -> x > 0), minDq, maxDq, errorFunc
		);

		try {
			double x = doSolve(min, max, errorFunc);

			return (float) x;
		} catch (NoBracketingException ex) {

			// Decide if we want to propagate the exception or try to come up with something anyway.
			handleRootForQuadMeanDiameterFractionalErrorException(ex);

			// Try three values and take the least bad option.

			double x = bestOf(errorFunc, 0, -0.1, 0.1);

			// Invoke the function again to set the species map via
			errorFunc.value(x);

			return (float) x;

		} catch (TooManyEvaluationsException ex) {

			if (tol > 0.0 && Math.abs(lastFs[0]) < tol / 2) {

				if (Math.abs(lastFs[0]) < tol) {

					// Decide if we want to propagate the exception or try to use the last result.
					handleRootForQuadMeanDiameterFractionalErrorException(ex);

					return (float) lastXes[0];
				}
			}

			throw new FatalProcessingException(
					"Could not find solution for quadratic mean diameter.  There appears to be a discontinuity.", ex
			);

		}
	}

	void debugModeExpandRootSearchWindow(
			Optional<Integer> percentage, Map<String, Float> minDq, Map<String, Float> maxDq,
			UnivariateFunction errorFunc
	) {
		percentage.map(p -> (float) p / 100f).ifPresent(p -> {
			final double f1 = errorFunc.value(-10f);
			final double f2 = errorFunc.value(10f);
			final float base = 7.5f;
			if (f2 * f1 > 0d) {
				float lowFactor = 1.0f - p;
				float highFactor = 1.0f + p;
				for (var key : maxDq.keySet()) {
					minDq.put(key, base + lowFactor * (minDq.get(key) - base));
					maxDq.put(key, base + highFactor * (maxDq.get(key) - base));
				}
			}
		});
	}

	double doSolve(float min, float max, UnivariateFunction errorFunc) {
		var interval = new Interval(min, max);

		// I couldn't identify the method the original Fortran was using, so I just picked one and it worked
		// We could swap this for another like NewtonRaphsonSolver
		var solver = new BrentSolver();

		// The Fortran solver library, $ZERO, included an ability to search for a better interval if given one where
		// the function values at the end points have the same sign. This replicates that.
		interval = findInterval(new Interval(min, max), errorFunc);

		double x = solver.solve(100, errorFunc, interval.start(), interval.end(), interval.mid());
		return x;
	}

	/**
	 * Returns the x value for which func(x) is closest to 0.
	 *
	 * @param func
	 * @param values
	 * @return
	 */
	static double bestOf(UnivariateFunction func, double... values) {
		if (values.length <= 0) {
			throw new IllegalArgumentException("bestOf requires at least one point to compare");
		}
		double bestX = values[0];
		double bestY = func.value(bestX);
		for (int i = 1; i < values.length; i++) {
			double newX = values[i];
			double newY = func.value(newX);
			if (Math.abs(newY) < Math.abs(bestY)) {
				bestX = newX;
				bestY = newY;
			}
		}
		return bestX;
	}

	private void handleRootForQuadMeanDiameterFractionalErrorException(RuntimeException ex)
			throws FatalProcessingException {
		// Only do this in VRIStart

		if (getDebugModes().getValue(1) == 2) {
			throw new FatalProcessingException("Could not find solution for quadratic mean diameter", ex);
		}

		log.atWarn().setMessage("Could not find exact solution for quadratic mean diameter.  Using inexact estimate.")
				.setCause(ex);

	}

	public static record Interval(double start, double end) {
		double mid() {
			return (start() + end()) / 2;
		}

		double size() {
			return end() - start();
		}

		Interval evaluate(UnivariateFunction func) {
			return new Interval(func.value(start()), func.value(end()));
		}

	}

	/**
	 * This replicates the behavior of the SZERO root finding library used by VDYP7
	 *
	 * @param interval Initial interval of parameters to func
	 * @param func
	 * @return an interval for parameters to func
	 */
	public Interval findInterval(Interval intervalInit, UnivariateFunction func) {

		var interval = intervalInit;
		// Try 40 times before giving up.

		double currentX = interval.start();
		double lastX = interval.end();
		double lastF = func.value(lastX);
		double currentF = func.value(currentX);
		int i;
		for (i = 0; i < 40; i++) {

			if (currentF * lastF <= 0) {
				var newInterval = new Interval(Math.min(currentX, lastX), Math.max(currentX, lastX));
				log.atInfo().setMessage("Looking for root in range {}").addArgument(interval);
				return newInterval;
			}

			double tp = currentF / lastF;

			if (tp >= 1) {
				double temp = currentX;
				currentX = lastX;
				lastX = temp;
				temp = currentF;
				currentF = lastF;
				lastF = temp;
			}

			if (Math.abs(currentF) >= 8 * Math.abs(lastF - currentF)) {
				tp = 8;
			} else {
				tp = Math.max(0.25 * i, currentF / (lastF - currentF));
			}

			lastF = currentF;
			double oppositeX = lastX;
			lastX = currentX;
			if (currentX == oppositeX) {
				oppositeX = 1.03125 * currentX + (0.001 * Math.signum(currentX));
			}
			currentX += tp * (currentX - oppositeX);
			currentF = func.value(currentX);
		}

		throw new NoBracketingException(currentX, lastX, currentF, lastF);
	}

	@Override
	protected String getDefaultControlFileName() {
		return "vristart.ctr";
	}
}
