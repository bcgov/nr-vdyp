package ca.bc.gov.nrs.vdyp.common;

import static ca.bc.gov.nrs.vdyp.math.FloatMath.abs;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.sqrt;

import java.text.MessageFormat;
import java.util.List;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;

public class ReconcilationMethods {

	private static final List<UtilizationClass> MODE_1_RECONCILE_AVAILABILITY_CLASSES = List
			.of(UtilizationClass.OVER225, UtilizationClass.U175TO225, UtilizationClass.U125TO175);

	private ReconcilationMethods() {
	}

	/**
	 * YUC1R. Implements the three reconciliation modes for layer 1 as described in ipsjf120.doc
	 *
	 * @param baseAreaUtil
	 * @param treesPerHectareUtil
	 * @param quadMeanDiameterUtil
	 * @throws ProcessingException
	 */
	public static void reconcileComponents(
			UtilizationVector baseAreaUtil, UtilizationVector treesPerHectareUtil,
			UtilizationVector quadMeanDiameterUtil
	) throws ProcessingException {
		if (baseAreaUtil.getAll() == 0f) {
			UtilizationClass.UTIL_CLASSES.forEach(uc -> {
				treesPerHectareUtil.setCoe(uc.index, 0f);
				baseAreaUtil.setCoe(uc.index, 0f);
			});
			return;
		}

		@SuppressWarnings("unused")
		float tphSum = 0f;
		float baSum = 0f;
		for (var uc : UtilizationClass.UTIL_CLASSES) {
			tphSum += treesPerHectareUtil.getCoe(uc.index);
			baSum += baseAreaUtil.getCoe(uc.index);
		}

		if (abs(baSum - baseAreaUtil.getAll()) > 0.00003 * baSum) {
			throw new ProcessingException(
					MessageFormat.format(
							"Computed base areas for {}+ components do not sum to expected total",
							UtilizationClass.U75TO125.lowBound
					)
			);
		}

		float dq0 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseAreaUtil.getAll(), treesPerHectareUtil.getAll());

		if (dq0 < UtilizationClass.U75TO125.lowBound) {
			throw new ProcessingException(
					MessageFormat.format(
							"Quadratic mean diameter computed from total"
									+ " base area and trees per hectare is less than {0} cm",
							UtilizationClass.U75TO125.lowBound
					)
			);
		}

		float tphSumHigh = (float) UtilizationClass.UTIL_CLASSES.stream()
				.mapToDouble(
						uc -> BaseAreaTreeDensityDiameter.treesPerHectare(baseAreaUtil.getCoe(uc.index), uc.lowBound)
				).sum();

