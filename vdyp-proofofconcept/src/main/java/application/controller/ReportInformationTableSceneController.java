package application.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;

public class ReportInformationTableSceneController implements Initializable {

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		
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
	
	//Bottom Menu Bar functionality 	
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
	 * This method is triggered when the run model button is clicked in the 
	 * table window. It checks if the total percentage from all the spinners 
	 * is equal to 100%. If the total percentage is not 100%, an error popup
	 *  is displayed to notify the user.
	 *
	 * @param event The ActionEvent triggered by the run button click.
	 */
	public void runButtonAction(ActionEvent event) {
		// Code to run the model goes here
	}
}
