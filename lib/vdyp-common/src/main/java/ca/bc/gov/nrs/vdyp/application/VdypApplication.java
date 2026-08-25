package ca.bc.gov.nrs.vdyp.application;

import static ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter.quadMeanDiameter;
import static ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter.treesPerHectare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common.ComputationMethods;
import ca.bc.gov.nrs.vdyp.common.EstimationMethods;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationInitializationException;
import ca.bc.gov.nrs.vdyp.common.VdypApplicationProcessingException;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.exceptions.CouldNotFindBracketingIntervalException;
import ca.bc.gov.nrs.vdyp.exceptions.FatalProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.DebugSettings;
import ca.bc.gov.nrs.vdyp.model.NonFipDebugSettings;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;

/**
 * Base class of all VDYP applications.
 *
 * <p>
 * Expects <tt>application.properties</tt> to be on the class path.
 *
 * @author Michael Junkin, Vivid Solutions
 * @author Kevin Smith, Vivid Solutions
 */
public abstract class VdypApplication<D extends DebugSettings> extends VdypComponent implements AutoCloseable {

	public static final int SUCCESS = 0;
	public static final int CONFIG_LOAD_ERROR = 1;
	public static final int PROCESSING_ERROR = 2;
	public static final int OTHER_ERROR = -1;

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(VdypApplication.class);

	public abstract VdypApplicationIdentifier getId();

	/** The computation instance used by this engine */
	protected ComputationMethods computers;

	protected Map<String, Object> controlMap = new HashMap<>();
	protected FileResolver fileResolver;

	public EstimationMethods estimationMethods;

	private Optional<D> debugModes = Optional.empty();

	protected ResolvedControlMap resolvedControlMap;

	public D getDebugModes() {
		return debugModes.orElseThrow(() -> new IllegalStateException("Can not get debug modes before initialization"));
	}

	public Optional<NonFipDebugSettings> getNonFipDebugModes() {
		return Optional.of(getDebugModes()) //
				.filter(NonFipDebugSettings.class::isInstance) //
				.map(NonFipDebugSettings.class::cast);
	}

	public void setDebugModes(D newDebugModes) {
		debugModes = Optional.of(newDebugModes);
	}

	/**
	 * @returns the ordinal of the application's identifier. It will agree with the JPROGRAM values from the FORTRAN
	 *          implementation.
	 */
	public int getJProgramNumber() {
		return getId().getJProgramNumber();
	}

