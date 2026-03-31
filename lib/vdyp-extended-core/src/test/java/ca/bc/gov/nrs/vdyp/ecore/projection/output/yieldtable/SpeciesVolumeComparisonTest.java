package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.api.helpers.ResultYieldTable;

// Compares PRJ_SP{n}_VOL_* columns between a VDYP7 and a VDYP8 YieldTable CSV and reports any rows where the values differ by more than TOLERANCE_PCT percent.
class SpeciesVolumeComparisonTest {

	private static final Logger logger = LoggerFactory.getLogger(SpeciesVolumeComparisonTest.class);

	private static final String RESOURCE_BASE = "test-data/vdyp7-comparison/";
	private static final String RESOURCE_VDYP8 = RESOURCE_BASE + "vdyp8_Output_YldTbl.csv";
	private static final String RESOURCE_VDYP7 = RESOURCE_BASE + "vdyp7_Output_YldTbl.csv";

	/** Rows with a percentage difference greater than this value are reported. */
	private static final double TOLERANCE_PCT = 1.0;

	private static final String[] SPECIES_VOLUME_TYPES = { "WS", "CU", "D", "DW", "DWB" };
	private static final int MAX_SPECIES = 6;

	@Test
	void compareSpeciesVolumes_vdyp1026() {
		ResultYieldTable vdyp8Table = new ResultYieldTable(
				new InputStreamReader(getClass().getClassLoader().getResourceAsStream(RESOURCE_VDYP8))
		);
		ResultYieldTable vdyp7Table = new ResultYieldTable(
				new InputStreamReader(getClass().getClassLoader().getResourceAsStream(RESOURCE_VDYP7))
		);
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
			fail(
					mismatches.size() + " PRJ_SP*_VOL_* column(s) differ by more than " + TOLERANCE_PCT + "%:\n"
							+ String.join("\n", mismatches)
			);
		}
	}
}
