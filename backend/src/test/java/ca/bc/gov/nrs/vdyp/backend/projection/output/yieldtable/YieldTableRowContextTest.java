package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.PolygonProjectionState;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.ValidatedParameters;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

public class YieldTableRowContextTest {

	static class AgeYearCombo {
		private AgeYearCombo(Parameters.AgeYearRangeCombinationKind k, Integer as, Integer ae, Integer ys, Integer ye) {
			ageStart = as;
			ageEnd = ae;
			yearStart = ys;
			yearEnd = ye;
			kind = k;
		}

		static AgeYearCombo of(
				Parameters.AgeYearRangeCombinationKind kind, Integer ageStart, Integer ageEnd, Integer yearStart,
				Integer yearEnd
		) {
			return new AgeYearCombo(kind, ageStart, ageEnd, yearStart, yearEnd);
		}

		Parameters.AgeYearRangeCombinationKind kind;
		final Integer ageStart;
		final Integer ageEnd;
		final Integer yearStart;
		final Integer yearEnd;
	}

	static class AgeYearComboResults {
		private AgeYearComboResults(Integer ays, Integer aye, Integer ags, Integer age) {
			ageAtStartYear = ays;
			ageAtEndYear = aye;
			ageAtGapStart = ags;
			ageAtGapEnd = age;
		}

		static AgeYearComboResults
				of(Integer ageAtStartYear, Integer ageAtEndYear, Integer ageAtGapStart, Integer ageAtGapEnd) {
			return new AgeYearComboResults(ageAtStartYear, ageAtEndYear, ageAtGapStart, ageAtGapEnd);
		}

		final Integer ageAtStartYear;
		final Integer ageAtEndYear;
		final Integer ageAtGapStart;
		final Integer ageAtGapEnd;
	}

	static Stream<Arguments> ageYearCombinations() {
		return Stream.of(

				Arguments.of(AgeYearCombo.of(null, 0, 100, null, null), AgeYearComboResults.of(0, 100, null, null)),
				Arguments
						.of(AgeYearCombo.of(null, null, null, 2000, 2100), AgeYearComboResults.of(50, 150, null, null)),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.INTERSECT, 0, 100, 2000, 2100), //
						AgeYearComboResults.of(50, 100, null, null)
				),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.INTERSECT, 0, 40, 2000, 2100), //
						AgeYearComboResults.of(null, null, null, null)
				),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.UNION, 0, 100, 2000, 2100), //
						AgeYearComboResults.of(0, 150, null, null)
				),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.UNION, 0, 40, 2000, 2100), //
						AgeYearComboResults.of(0, 150, 40, 50)
				),

				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.DIFFERENCE, 50, 100, 1950, 2100), // Overlapping
						// Difference
						AgeYearComboResults.of(0, 150, 49, 101)
				),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.DIFFERENCE, 0, 100, 1950, 2050), // Overlapping
						// Difference
						AgeYearComboResults.of(null, null, null, null)
				),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.DIFFERENCE, 0, 150, 1975, 2050), // Overlapping
						// Difference
						AgeYearComboResults.of(0, 150, 24, 101)
				),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.DIFFERENCE, 0, 100, 2000, 2100), // Overlapping
						// Difference
						AgeYearComboResults.of(0, 150, 49, 101)
				),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.DIFFERENCE, 0, 40, 2000, 2100), // Disjoint
						// Difference
						AgeYearComboResults.of(0, 150, 40, 50)
				),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.DIFFERENCE, 75, 100, 1950, 2000), // Disjoint
						// Difference
						AgeYearComboResults.of(0, 100, 50, 75)
				),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.DIFFERENCE, 0, 150, 1950, 2000), // Disjoint
						// Difference
						AgeYearComboResults.of(51, 150, null, null)
				),
				Arguments.of(
						AgeYearCombo.of(Parameters.AgeYearRangeCombinationKind.DIFFERENCE, 0, 50, 1975, 2000), // Disjoint
						// Difference
						AgeYearComboResults.of(0, 24, null, null)
				)
		);
	}

	@ParameterizedTest
	@MethodSource("ageYearCombinations")
	void testAgeYearCombinations(AgeYearCombo ageYearParams, AgeYearComboResults results)
			throws AbstractProjectionRequestException {

		var params = new Parameters().ageStart(ageYearParams.ageStart).ageEnd(ageYearParams.ageEnd)
				.yearStart(ageYearParams.yearStart).yearEnd(ageYearParams.yearEnd);

		ValidatedParameters vp = new ValidatedParameters().ageStart(ageYearParams.ageStart).ageEnd(ageYearParams.ageEnd)
				.yearStart(ageYearParams.yearStart).yearEnd(ageYearParams.yearEnd);
		vp.setCombineAgeYearRange(ageYearParams.kind);

		var polygon = new Polygon.Builder().referenceYear(1950).build();
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false) {
			// Overriding because parameter validation does not allow for these other kinds of Age YEar Combos despite
			@Override
			public ValidatedParameters getParams() {
				return vp;
			}
		};
		var state = new PolygonProjectionState();

		var yieldTableRowContext = YieldTableRowContext.of(context, polygon, state, null);
		assertThat(yieldTableRowContext.getAgeAtStartYear(), is(results.ageAtStartYear));
		assertThat(yieldTableRowContext.getAgeAtEndYear(), is(results.ageAtEndYear));
		assertThat(yieldTableRowContext.getAgeAtGapStart(), is(results.ageAtGapStart));
		assertThat(yieldTableRowContext.getAgeAtGapEnd(), is(results.ageAtGapEnd));
	}
}