		if (tphSumHigh < treesPerHectareUtil.getAll()) {
			reconcileComponentsMode1(baseAreaUtil, treesPerHectareUtil, quadMeanDiameterUtil, tphSumHigh);
		} else {
			reconcileComponentsMode2Check(baseAreaUtil, treesPerHectareUtil, quadMeanDiameterUtil);
		}
	}

	public static void reconcileComponentsMode1(
			UtilizationVector baseAreaUtil, UtilizationVector treesPerHectareUtil,
			UtilizationVector quadMeanDiameterUtil, float tphSumHigh
	) {
		// MODE 1

		// the high sum of TPH's is too low. Need MODE 1 reconciliation MUST set DQU's
		// to lowest allowable values AND must move BA from upper classes to lower
		// classes.

		float tphNeed = treesPerHectareUtil.getAll() - tphSumHigh;

		UtilizationClass.UTIL_CLASSES.forEach(uc -> quadMeanDiameterUtil.setCoe(uc.index, uc.lowBound));

		for (var uc : MODE_1_RECONCILE_AVAILABILITY_CLASSES) {
			float tphAvail = BaseAreaTreeDensityDiameter
					.treesPerHectare(baseAreaUtil.getCoe(uc.index), uc.previous().get().lowBound)
					- BaseAreaTreeDensityDiameter.treesPerHectare(baseAreaUtil.getCoe(uc.index), uc.lowBound);

			if (tphAvail < tphNeed) {
				baseAreaUtil.scalarInPlace(uc.previous().get().index, x -> x + baseAreaUtil.getCoe(uc.index));
				baseAreaUtil.setCoe(uc.index, 0f);
				tphNeed -= tphAvail;
			} else {
				float baseAreaMove = baseAreaUtil.getCoe(uc.index) * tphNeed / tphAvail;
				baseAreaUtil.scalarInPlace(uc.previous().get().index, x -> x + baseAreaMove);
				baseAreaUtil.scalarInPlace(uc.index, x -> x - baseAreaMove);
				break;
			}
		}
		UtilizationClass.UTIL_CLASSES.forEach(
				uc -> treesPerHectareUtil.setCoe(
						uc.index,
						BaseAreaTreeDensityDiameter
								.treesPerHectare(baseAreaUtil.getCoe(uc.index), quadMeanDiameterUtil.getCoe(uc.index))
				)
		);
	}

	public static void reconcileComponentsMode2Check(
			UtilizationVector baseAreaUtil, UtilizationVector treesPerHectareUtil,
			UtilizationVector quadMeanDiameterUtil
	) throws ProcessingException {
		// Before entering mode 2, check to see if reconciliation is already adequate

		float tphSum = (float) UtilizationClass.UTIL_CLASSES.stream()
				.mapToDouble(uc -> treesPerHectareUtil.getCoe(uc.index)).sum();

		if (abs(tphSum - treesPerHectareUtil.getAll()) / tphSum > 0.00001) {
			reconcileComponentsMode2(baseAreaUtil, treesPerHectareUtil, quadMeanDiameterUtil);
			return;
		}
		for (var uc : UtilizationClass.UTIL_CLASSES) {
			if (baseAreaUtil.getCoe(uc.index) > 0f) {
				if (treesPerHectareUtil.getCoe(uc.index) <= 0f) {
					reconcileComponentsMode2(baseAreaUtil, treesPerHectareUtil, quadMeanDiameterUtil);
					return;
				}
				float dWant = BaseAreaTreeDensityDiameter
						.quadMeanDiameter(baseAreaUtil.getCoe(uc.index), treesPerHectareUtil.getCoe(uc.index));
				float dqI = quadMeanDiameterUtil.getCoe(uc.index);
				if (dqI >= uc.lowBound && dqI <= uc.highBound && abs(dWant - dqI) < 0.00001) {
					return;
				}
			}
		}
	}

	public static void reconcileComponentsMode2(
			UtilizationVector baseAreaUtil, UtilizationVector treesPerHectareUtil,
			UtilizationVector quadMeanDiameterUtil
	) throws ProcessingException {
		int n = 0;
		float baseAreaFixed = 0f;
		float treesPerHectareFixed = 0f;
		var quadMeanDiameterLimit = new boolean[] { false, false, false, false, false };
		UtilizationVector dqTrial = Utils.utilizationVector();

		while (true) {
			n++;

			if (n > 4) {
				throw new ProcessingException("Mode 2 component reconciliation iterations exceeded 4");
			}

			float sum = (float) UtilizationClass.UTIL_CLASSES.stream().mapToDouble(uc -> {
				float baI = baseAreaUtil.getCoe(uc.index);
				float dqI = quadMeanDiameterUtil.getCoe(uc.index);
				if (baI != 0 && !quadMeanDiameterLimit[uc.index]) {
					return baI / (dqI * dqI);
				}
				return 0;
			}).sum();

			float baAll = baseAreaUtil.getAll() - baseAreaFixed;
			float tphAll = treesPerHectareUtil.getAll() - treesPerHectareFixed;

			if (baAll <= 0f || tphAll <= 0f) {
				reconcileComponentsMode3(baseAreaUtil, treesPerHectareUtil, quadMeanDiameterUtil);
				return;
			}

			float dqAll = BaseAreaTreeDensityDiameter.quadMeanDiameter(baAll, tphAll);

			float k = dqAll * dqAll / baAll * sum;
			float sqrtK = sqrt(k);

			for (var uc : UtilizationClass.UTIL_CLASSES) {
				if (!quadMeanDiameterLimit[uc.index] && baseAreaUtil.getCoe(uc.index) > 0f) {
					dqTrial.setCoe(uc.index, quadMeanDiameterUtil.getCoe(uc.index) * sqrtK);
				}
			}

			UtilizationClass violateClass = null;
			float violate = 0f;
			boolean violateLow = false;

			for (var uc : UtilizationClass.UTIL_CLASSES) {
				if (baseAreaUtil.getCoe(uc.index) > 0f && dqTrial.getCoe(uc.index) < uc.lowBound) {
					float vi = 1f - dqTrial.getCoe(uc.index) / uc.lowBound;
					if (vi > violate) {
						violate = vi;
						violateClass = uc;
						violateLow = true;

					}
				}
				if (dqTrial.getCoe(uc.index) > uc.highBound) {
					float vi = dqTrial.getCoe(uc.index) / uc.highBound - 1f;
					if (vi > violate) {
						violate = vi;
						violateClass = uc;
						violateLow = false;
					}
				}
			}
			if (violateClass == null)
				break;
			// Move the worst offending DQ to its limit
			dqTrial.setCoe(violateClass.index, violateLow ? violateClass.lowBound : violateClass.highBound);

			quadMeanDiameterLimit[violateClass.index] = true;
			baseAreaFixed += baseAreaUtil.getCoe(violateClass.index);
			treesPerHectareFixed += BaseAreaTreeDensityDiameter
					.treesPerHectare(baseAreaUtil.getCoe(violateClass.index), dqTrial.getCoe(violateClass.index));
		}

		// Make BA's agree with DQ's and TPH's
		for (var uc : UtilizationClass.UTIL_CLASSES) {
			quadMeanDiameterUtil.setCoe(uc.index, dqTrial.getCoe(uc.index));
			treesPerHectareUtil.setCoe(
					uc.index,
					BaseAreaTreeDensityDiameter
							.treesPerHectare(baseAreaUtil.getCoe(uc.index), quadMeanDiameterUtil.getCoe(uc.index))
			);
		}
		// RE VERIFY That sums are correct
		float baSum = (float) UtilizationClass.UTIL_CLASSES.stream().mapToDouble(uc -> baseAreaUtil.getCoe(uc.index))
				.sum();
		float tphSum = (float) UtilizationClass.UTIL_CLASSES.stream()
				.mapToDouble(uc -> treesPerHectareUtil.getCoe(uc.index)).sum();
		if (abs(baSum - baseAreaUtil.getAll()) > 0.0002 * baSum) {
			throw new ProcessingException("Failed to reconcile Base Area");
		}
		if (abs(tphSum - treesPerHectareUtil.getAll()) > 0.0002 * tphSum) {
			throw new ProcessingException("Failed to reconcile Trees per Hectare");
		}
	}

	public static void reconcileComponentsMode3(
			UtilizationVector baseAreaUtil, UtilizationVector treesPerHectareUtil,
			UtilizationVector quadMeanDiameterUtil
	) {

		/*
		 * Reconciliation mode 3 NOT IN THE ORIGINAL DESIGN The primary motivation for this mode is an example where all
		 * trees were in a single utilization class and had a DQ of 12.4 cm. BUT the true DQ for the stand was slightly
		 * over 12.5. In this case the best solution is to simply reassign all trees to the single most appropriate
		 * class.
		 *
		 * Note, "original design" means something pre-VDYP 7. This was added to the Fortran some time before the port
		 * to Java including the comment above.
		 */
		UtilizationClass.UTIL_CLASSES.forEach(uc -> {
			baseAreaUtil.setCoe(uc.index, 0f);
			treesPerHectareUtil.setCoe(uc.index, 0f);
			quadMeanDiameterUtil.setCoe(uc.index, uc.lowBound + 2.5f);
		});

		var ucToUpdate = UtilizationClass.UTIL_CLASSES.stream()
				.filter(uc -> quadMeanDiameterUtil.getAll() < uc.highBound).findFirst().get();

		baseAreaUtil.setCoe(ucToUpdate.index, baseAreaUtil.getAll());
		treesPerHectareUtil.setCoe(ucToUpdate.index, treesPerHectareUtil.getAll());
		quadMeanDiameterUtil.setCoe(ucToUpdate.index, quadMeanDiameterUtil.getAll());
	}
}
