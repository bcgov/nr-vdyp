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
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setDefaults();
	}

	public void setDefaults() {
		SpinnerValueFactory<Integer> startingAgeValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
				0, 500, 0, 10
		); // min, max, default, increment
		startingAge.setValueFactory(startingAgeValueFactory);
		
		SpinnerValueFactory<Integer>finishingAgeValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
				1, 460, 250, 10
		); // min, max, default, increment
		finishingAge.setValueFactory(finishingAgeValueFactory);
		
		SpinnerValueFactory<Integer>ageIncrementValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
				1, 350, 25, 5
		); // min, max, default, increment
		ageIncrement.setValueFactory(ageIncrementValueFactory);
		
		projectionType.getItems().addAll("Volume", "CFS Biomass");
		projectionType.setValue("Volume");
		
		nextPageButton.setDisable(true);
		nextLabel.setDisable(true);
	}
	
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
	// Bottom Menu Bar functionality
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
	 * Handles the default button action event. This method is triggered when the
	 * default button is clicked in the new table window. It sets default values for
	 * species and percentages.
	 *
	 * @param event The ActionEvent triggered by the default button click.
	 */
	public void defaultButtonAction(ActionEvent event) {

	}

	/**
	 * Handles the run button action event.
	 *
	 * This method is triggered when the run model button is clicked in the table
	 * window. It checks if the total percentage from all the spinners is equal to
	 * 100%. If the total percentage is not 100%, an error popup is displayed to
	 * notify the user.
	 *
	 * @param event The ActionEvent triggered by the run button click.
	 */
	public void runButtonAction(ActionEvent event) {
		// Code to run the model goes here
	}
}
