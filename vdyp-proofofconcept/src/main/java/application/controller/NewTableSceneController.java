package application.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

public class NewTableSceneController implements Initializable {

	
	// Set up species choice boxes
	@FXML
	private ChoiceBox<String> species_1;
	@FXML
	private ChoiceBox<String> species_2;
	@FXML
	private ChoiceBox<String> species_3;
	@FXML
	private ChoiceBox<String> species_4;
	@FXML
	private ChoiceBox<String> species_5;
	@FXML
	private ChoiceBox<String> species_6;

	// List of speciesChoiceBoxes
	private List<ChoiceBox<String>> speciesChoiceBoxes = new ArrayList<>();

	// An array containing the names of different tree species
	private final String[] treeSpecies = { "AC - Popular", "AT - Aspen", "B - True Fir", "BA - Amabilis Fir",
			"BG - Grand Fir", "BL - Alpine Fir", "CW - Western Red Cedar", "DR - Red Alder", "E - Birch",
			"EA - Alaska Paper Birch", "EP - Common Paper Birch", "FD - Douglas Fir", "H - Hemlock",
			"HM - Mountain Hemlock", "HW - Western Hemlock", "L - Larch", "LA - Alpine Larch", "LT - Tamarack",
			"LW - Western Larch", "MB - Bigleaf Maple", "PA - Whitebark Pine", "PF - Limber Pine", "PJ - Jack Pine",
			"PL - Lodgepole Pine", "PY - Western White Pine", "PY - Ponderosa (Yellow) Pine", "S - Spruce",
			"SB - Black Spruce", "SE - Engelmann Spruce", "SS - Sitka Spruce", "SW - White Spruce",
			"YC - Yellow Cedar" };

	// Labels for display based on choice boxes
	@FXML
	private Label species1Group;
	@FXML
	private Label species1Site;
	@FXML
	private Label species2Group;
	@FXML
	private Label species2Site;
	@FXML
	private Label species3Group;
	@FXML
	private Label species3Site;
	@FXML
	private Label species4Group;
	@FXML
	private Label species4Site;
	@FXML
	private Label species5Group;
	@FXML
	private Label species5Site;
	@FXML
	private Label species6Group;
	@FXML
	private Label species6Site;

	// Array of Species Groups and Sites
	@FXML
	private Label[] speciesGroups = new Label[6];
	@FXML
	private Label[] speciesSites = new Label[6];

	// Species percent and species group percent spinners
	@FXML
	private Spinner<Integer> species1Percent;
	@FXML
	private Spinner<Integer> species2Percent;
	@FXML
	private Spinner<Integer> species3Percent;
	@FXML
	private Spinner<Integer> species4Percent;
	@FXML
	private Spinner<Integer> species5Percent;
	@FXML
	private Spinner<Integer> species6Percent;

	// Species group percent labels
	@FXML
	private Label species1GroupPercent;
	@FXML
	private Label species2GroupPercent;
	@FXML
	private Label species3GroupPercent;
	@FXML
	private Label species4GroupPercent;
	@FXML
	private Label species5GroupPercent;
	@FXML
	private Label species6GroupPercent;

	// List of speciesPercentSpinners
	private List<Spinner<Integer>> speciesPercentSpinners = new ArrayList<>();

	// Spinner labels
	@FXML
	private Label[] speciesGroupPercentLabels = new Label[6];
	@FXML
	private Label totalPercentLabel;
	
	// Needed to switch to the next scene
	private Stage stage;
	private Scene scene;
	private Parent root;
	private static int sceneNumber = 1;
	
