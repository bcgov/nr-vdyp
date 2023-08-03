package application.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

public class ReportInformationTableSceneController implements Initializable {

	@FXML
	private Spinner<Integer> startingAge;
	@FXML
	private Spinner<Integer> finishingAge;
	@FXML
	private Spinner<Integer> ageIncrement;
	@FXML
	private ChoiceBox<String> projectionType;
	@FXML
	private Button nextPageButton;
	@FXML
	private Label nextLabel;

	/**
	 * Initializes the window by setting default values for various controls.
	 *
	 *
	 * @param location  The URL of the FXML file. Unused in this method.
	 * @param resources The ResourceBundle containing locale-specific resources.
	 *                  Unused in this method.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setDefaults();
	}

	/**
	 * Sets default values for various controls in the window.
	 * The default values and control states are as follows:
	 * @formatter:off
	 * 		- startingAge: Default value set to 0, with a range from 0 to 500 and an increment of 10.
	 * 		- finishingAge: Default value set to 250, with a range from 1 to 460 and an increment of 10.
	 * 		- ageIncrement: Default value set to 25, with a range from 1 to 350 and an increment of 5.
	 * 		- projectionType: Default value set to "Volume" with options "Volume" and "CFS Biomass".
	 * 		- nextPageButton: The nextPageButton control is disabled by default.
	 * 		- nextLabel: The nextLabel control is disabled by default.
	 * @formatter:on
	 */
	public void setDefaults() {
		SpinnerValueFactory<Integer> startingAgeValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
				0, 500, 0, 10
		); // min, max, default, increment
		startingAge.setValueFactory(startingAgeValueFactory);

		SpinnerValueFactory<Integer> finishingAgeValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
				1, 460, 250, 10
		); // min, max, default, increment
		finishingAge.setValueFactory(finishingAgeValueFactory);

		SpinnerValueFactory<Integer> ageIncrementValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
				1, 350, 25, 5
		); // min, max, default, increment
		ageIncrement.setValueFactory(ageIncrementValueFactory);

		projectionType.getItems().addAll("Volume", "CFS Biomass");
		projectionType.setValue("Volume");

		nextPageButton.setDisable(true);
		nextLabel.setDisable(true);
	}

	// Bottom menu bar
	/**
	 * Switches the application to Scene 4 - StandDensityTableScene.
	 *
	 * @param event The ActionEvent triggering the scene switch.
	 * @throws IOException If an I/O error occurs during scene loading.
	 */
	public void switchToScene4(ActionEvent event) throws IOException {
		StandDensityTableSceneController standDensityTableSceneController = new StandDensityTableSceneController();
		standDensityTableSceneController.switchToScene4(event);
	}

	/**
	 * Handles the cancel button action event. This method is triggered when the
	 * cancel button is clicked in the new table window. It closes this new table
	 * window.
	 *
	 * @param event The ActionEvent triggered by the cancel button click.
	 */
	public void cancelButtonAction(ActionEvent event) {
		MainController.getNewWindow().close();
	}

	/**
	 * Handles the run button action event.
	 *
	 * This method is triggered when the run model button is clicked in the table
	 * window.
	 *
	 * @param event The ActionEvent triggered by the run button click.
	 */
	public void runButtonAction(ActionEvent event) {
		// Code to run the model goes here
	}
}
