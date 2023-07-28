package application.controller;

import javafx.scene.control.SpinnerValueFactory;

/**
 * Custom SpinnerValueFactory that increments and decrements the value by 5.
 */
public class IncrementByFiveSpinnerValueFactory extends SpinnerValueFactory.IntegerSpinnerValueFactory {
	int totalPercent;

	public IncrementByFiveSpinnerValueFactory(int initialValue, int endValue) {
		super(initialValue, endValue);
	}

	@Override
	public void increment(int steps) {
		setValue(getValue() + 5 * steps);
	}

	@Override
	public void decrement(int steps) {
		setValue(getValue() - 5 * steps);
	}

}