	//Choice boxes for scene #2
	@FXML
	private ChoiceBox<String> ecoZone;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		if(sceneNumber == 1) {
			// Add species choice boxes to the List
			speciesChoiceBoxes.add(species_1);
			speciesChoiceBoxes.add(species_2);
			speciesChoiceBoxes.add(species_3);
			speciesChoiceBoxes.add(species_4);
			speciesChoiceBoxes.add(species_5);
			speciesChoiceBoxes.add(species_6);
	
			// Add the labels to the arrays
			speciesGroups[0] = species1Group;
			speciesGroups[1] = species2Group;
			speciesGroups[2] = species3Group;
			speciesGroups[3] = species4Group;
			speciesGroups[4] = species5Group;
			speciesGroups[5] = species6Group;
	
			speciesSites[0] = species1Site;
			speciesSites[1] = species2Site;
			speciesSites[2] = species3Site;
			speciesSites[3] = species4Site;
			speciesSites[4] = species5Site;
			speciesSites[5] = species6Site;
	
			// Add species percent spinners to the array
			speciesPercentSpinners.add(species1Percent);
			speciesPercentSpinners.add(species2Percent);
			speciesPercentSpinners.add(species3Percent);
			speciesPercentSpinners.add(species4Percent);
			speciesPercentSpinners.add(species5Percent);
			speciesPercentSpinners.add(species6Percent);
	
			// Add labels to array
			speciesGroupPercentLabels[0] = species1GroupPercent;
			speciesGroupPercentLabels[1] = species2GroupPercent;
			speciesGroupPercentLabels[2] = species3GroupPercent;
			speciesGroupPercentLabels[3] = species4GroupPercent;
			speciesGroupPercentLabels[4] = species5GroupPercent;
			speciesGroupPercentLabels[5] = species6GroupPercent;
	
			// Add "Select species" as the item for each ChoiceBox and set items
			for (ChoiceBox<String> choiceBox : speciesChoiceBoxes) {
				choiceBox.getItems().addAll(treeSpecies);
				choiceBox.setValue("Select species");
			}
	
			// Set Default values
			setDefaults(); // Initially the code above was within this method but this lead to slower
							// execution
	
			// Set up listeners for each choice box
			for (int i = 0; i < speciesChoiceBoxes.size(); i++) {
				final int index = i;
	
				speciesChoiceBoxes.get(i).getSelectionModel().selectedItemProperty()
						.addListener(new ChangeListener<String>() {
							@Override
							public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
								// Get the selected item from the choice box at the current index
								String currentSpecies = speciesChoiceBoxes.get(index).getSelectionModel().getSelectedItem()
										.substring(0, 2);
	
								// Update the labels to display the selected item
								speciesGroups[index].setText(currentSpecies);
								speciesSites[index].setText(currentSpecies);
							}
						});
			}
	
			// Set up listeners for each spinner
			for (int i = 0; i < speciesPercentSpinners.size(); i++) {
	
				int index = i;
	
				speciesPercentSpinners.get(i).valueProperty().addListener(new ChangeListener<Integer>() {
					public void changed(ObservableValue<? extends Integer> arg0, Integer arg1, Integer arg2) {
						int currentValue = speciesPercentSpinners.get(index).getValue();
						updateTotalLabel(); // Update the total label when a spinner value changes
						speciesGroupPercentLabels[index].setText(Integer.toString(currentValue));
					}
				});
			}
		} else if (sceneNumber == 2) {
			
		}
	}

