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
        int currentValue = getValue() + 5 * steps;
        if(currentValue < 100) {
        	setValue(currentValue);
        } else {
        	setValue(100);
        }
    }

    @Override
    public void decrement(int steps) {
        int currentValue = getValue() - 5 * steps;
        setValue(currentValue);
    }

}
