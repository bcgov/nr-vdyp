package ca.bc.gov.nrs.vdyp.exceptions;

import java.util.Map;
import java.util.function.Function;

import ca.bc.gov.nrs.vdyp.common.Utils;

public class RuntimeProcessingException extends RuntimeException {

	private static final long serialVersionUID = 3236293487579672502L;

	public RuntimeProcessingException(ProcessingException causedBy) {
		super(causedBy);
	}

	@Override
	public synchronized ProcessingException getCause() {
		return (ProcessingException) super.getCause();
	}

	static final private Map<Class<? extends ProcessingException>, Function<RuntimeProcessingException, ? extends ProcessingException>> STAND_EXCEPTION_UNWRAPPERS = Utils
			.constMap(map -> {
				map.put(BaseAreaLowException.class, BaseAreaLowException::new);
				map.put(BecMissingException.class, BecMissingException::new);
				map.put(BreastHeightAgeLowException.class, BreastHeightAgeLowException::new);
				map.put(CrownClosureLowException.class, CrownClosureLowException::new);
				map.put(FailedToGrowYoungStandException.class, FailedToGrowYoungStandException::new);
				map.put(HeightLowException.class, HeightLowException::new);
				map.put(IncorrectLayerCodesException.class, IncorrectLayerCodesException::new);
				map.put(LayerMissingException.class, LayerMissingException::new);
				map.put(
						LayerSpeciesDoNotSumTo100PercentException.class, LayerSpeciesDoNotSumTo100PercentException::new
				);
				map.put(PreprocessEstimatedBaseAreaLowException.class, PreprocessEstimatedBaseAreaLowException::new);
				map.put(QuadraticMeanDiameterLowException.class, QuadraticMeanDiameterLowException::new);
				map.put(ResultBaseAreaLowException.class, ResultBaseAreaLowException::new);
				map.put(SiteIndexLowException.class, SiteIndexLowException::new);
				map.put(TotalAgeLowException.class, TotalAgeLowException::new);
				map.put(TreesPerHectareLowException.class, TreesPerHectareLowException::new);
				map.put(UnsupportedModeException.class, UnsupportedModeException::new);
				map.put(UnsupportedSpeciesException.class, UnsupportedSpeciesException::new);
				map.put(YearsToBreastHeightLowException.class, YearsToBreastHeightLowException::new);
				map.put(LayerMissingValuesRequiredForMode.class, LayerMissingValuesRequiredForMode::new);
			});

	public ProcessingException unwrap() {
		final ProcessingException cause = getCause();
		var klazz = cause.getClass();

		return STAND_EXCEPTION_UNWRAPPERS.getOrDefault(klazz, FatalProcessingException::new).apply(this);
	}
}