// Below until switchToScene2 this is relevant to scene#1 from NewTableScene which is triggered when a new table opens
	/**
	 * Sets default values for species and percentages in the new table window.
	 * @formatter:off
	 * The default values are:
	 * 			- Species 1: "PL - Lodgepole Pine" with 30%.
	 * 			- Species 2:"AC - Poplar" with 30%.
	 * 			- Species 3: "H - Hemlock" with 30%.
	 * 			- Species 4: "S - Spruce" with 10%.
	 * For Species 5 to Species 6, the selection is cleared, and percentages are reset to 0.
	 * It updates the labels to display the selected item and percentage. After setting the
	 * default values, it updates the total label.
	 * @formatter:on
	 */
	private void setDefaults() {
		// Set all percent values to 0, this avoids totalPercent from exceeding 100% and
		// causing errors
		for (Spinner<Integer> spinner : speciesPercentSpinners) {
			IncrementByFiveSpinnerValueFactory valueFactory = new IncrementByFiveSpinnerValueFactory(0, 100);
			valueFactory.setValue(0);
			spinner.setValueFactory(valueFactory);
		}
		// Set the default species and percentage for species 1-4
		species_1.setValue("PL - Lodgepole Pine");
		species1Percent.getValueFactory().setValue(30);

		species_2.setValue("AC - Poplar");
		species2Percent.getValueFactory().setValue(30);

		species_3.setValue("H - Hemlock");
		species3Percent.getValueFactory().setValue(30);

		species_4.setValue("S - Spruce");
		species4Percent.getValueFactory().setValue(10);

		// Clear the selection and reset percentages for other species (species 5 to
		// species 6)
		for (int i = 5; i < speciesChoiceBoxes.size(); i++) {
			speciesChoiceBoxes.get(i).setValue("Select species");
			speciesPercentSpinners.get(i).getValueFactory().setValue(0);
		}

		// Update the labels to display the selected item and percentage labels for
		// species 1-4
		species1Group.setText("PL");
		species1Site.setText("PL");
		species1GroupPercent.setText("30");

		species2Group.setText("AC");
		species2Site.setText("AC");
		species2GroupPercent.setText("30");

		species3Group.setText("H");
		species3Site.setText("H");
		species3GroupPercent.setText("30");

		species4Group.setText("S");
		species4Site.setText("S");
		species4GroupPercent.setText("10");

		// Update the total label after setting the default values
		updateTotalLabel();
	}

	/**
	 * Updates the total label with the sum of all species percent values from the
	 * spinners.
	 *
	 * This method iterates through a list of species percent spinners and
	 * calculates the total sum of their values. The resulting total is displayed in
	 * the totalPercentLabel. 
	 *
	 */
	private void updateTotalLabel() throws ArithmeticException {
		int total = getTotalPercent();
		totalPercentLabel.setText(Integer.toString(total));
	}

	/**
	 * Method to calculate the total percentage from all the spinners.
	 *
	 * @return The total percentage calculated from the spinners.
	 */
	private int getTotalPercent() {
		int total = 0;
		for (Spinner<Integer> spinner : speciesPercentSpinners) {
			total += spinner.getValue();
		}
		return total;
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
	 * Handles the default button action event. This method is triggered when the
	 * default button is clicked in the new table window. It sets default values for
	 * species and percentages.
	 *
	 * @param event The ActionEvent triggered by the default button click.
	 */
	public void defaultButtonAction(ActionEvent event) {
		setDefaults();
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
		int total = getTotalPercent();

		if (total != 100) {
			showErrorPopup("Total percent does not total 100%");
		} else {
			// Code to run the model goes here
		}
	}

	/**
	 * Displays an error popup with the specified error message.
	 *
	 * This method creates an Alert dialog of type INFORMATION to display an error
	 * message to the user. The title of the dialog is set to "Error", and the
	 * header text is set to null. The error message passed as the 'message'
	 * parameter is displayed as the content of the dialog.
	 *
	 * @param message The error message to be shown in the error popup.
	 */
	private static void showErrorPopup(String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Error");
		alert.setHeaderText("An error has occurred.");
		alert.setContentText(message);

		alert.showAndWait();
	}

	public void switchToScene2(ActionEvent event) throws IOException {
		sceneNumber = 2;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/SiteInformationTableScene.fxml"));
		root = loader.load();
		stage = (Stage)((Node)event.getSource()).getScene().getWindow();
		scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}
	
// Below until switchToScene3 this is relevant to scene#2 from SiteInformationTableScene 
	public void switchToScene1(ActionEvent event) throws IOException {
		sceneNumber = 1;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/NewTableScene.fxml"));
		root = loader.load();
		stage = (Stage)((Node)event.getSource()).getScene().getWindow();
		scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}
}
