package application.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;


public class StandDensityTableSceneController implements Initializable {
	//Spinners and choice boxes for scene 3
	@FXML
	private Spinner<Integer> percentStockableArea;
	@FXML
	private Spinner<String> treesPerHectare;
	@FXML
	private Spinner<Integer> percentCrownClosure;
	@FXML
	private Spinner<String> basalArea;
	@FXML
	private ChoiceBox<String> minimumDBHLimit;
	
	/**
	 * Initializes the window by setting default values for various controls.
	 *
	 * @param location   The URL of the FXML file. Unused in this method.
	 * @param resources  The ResourceBundle containing locale-specific resources. Unused in this method.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setDefaults();
		
	}

	/**
	 * Sets default values for various controls in the window.
	 * Also, disables certain controls as specified in the Balsamiq mock-ups.
	 * @formatter:off
	 * The default values and control states are as follows:
	 *   	- minimumDBHLimit: "7.5 cm+" with the control disabled.
	 *   	- percentStockableArea: Default value set to 55, with a range from 0 to 100 and an increment of 1.
	 *   	- percentCrownClosure: Default value set to 50, with a range from 0 to 100 and an increment of 1.
	 *   	- treesPerHectare: Spinner values range from "N/A" to "100", representing the sequence of integers from 0 to 100.
	 *   	- basalArea: Spinner values range from "N/A" to "100", representing the sequence of integers from 0 to 100.
	 * @formatter:on
	 */
	public void setDefaults() {
		minimumDBHLimit.setValue("7.5 cm+");
		minimumDBHLimit.setDisable(true); //in Balsamiq mock ups
		
		SpinnerValueFactory<Integer> percentStockableAreaValueFactory =
				new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 55, 1); //min, max, default, increment
		percentStockableArea.setValueFactory(percentStockableAreaValueFactory);
		
		SpinnerValueFactory<Integer> percentCrownClosureValueFactory =
				new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 50, 1); 
		percentCrownClosure.setValueFactory(percentCrownClosureValueFactory);

		
		// Create an array to hold the sequence of integers from 1 to 100 cast to strings
		ObservableList<String> stringValues = FXCollections.observableArrayList();
		stringValues.add("N/A"); // add this so it's the first option. ie spinner is N/A,0,1,...,100
		int[] intValues = new int[100];
		for (int i = 1; i <= 100; i++) {
		    intValues[i - 1] = i;
		    stringValues.add(Integer.toString(intValues[i - 1])); // Corrected index from i to i - 1
		}
		
		SpinnerValueFactory<String> treesPerHectareValueFactory = new SpinnerValueFactory.ListSpinnerValueFactory<>(stringValues);
		treesPerHectare.setValueFactory(treesPerHectareValueFactory);

		SpinnerValueFactory<String> basalAreaValueFactory = new SpinnerValueFactory.ListSpinnerValueFactory<>(stringValues);
		basalArea.setValueFactory(basalAreaValueFactory);
	}
	
	/**
	 * Switches the application to Scene 4 - AdditionalStandAttributesTableScene.
	 *
	 * @param event The ActionEvent triggering the scene switch.
	 * @throws IOException If an I/O error occurs during scene loading.
	 */
	public void switchToScene4(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/AdditionalStandAttributesTableScene.fxml"));
		NewTableSceneController.root = loader.load();
		NewTableSceneController.stage = (Stage)((Node)event.getSource()).getScene().getWindow();
		NewTableSceneController.scene = new Scene(NewTableSceneController.root);
		NewTableSceneController.stage.setScene(NewTableSceneController.scene);
		NewTableSceneController.stage.show();
	}
	
	/**
	 * Switches the application to Scene 2 - SiteInformationTableScene.
	 * Calls the appropriate function in NewTableSceneController
	 * 		 
	 * @param event The ActionEvent triggering the scene switch.
	 *  @throws IOException If an I/O error occurs during scene loading.
	 */
	public void switchToScene2(ActionEvent event) throws IOException {
			NewTableSceneController newTableSceneController = new NewTableSceneController();
			newTableSceneController.switchToScene2(event);
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
