package application.controller;

import java.util.List;

import javafx.scene.control.Alert;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

/**
 * Custom SpinnerValueFactory that increments and decrements the value by 5.
 */
public class IncrementByFiveSpinnerValueFactory extends SpinnerValueFactory.IntegerSpinnerValueFactory {
    private List<Spinner<Integer>> otherSpinners;

    public IncrementByFiveSpinnerValueFactory(int initialValue, int endValue, List<Spinner<Integer>> otherSpinners) {
        super(initialValue, endValue);
        this.otherSpinners = otherSpinners;
    }

    @Override
    public void increment(int steps) {
        int currentValue = getValue() + 5 * steps;
        setValue(currentValue);
    }

    @Override
    public void decrement(int steps) {
        int currentValue = getValue() - 5 * steps;
        setValue(currentValue);
    }

}
