package ca.bc.gov.nrs.vdyp.ecore.projection.model;

/**
 * Exposes package-private model operations to tests in other packages so that they can build minimal, but complete,
 * model fixtures without running the full polygon definition process.
 */
public final class ModelTestAccess {

	private ModelTestAccess() {
	}

	/**
	 * Build the site species list of <code>layer</code> from its stands, so that the leading SP0 can be determined.
	 *
	 * @param layer the layer whose site species are to be built
	 */
	public static void buildSiteSpecies(Layer layer) {
		layer.doBuildSiteSpecies();
	}
}
