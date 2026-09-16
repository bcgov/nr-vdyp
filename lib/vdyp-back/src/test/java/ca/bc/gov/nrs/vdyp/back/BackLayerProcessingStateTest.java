package ca.bc.gov.nrs.vdyp.back;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.closeTo;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.mmHasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ca.bc.gov.nrs.vdyp.back.processing_state.BackProcessingState;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ProcessingControlParser;
import ca.bc.gov.nrs.vdyp.model.CompatibilityVariables;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class BackLayerProcessingStateTest {

	@SuppressWarnings("unchecked")
	@ParameterizedTest
	@ValueSource(floats = { 0.0f, 0.25f, 0.5f, 0.75f, 1.0f })
	void testFractionalCompatibilityVariables(float fraction) throws ProcessingException {
		Map<String, Object> rawControlMap = TestUtils
				.loadControlMap(new ProcessingControlParser(), Path.of("VDYP.CTR"));

		var polygon = VdypPolygon.build(pb -> {
			pb.controlMap(rawControlMap);
			pb.polygonIdentifier("092P037  72999905UNK 2011");
			pb.biogeoclimaticZone("MS");
			pb.forestInventoryZone("");
			pb.percentAvailable(61f);
			pb.addLayer(lb -> {
				lb.layerType(LayerType.PRIMARY);
				lb.addSpecies(sb -> {
					sb.speciesIndex(12);
					sb.percentGenus(100);
					sb.addSite(ib -> {
						ib.siteCurveNumber(45);
						ib.yearsAtBreastHeight(77.3f);
						ib.yearsToBreastHeight(8.2f);
						ib.ageTotal(85f);
						ib.siteIndex(12.39f);
						ib.height(16f);
					});
				});
				lb.primaryGenus("PL");
				lb.empiricalRelationshipParameterIndex(118);
			});
		});

		BackProcessingState state = new BackProcessingState(rawControlMap);
		state.setPolygon(polygon);
		var layerState = state.getPrimaryLayerProcessingState();

		final CompatibilityVariables[] cvs;
		final CompatibilityVariables[] backCvs;

		{
			// The values here should all be replaced
			MatrixMap2<UtilizationClass, VolumeVariable, Float>[] cvVolume;
			Map<UtilizationClass, Float>[] cvBa;
			Map<UtilizationClass, Float>[] cvDq;
			Map<UtilizationClassVariable, Float>[] cvSm;
			cvVolume = new MatrixMap2[] { null,
					new MatrixMap2Impl<UtilizationClass, VolumeVariable, Float>(
							List.of(UtilizationClass.values()), List.of(VolumeVariable.values()), (uc, vv) -> 3f
					) };

			cvBa = new Map[] { null, new EnumMap<UtilizationClass, Float>(UtilizationClass.class) };
			for (var uc : UtilizationClass.values()) {
				cvBa[1].put(uc, 3f);
			}

			cvDq = new Map[] { null, new EnumMap<UtilizationClass, Float>(UtilizationClass.class) };
			for (var uc : UtilizationClass.values()) {
				cvDq[1].put(uc, 3f);
			}

			cvSm = new EnumMap[] { null, new EnumMap<UtilizationClassVariable, Float>(UtilizationClassVariable.class) };

			for (var uc : UtilizationClassVariable.values()) {
				cvSm[1].put(uc, 3f);
			}
			cvs = CompatibilityVariables.fromArrays(cvVolume, cvBa, cvDq, cvSm);
			layerState.setCompatibilityVariableDetails(cvs);
		}
		{
			MatrixMap2<UtilizationClass, VolumeVariable, Float>[] cvVolume;
			Map<UtilizationClass, Float>[] cvBa;
			Map<UtilizationClass, Float>[] cvDq;
			Map<UtilizationClassVariable, Float>[] cvSm;
			cvVolume = new MatrixMap2[] { null,
					new MatrixMap2Impl<UtilizationClass, VolumeVariable, Float>(
							List.of(UtilizationClass.values()), List.of(VolumeVariable.values()),
							(uc, vv) -> 11f + vv.ordinal() * 2f + uc.ordinal() * 3f
					) };

			cvBa = new Map[] { null, new EnumMap<UtilizationClass, Float>(UtilizationClass.class) };
			for (var uc : UtilizationClass.values()) {
				cvBa[1].put(uc, 13f + uc.ordinal() * 3f);
			}

			cvDq = new Map[] { null, new EnumMap<UtilizationClass, Float>(UtilizationClass.class) };
			for (var uc : UtilizationClass.values()) {
				cvDq[1].put(uc, 17f + uc.ordinal() * 3f);
			}

			cvSm = new EnumMap[] { null, new EnumMap<UtilizationClassVariable, Float>(UtilizationClassVariable.class) };

			for (var uc : UtilizationClassVariable.values()) {
				cvSm[1].put(uc, 1 + uc.ordinal() * 7f);
			}
			backCvs = CompatibilityVariables.fromArrays(cvVolume, cvBa, cvDq, cvSm);
			layerState.setBackCompatibilityVariables(backCvs);
		}

		layerState.setFractionalCompatibilityVariables(fraction);

		assertThat(
				"basalArea", cvs[1].basalArea(),
				allOf(
						hasEntry(is(UtilizationClass.U75TO125), closeTo(19f * fraction)),
						hasEntry(is(UtilizationClass.U125TO175), closeTo(22f * fraction)),
						hasEntry(is(UtilizationClass.U175TO225), closeTo(25f * fraction)),
						hasEntry(is(UtilizationClass.OVER225), closeTo(28f * fraction))
				)
		);
		assertThat(
				"quadraticMeanDiameter", cvs[1].quadraticMeanDiameter(),
				allOf(
						hasEntry(is(UtilizationClass.U75TO125), closeTo(23f * fraction)),
						hasEntry(is(UtilizationClass.U125TO175), closeTo(26f * fraction)),
						hasEntry(is(UtilizationClass.U175TO225), closeTo(29f * fraction)),
						hasEntry(is(UtilizationClass.OVER225), closeTo(32f * fraction))
				)
		);
		assertThat(
				"volume WS", cvs[1].volume(),
				allOf(
						mmHasEntry(closeTo(17f * fraction), UtilizationClass.U75TO125, VolumeVariable.WHOLE_STEM_VOL),
						mmHasEntry(closeTo(20f * fraction), UtilizationClass.U125TO175, VolumeVariable.WHOLE_STEM_VOL),
						mmHasEntry(closeTo(23f * fraction), UtilizationClass.U175TO225, VolumeVariable.WHOLE_STEM_VOL),
						mmHasEntry(closeTo(26f * fraction), UtilizationClass.OVER225, VolumeVariable.WHOLE_STEM_VOL)
				)
		);
		assertThat(
				"volume WS", cvs[1].volume(),
				allOf(
						mmHasEntry(closeTo(19f * fraction), UtilizationClass.U75TO125, VolumeVariable.CLOSE_UTIL_VOL),
						mmHasEntry(closeTo(22f * fraction), UtilizationClass.U125TO175, VolumeVariable.CLOSE_UTIL_VOL),
						mmHasEntry(closeTo(25f * fraction), UtilizationClass.U175TO225, VolumeVariable.CLOSE_UTIL_VOL),
						mmHasEntry(closeTo(28f * fraction), UtilizationClass.OVER225, VolumeVariable.CLOSE_UTIL_VOL)
				)
		);
		assertThat(
				"volume WS", cvs[1].volume(),
				allOf(
						mmHasEntry(
								closeTo(21f * fraction), UtilizationClass.U75TO125,
								VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY
						),
						mmHasEntry(
								closeTo(24f * fraction), UtilizationClass.U125TO175,
								VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY
						),
						mmHasEntry(
								closeTo(27f * fraction), UtilizationClass.U175TO225,
								VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY
						),
						mmHasEntry(
								closeTo(30f * fraction), UtilizationClass.OVER225,
								VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY
						)
				)
		);
		assertThat(
				"volume WS", cvs[1].volume(),
				allOf(
						mmHasEntry(
								closeTo(23f * fraction), UtilizationClass.U75TO125,
								VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE
						),
						mmHasEntry(
								closeTo(26f * fraction), UtilizationClass.U125TO175,
								VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE
						),
						mmHasEntry(
								closeTo(29f * fraction), UtilizationClass.U175TO225,
								VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE
						),
						mmHasEntry(
								closeTo(32f * fraction), UtilizationClass.OVER225,
								VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE
						)
				)
		);
		assertThat(
				"primaryLayerSmall", cvs[1].primaryLayerSmall(),
				allOf(
						hasEntry(is(UtilizationClassVariable.BASAL_AREA), closeTo(1f * fraction)),
						hasEntry(is(UtilizationClassVariable.LOREY_HEIGHT), closeTo(15f * fraction)),
						hasEntry(is(UtilizationClassVariable.QUAD_MEAN_DIAMETER), closeTo(8f * fraction)),
						hasEntry(is(UtilizationClassVariable.WHOLE_STEM_VOLUME), closeTo(22f * fraction))
				)
		);
	}
}
