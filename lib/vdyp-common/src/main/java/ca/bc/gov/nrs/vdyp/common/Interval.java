package ca.bc.gov.nrs.vdyp.common;

import org.apache.commons.math3.analysis.UnivariateFunction;

public record Interval(double start, double end) {
	double mid() {
		return (start() + end()) / 2;
	}

	double size() {
		return end() - start();
	}

	Interval evaluate(UnivariateFunction func) {
		return new Interval(func.value(start()), func.value(end()));
	}

}