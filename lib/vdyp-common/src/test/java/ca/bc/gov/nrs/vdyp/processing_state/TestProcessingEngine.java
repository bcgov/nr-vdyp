package ca.bc.gov.nrs.vdyp.processing_state;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.ProcessingEngine;;

public class TestProcessingEngine extends ProcessingEngine<TestProcessingState, TestLayerProcessingState> {

	public TestProcessingEngine(TestProcessingState processingState) {
		super(processingState, Optional.empty());
	}

	public enum TestExecutionStep implements ProcessingEngine.ExecutionStep {
		;

		@Override
		public int compareTo(ExecutionStep o) {
			if (o instanceof TestExecutionStep fes) {
				return this.compareTo(fes);
			}
			if (o instanceof BaseExecutionStep bes) {
				return -bes.compareTo(this);
			}
			throw new IllegalArgumentException("Cannot compare " + o.getClass() + " and " + this.getClass());

		}

	}

}
