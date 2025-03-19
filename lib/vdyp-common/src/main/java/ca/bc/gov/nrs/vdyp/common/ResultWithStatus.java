package ca.bc.gov.nrs.vdyp.common;

/**
 * Result that includes a non-fatal status
 */
public record ResultWithStatus<V, S>(V value, S status) {

	static interface Status {
		/**
		 * The status is normal and unexceptional.
		 * 
		 * @return
		 */
		boolean isOK();
	}

	/**
	 * A basic status enum
	 */
	public enum BasicStatus implements Status {
		OK(true),
		WARNING(false);

		private final boolean ok;

		BasicStatus(boolean ok) {
			this.ok = ok;
		}

		@Override
		public boolean isOK() {
			return ok;
		}
	}
}
