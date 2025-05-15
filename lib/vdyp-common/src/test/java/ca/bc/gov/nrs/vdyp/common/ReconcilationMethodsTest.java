package ca.bc.gov.nrs.vdyp.common;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class ReconcilationMethodsTest {

	@Test
	void testReconcilationWhenBasalAreaAllIsZero() throws ProcessingException {
		UtilizationVector basalAreaByUtilization = Utils.utilizationVector(0.0f);
		UtilizationVector quadMeanDiameterByUtilization = Utils.utilizationVector(0.0f);
		UtilizationVector treesPerHectareByUtilization = Utils.utilizationVector(0.0f);

		ReconcilationMethods.reconcileComponents(
				basalAreaByUtilization, treesPerHectareByUtilization, quadMeanDiameterByUtilization
		);
		assertThat(treesPerHectareByUtilization, VdypMatchers.utilization(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
		assertThat(quadMeanDiameterByUtilization, VdypMatchers.utilization(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
	}

	@Test
	void testReconcileComponentsMode1() throws ProcessingException {

		var dq = Utils.utilizationVector();
		var ba = Utils.utilizationVector();
		var tph = Utils.utilizationVector();

		// '082E004 615 1988' with component BA re-ordered from smallest to largest to
		// force mode 1.

		dq.setCoe(0, 13.4943399f);
		dq.setCoe(1, 10.2766619f);
		dq.setCoe(2, 14.67033f);
		dq.setCoe(3, 19.4037666f);
		dq.setCoe(4, 25.719244f);

		ba.setCoe(0, 2.20898318f);
		ba.setCoe(1, 0.220842764f);
		ba.setCoe(2, 0.433804274f);
		ba.setCoe(3, 0.691931725f);
		ba.setCoe(4, 0.862404406f);

		tph.setCoe(0, 154.454025f);
		tph.setCoe(1, 83.4198151f);
		tph.setCoe(2, 51.0201035f);
		tph.setCoe(3, 14.6700592f);
		tph.setCoe(4, 4.25086117f);

		ReconcilationMethods.reconcileComponents(ba, tph, dq);

		assertThat(ba, VdypMatchers.utilization(0f, 2.20898318f, 0.220842764f, 0.546404183f, 1.44173622f, 0f));
		assertThat(tph, VdypMatchers.utilization(0f, 154.454025f, 49.988575f, 44.5250206f, 59.9404259f, 0f));
		assertThat(dq, VdypMatchers.utilization(0f, 13.4943399f, 7.5f, 12.5f, 17.5f, 22.5f));
	}

	@Test
	void testReconcileComponentsMode2() throws ProcessingException {

		var dq = Utils.utilizationVector();
		var ba = Utils.utilizationVector();
		var tph = Utils.utilizationVector();
		dq.setCoe(0, 31.6622887f);
		dq.setCoe(1, 10.0594692f);
		dq.setCoe(2, 14.966774f);
		dq.setCoe(3, 19.9454956f);
		dq.setCoe(4, 46.1699982f);

		ba.setCoe(0, 0.397305071f);
		ba.setCoe(1, 0.00485289097f);
		ba.setCoe(2, 0.0131751001f);
		ba.setCoe(3, 0.0221586525f);
		ba.setCoe(4, 0.357118428f);

		tph.setCoe(0, 5.04602766f);
		tph.setCoe(1, 0.61060524f);
		tph.setCoe(2, 0.748872101f);
		tph.setCoe(3, 0.709191978f);
		tph.setCoe(4, 2.13305807f);

		ReconcilationMethods.reconcileComponents(ba, tph, dq);

		assertThat(
				ba,
				VdypMatchers.utilization(0f, 0.397305071f, 0.00485289097f, 0.0131751001f, 0.0221586525f, 0.357118428f)
		);
		assertThat(
				tph, VdypMatchers.utilization(0f, 5.04602766f, 0.733301044f, 0.899351299f, 0.851697803f, 2.56167722f)
		);
		assertThat(dq, VdypMatchers.utilization(0f, 31.6622887f, 9.17939758f, 13.6573782f, 18.2005272f, 42.1307297f));

	}

	@Test
	void testReconcileComponentsMode3() throws ProcessingException {

		var dq = Utils.utilizationVector();
		var ba = Utils.utilizationVector();
		var tph = Utils.utilizationVector();

		// Set of inputs that cause mode 2 to fail over into mode 3

		dq.setCoe(0, 12.51f);
		dq.setCoe(1, 12.4f);
		dq.setCoe(2, 0f);
		dq.setCoe(3, 0f);
		dq.setCoe(4, 0f);

		ba.setCoe(0, 2.20898318f);
		ba.setCoe(1, 2.20898318f);
		ba.setCoe(2, 0f);
		ba.setCoe(3, 0f);
		ba.setCoe(4, 0f);

		tph.setCoe(0, 179.71648f);
		tph.setCoe(1, 182.91916f);
		tph.setCoe(2, 0f);
		tph.setCoe(3, 0f);
		tph.setCoe(4, 0f);

		ReconcilationMethods.reconcileComponents(ba, tph, dq);

		assertThat(ba, VdypMatchers.utilization(0f, 2.20898318f, 0f, 2.20898318f, 0f, 0f));
		assertThat(tph, VdypMatchers.utilization(0f, 179.71648f, 0f, 179.71648f, 0f, 0f));
		assertThat(dq, VdypMatchers.utilization(0f, 12.51f, 10, 12.51f, 20f, 25f));

	}

}