	public static List<String> getControlMapFileNames(
			final String[] args, final String defaultName, final VdypApplicationIdentifier appId,
			PrintStream writeToIfNoArgs, InputStream readFromIfNoArgs
	) throws IOException {
		List<String> controlFileNames;
		if (args.length == 0) {
			writeToIfNoArgs.printf(
					"Enter name of %s control file (or RETURN for %s) or *name for both): ", appId.toString(),
					defaultName
			);

			controlFileNames = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(readFromIfNoArgs))) {
				String userResponse = br.readLine().strip();
				if (userResponse.length() == 0 || userResponse.equals("*")) {
					controlFileNames.add(defaultName);
				} else if (userResponse.startsWith("*")) {
					controlFileNames.add(defaultName);

					userResponse = userResponse.substring(1);
					controlFileNames.addAll(Arrays.asList(userResponse.split("\s+")));
				} else {
					controlFileNames.addAll(Arrays.asList(userResponse.split("\s+")));
				}
			}
		} else {
			controlFileNames = Arrays.stream(args)
					.flatMap(arg -> arg.startsWith("*") ? Stream.of(defaultName, arg.substring(1)) : Stream.of(arg))
					.filter(s -> !s.isEmpty()).toList();
		}
		return controlFileNames;
	}

	protected abstract String getDefaultControlFileName();

	/**
	 * Initialize application
	 *
	 * @param resolver
	 * @param controlFilePath
	 * @throws IOException
	 * @throws ResourceParseException
	 */
	protected void init(
			FileSystemFileResolver resolver, PrintStream writeToIfNoArgs, InputStream readFromIfNoArgs,
			String... controlFilePaths
	) throws IOException, ResourceParseException {

		var controlFileNames = VdypApplication.getControlMapFileNames(
				controlFilePaths, getDefaultControlFileName(), getId(), writeToIfNoArgs, readFromIfNoArgs
		);

		init(resolver, getControlFileParser().parseByName(controlFileNames, resolver, new HashMap<>()));
	}

	protected abstract BaseControlParser<D> getControlFileParser();

	/**
	 * Initialize application
	 *
	 * @param controlMap
	 * @throws IOException
	 */
	void init(FileSystemFileResolver resolver, Map<String, Object> controlMap) throws IOException {
		this.fileResolver = resolver;
		setControlMap(controlMap);
	}

	public void doMain(final Path... args)
			throws VdypApplicationInitializationException, VdypApplicationProcessingException {
		doMain(Arrays.stream(args).map(Path::toAbsolutePath).map(Object::toString).toArray(String[]::new));
	}

	public void doMain(final String... args)
			throws VdypApplicationInitializationException, VdypApplicationProcessingException {
		var resolver = new FileSystemFileResolver();

		doMain(resolver, args);
	}

	public void doMain(FileSystemFileResolver resolver, final String... args)
			throws VdypApplicationInitializationException, VdypApplicationProcessingException {

		try {
			init(resolver, System.out, System.in, args);
		} catch (Exception ex) {
			logger.error("Error during initialization", ex);
			throw new VdypApplicationInitializationException(ex);
		}

		try {
			process();
		} catch (Exception ex) {
			logger.error("Error during processing", ex);
			throw new VdypApplicationProcessingException(ex);
		}
	}

	protected abstract void process() throws ProcessingException;

	protected abstract ResolvedControlMap resolveControlMap(Map<String, Object> rawControlMap);

	@SuppressWarnings("unchecked")
	protected void setControlMap(Map<String, Object> controlMap) {
		this.controlMap = controlMap;
		this.resolvedControlMap = resolveControlMap(controlMap);
		this.estimationMethods = new EstimationMethods(this.resolvedControlMap);
		this.debugModes = Optional.of((D) this.resolvedControlMap.getDebugSettings());
		this.computers = new ComputationMethods(estimationMethods, getId());
	}

	protected void logVersionInformation() {
		logger.info("{} {}", RESOURCE_SHORT_VERSION, RESOURCE_VERSION_DATE);
		logger.info("{} Ver:{} {}", RESOURCE_BINARY_NAME, RESOURCE_SHORT_VERSION, RESOURCE_VERSION_DATE);
		logger.info("VDYP7 Support Ver: {}", AVERSION);
	}

	protected static void runApp(Supplier<? extends VdypApplication<?>> getApp, String... args) {
		System.exit(doRunApp(getApp, args));
	}

	protected static int doRunApp(Supplier<? extends VdypApplication<?>> getApp, String... args) {
		try (var app = getApp.get();) {
			app.logVersionInformation();
			app.doMain(args);
		} catch (VdypApplicationInitializationException e) {
			return CONFIG_LOAD_ERROR;
		} catch (VdypApplicationProcessingException e) {
			return PROCESSING_ERROR;
		} catch (Throwable e) {
			// Error or a RuntimeException, or an exception while closing the app
			logger.atError().setCause(e).setMessage("Error");
			return OTHER_ERROR;
		}
		return SUCCESS;
	}

	// ROOTV01
	protected void getDqBySpecies(VdypLayer layer, Region region) throws FatalProcessingException {

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

		UnivariateFunction errorFunc = createQuadMeanDiameterFractionalErrorFunction(
				resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph, lastXes, lastFs
		);
		debugModeExpandRootSearchWindow(
				getNonFipDebugModes().flatMap(NonFipDebugSettings::getExpandDiameterForTPHRecovery), minDq, maxDq,
				errorFunc
		);
		// If the search window was expanded we need to rebox the min and max values in the univariate function
		errorFunc = createQuadMeanDiameterFractionalErrorFunction(
				resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph, lastXes, lastFs
		);
		try {
			double x = doSolve(min, max, errorFunc);

			return (float) x;
		} catch (CouldNotFindBracketingIntervalException ex) {
			double x;
			if (ex.isExitEarly()) {
				x = ex.getLo();
				// Note that in this case we don't call handleRootForQuadMeanDiameterFractionalErrorException to
				// potentially error out. This is how VDYP 7 works although I'm not sure why.
			} else {
				// Decide if we want to propagate the exception or try to come up with something anyway.
				handleRootForQuadMeanDiameterFractionalErrorException(ex);

				// Try three values and take the least bad option.

				x = bestOf(errorFunc, 0, -0.1, 0.1);
			}
			// Invoke the function again to set the species map via
			var error = errorFunc.value(x);
			logger.atWarn().setMessage(
					"Failed to reconcile total DQ/TPH for species with layer.  Using a distribution that has an error of {}."
			).addArgument(error).log();

			return (float) x;

		} catch (TooManyEvaluationsException ex) {

			if (tol > 0.0 && Math.abs(lastFs[0]) < tol / 2) {

				// Decide if we want to propagate the exception or try to use the last result.
				handleRootForQuadMeanDiameterFractionalErrorException(ex);

				return (float) lastXes[0];

			}

			throw new FatalProcessingException(
					"Could not find solution for quadratic mean diameter.  There appears to be a discontinuity.", ex
			);

		}
	}

	private UnivariateFunction createQuadMeanDiameterFractionalErrorFunction(
			Map<String, Float> resultPerSpecies, Map<String, Float> initialDqs, Map<String, Float> baseAreas,
			Map<String, Float> minDq, Map<String, Float> maxDq, float tph, double[] lastXes, double[] lastFs
	) {
		return x -> {
			lastXes[1] = lastXes[0];
			lastXes[0] = x;
			lastFs[1] = lastFs[0];
			lastFs[0] = this
					.quadMeanDiameterFractionalError(x, resultPerSpecies, initialDqs, baseAreas, minDq, maxDq, tph);
			return lastFs[0];
		};
	}

	void debugModeExpandRootSearchWindow(
			Optional<Float> recoveryFactor, Map<String, Float> minDq, Map<String, Float> maxDq,
			UnivariateFunction errorFunc
	) {
		recoveryFactor.ifPresent(p -> {
			final double f1 = errorFunc.value(-10f);
			final double f2 = errorFunc.value(10f);
			final float base = 7.5f;
			if (f2 * f1 > 0d) {
				float lowFactor = 1.0f - p;
				float highFactor = 1.0f + p;
				for (var entry : maxDq.entrySet()) {
					var key = entry.getKey();
					minDq.put(key, base + lowFactor * (minDq.get(key) - base));
					maxDq.put(key, base + highFactor * (entry.getValue() - base));
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
		interval = findInterval(interval, errorFunc, (i, x) -> i >= 2 && Math.abs(x) > 20);

		return solver.solve(100, errorFunc, interval.start(), interval.end(), interval.mid());

	}

	private void handleRootForQuadMeanDiameterFractionalErrorException(RuntimeException ex)
			throws FatalProcessingException {
		// Only do this in VRIStart

		if (getNonFipDebugModes().map(NonFipDebugSettings::getMode1ErrorsFatal).orElse(false)) {
			throw new FatalProcessingException("Could not find solution for quadratic mean diameter", ex);
		}

		logger.atWarn()
				.setMessage("Could not find exact solution for quadratic mean diameter.  Using inexact estimate.")
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
	public Interval findInterval(Interval intervalInit, UnivariateFunction func, BiPredicate<Integer, Double> breakIf) {

		var interval = intervalInit;
		// Try 40 times before giving up.

		double x1 = interval.start();
		double x2 = interval.end();
		double f1 = func.value(x1);
		double f2 = func.value(x2);

		double currentX = x1; // XX
		double currentF = f1; // FF
		// the "last" variables are set once the first time SZERO is called, then left uninitialized on subsequent
		// calls which has them preserve state over multiple iterations.
		double lastX = x2; // XL
		double lastF = f2; // FL
		int i;
		for (i = 0; i < 40; i++) {

			currentX = x1; // XX
			currentF = f1; // FF
			if (breakIf.test(i, x1)) {
				throw new CouldNotFindBracketingIntervalException(currentX, lastX, currentF, lastF, i, true);
			}

			if (currentF * lastF <= 0) {
				var newInterval = new Interval(Math.min(currentX, lastX), Math.max(currentX, lastX));
				logger.atInfo().setMessage("Looking for root in range {}").addArgument(interval);
				return newInterval;
			}

			double tp = currentF / lastF;

			if (tp > 1) {
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
				tp = Math.max(0.25 * (i + 1), currentF / (lastF - currentF));
			}

			lastF = currentF;
			double oppositeX = lastX; // XO
			lastX = currentX;
			if (currentX == oppositeX) {
				oppositeX = 1.03125 * currentX + (0.001 * Math.signum(currentX));
			}
			currentX += tp * (currentX - oppositeX);

			x1 = currentX;

			// In the original Fortran this would happen in the subroutine calling SZERO
			f1 = func.value(x1);
		}

		throw new CouldNotFindBracketingIntervalException(currentX, lastX, currentF, lastF, i - 1, false);
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

	protected abstract ComponentSizeLimits getLimitsForSpecies(VdypSpecies spec, Region region);
}
