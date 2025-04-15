package ca.bc.gov.nrs.vdyp.exceptions;

import static ca.bc.gov.nrs.vdyp.test.TestUtils.assumeThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.array;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;

class ExceptionsTest {

	static Collection<StandProcessingException> exampleStandProcessingExceptions() {
		var result = new LinkedList<StandProcessingException>();
		result.add(new BaseAreaLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)));
		result.add(new BecMissingException());
		result.add(new BreastHeightAgeLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)));
		result.add(new CrownClosureLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)));
		result.add(new FailedToGrowYoungStandException());
		result.add(new HeightLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)));
		result.add(new IncorrectLayerCodesException("X"));
		result.add(new LayerMissingException(LayerType.PRIMARY));
		result.add(new LayerSpeciesDoNotSumTo100PercentException(LayerType.PRIMARY));
		result.add(
				new PreprocessEstimatedBaseAreaLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f))
		);
		result.add(new QuadraticMeanDiameterLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)));
		result.add(new ResultBaseAreaLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)));
		result.add(new SiteIndexLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)));
		result.add(new TotalAgeLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)));
		result.add(new TreesPerHectareLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)));
		result.add(new UnsupportedModeException(Optional.of(PolygonMode.BATC)));
		result.add(new UnsupportedSpeciesException("X"));
		result.add(new YearsToBreastHeightLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)));
		result.add(
				new LayerMissingValuesRequiredForMode(
						LayerType.PRIMARY, Optional.of(PolygonMode.START), List.of("test")
				)
		);
		return result;
	}

	@ParameterizedTest
	@MethodSource("exampleStandProcessingExceptions")
	void testUnwrapRuntimeExceptionMessage(StandProcessingException ex) throws Exception {
		var wrapper = new RuntimeStandProcessingException(ex);
		var unwrapped = ex.getClass().getConstructor(RuntimeStandProcessingException.class).newInstance(wrapper);

		assertThat(unwrapped, hasProperty("message", equalTo(ex.getMessage())));
	}

	@ParameterizedTest
	@MethodSource("exampleStandProcessingExceptions")
	void testUnwrapRuntimeExceptionLayer(StandProcessingException ex) throws Exception {
		assumeThat(ex, isA(LayerValidationException.class));
		var wrapper = new RuntimeStandProcessingException(ex);
		var unwrapped = ex.getClass().getConstructor(RuntimeStandProcessingException.class).newInstance(wrapper);

		final var cast = (LayerValidationException) ex;
		assertThat(unwrapped, hasProperty("layer", equalTo(cast.getLayer())));
	}

	@ParameterizedTest
	@MethodSource("exampleStandProcessingExceptions")
	void testUnwrapRuntimeExceptionValue(StandProcessingException ex) throws Exception {
		assumeThat(ex, isA(LayerValueLowException.class));
		var wrapper = new RuntimeStandProcessingException(ex);
		var unwrapped = ex.getClass().getConstructor(RuntimeStandProcessingException.class).newInstance(wrapper);

		final var cast = (LayerValueLowException) ex;
		assertThat(unwrapped, hasProperty("value", equalTo(cast.getValue())));
	}

	@ParameterizedTest
	@MethodSource("exampleStandProcessingExceptions")
	void testUnwrapRuntimeExceptionThreshold(StandProcessingException ex) throws Exception {
		assumeThat(ex, isA(LayerValueLowException.class));
		var wrapper = new RuntimeStandProcessingException(ex);
		var unwrapped = ex.getClass().getConstructor(RuntimeStandProcessingException.class).newInstance(wrapper);

		final var cast = (LayerValueLowException) ex;
		assertThat(unwrapped, hasProperty("threshold", equalTo(cast.getThreshold())));
	}

	@ParameterizedTest
	@MethodSource("exampleStandProcessingExceptions")
	void testUnwrapRuntimeExceptionIpass(StandProcessingException ex) throws Exception {
		var wrapper = new RuntimeStandProcessingException(ex);
		var unwrapped = ex.getClass().getConstructor(RuntimeStandProcessingException.class).newInstance(wrapper);

		for (var app : VdypApplicationIdentifier.values()) {
			assertThat(unwrapped.getIpassCode(app), equalTo(ex.getIpassCode(app)));
		}
	}

	@Test
	void testUnwrapMismatch() {
		var ex = new BecMissingException();
		var wrapper = new RuntimeStandProcessingException(ex);
		var thrown = assertThrows(IllegalArgumentException.class, () -> new BaseAreaLowException(wrapper));

		assertThat(
				thrown,
				hasProperty(
						"message",
						equalTo(
								"Could not unwrap RuntimeStandProcessingException to "
										+ BaseAreaLowException.class.getCanonicalName()
						)
				)
		);
		assertThat(thrown, hasProperty("suppressed", array(sameInstance(wrapper))));
	}

}
