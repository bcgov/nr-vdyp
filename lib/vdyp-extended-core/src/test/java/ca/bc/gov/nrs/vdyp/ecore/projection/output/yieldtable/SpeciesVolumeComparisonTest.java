package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.ResultYieldTable;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionRunner;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;

// Runs a VDYP8 projection and compares PRJ_SP{n}_VOL_* columns against a reference VDYP7 YieldTable CSV,
// reporting any rows where the values differ by more than TOLERANCE_PCT percent.
class SpeciesVolumeComparisonTest {

	private static final Logger logger = LoggerFactory.getLogger(SpeciesVolumeComparisonTest.class);

	// private static final String RESOURCE_BASE = "test-data/vdyp7-comparison/1poly-1st/";
	private static final String RESOURCE_BASE = "test-data/vdyp7-comparison/";
	private static final String RESOURCE_VDYP7 = RESOURCE_BASE + "vdyp7_Output_YldTbl.csv";
	private static final String RESOURCE_INPUT_POLY = RESOURCE_BASE + "projection_spec_vol_errorPoly.csv";
	private static final String RESOURCE_INPUT_LAYER = RESOURCE_BASE + "projection_spec_vol_errorLayer.csv";

	/** Rows with a percentage difference greater than this value are reported. */
	private static final double TOLERANCE_PCT = 1.0;

	private static final String[] SPECIES_VOLUME_TYPES = { "WS", "CU", "D", "DW", "DWB" };
	private static final int MAX_SPECIES = 6;

	@Test
	void compareSpeciesVolumes_vdyp1026() throws AbstractProjectionRequestException, Exception {
		ResultYieldTable vdyp7Table = new ResultYieldTable(
				new InputStreamReader(getClass().getClassLoader().getResourceAsStream(RESOURCE_VDYP7))
		);

		Parameters params = new Parameters().ageStart(0).ageEnd(250).ageIncrement(10)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTED_MOF_VOLUMES)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_SUMMARIZE_PROJECTION_BY_LAYER)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_SPECIES_PROJECTION)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_PROJECTION_MODE_IN_YIELD_TABLE)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_INCLUDE_POLYGON_RECORD_ID_IN_YIELD_TABLE)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.FORWARD_GROW_ENABLED);

		ProjectionRunner runner = new ProjectionRunner(ProjectionRequestKind.HCSV, "VDYP1026", params, false);

		runner.run(
				Map.of(
						ParameterNames.HCSV_POLYGON_INPUT_DATA,
						getClass().getClassLoader().getResourceAsStream(RESOURCE_INPUT_POLY),
						ParameterNames.HCSV_LAYERS_INPUT_DATA,
						getClass().getClassLoader().getResourceAsStream(RESOURCE_INPUT_LAYER)
				)
		);

		String vdyp8Csv = new String(runner.getContext().getYieldTables().get(0).getAsStream().readAllBytes());
		ResultYieldTable vdyp8Table = new ResultYieldTable(new StringReader(vdyp8Csv));

		assertSpeciesVolumesMatch(vdyp7Table, vdyp8Table);
	}

	private void assertSpeciesVolumesMatch(ResultYieldTable vdyp7Table, ResultYieldTable vdyp8Table) {

		List<String> mismatches = new ArrayList<>();

		for (Map.Entry<String, Map<String, Map<String, Map<String, String>>>> featureEntry : vdyp7Table.entrySet()) {
			String featureId = featureEntry.getKey();

			if (!vdyp8Table.containsKey(featureId)) {
				mismatches.add("VDYP8 is missing featureId: " + featureId);
				continue;
			}

			for (Map.Entry<String, Map<String, Map<String, String>>> layerEntry : featureEntry.getValue().entrySet()) {
				String layerId = layerEntry.getKey();

				if (!vdyp8Table.get(featureId).containsKey(layerId)) {
					mismatches.add("VDYP8 is missing layerId " + layerId + " for featureId " + featureId);
					continue;
				}

				for (Map.Entry<String, Map<String, String>> yearEntry : layerEntry.getValue().entrySet()) {
					String year = yearEntry.getKey();
					Map<String, String> vdyp7Row = yearEntry.getValue();

					Map<String, String> vdyp8Row = vdyp8Table.get(featureId).get(layerId).get(year);
					if (vdyp8Row == null) {
						mismatches.add(
								String.format(
										"VDYP8 is missing year %s for featureId %s layerId %s", year, featureId, layerId
								)
						);
						continue;
					}

					for (int sp = 1; sp <= MAX_SPECIES; sp++) {
						for (String type : SPECIES_VOLUME_TYPES) {
							String col = "PRJ_SP" + sp + "_VOL_" + type;

							String v7str = vdyp7Row.get(col);
							String v8str = vdyp8Row.get(col);

							if (v7str == null || v7str.isBlank() || v8str == null || v8str.isBlank()) {
								continue;
							}

							try {
								double v7 = Double.parseDouble(v7str.trim());
								double v8 = Double.parseDouble(v8str.trim());

								if (v7 == 0.0 && v8 == 0.0) {
									continue;
								}

								double base = v7 != 0.0 ? Math.abs(v7) : Math.abs(v8);
								double diffPct = Math.abs(v7 - v8) / base * 100.0;

								if (diffPct > TOLERANCE_PCT) {
									mismatches.add(
											String.format(
													"[featureId=%s layerId=%s year=%s] %s: vdyp7=%.5f vdyp8=%.5f diff=%.2f%%",
													featureId, layerId, year, col, v7, v8, diffPct
											)
									);
								}
							} catch (NumberFormatException e) {
								mismatches.add(
										String.format(
												"[featureId=%s layerId=%s year=%s] %s: cannot parse values vdyp7='%s' vdyp8='%s'",
												featureId, layerId, year, col, v7str, v8str
										)
								);
							}
						}
					}
				}
			}
		}

		if (mismatches.isEmpty()) {
			logger.info("All PRJ_SP*_VOL_* columns match within {}%", TOLERANCE_PCT);
		} else {
			logger.warn("{} mismatch(es) found:", mismatches.size());
			mismatches.forEach(logger::warn);
		}
	}
}
