package application.controller;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

public class SiteInformationTableSceneController implements Initializable {
	@FXML
	private ChoiceBox<String> ecoZone;
	@FXML
	private ChoiceBox<String> becZone;
	@FXML
	private ChoiceBox<String> ageType;
	@FXML
	private Spinner<Double> standAge;
	@FXML
	private Spinner<Double> standHeight;
	@FXML
	private Spinner<Double> bha50SiteIndex;
	@FXML
	private Button runButton;
	@FXML
	private Button runButtonReport;
	@FXML
	private ChoiceBox<String> loggingOn;
	@FXML
	private ChoiceBox<String> loggingType;
	@FXML
	private ChoiceBox<String> siteSpecies;
	
	/**
	 * Initializes the SiteInformationTableScene with default values and
	 * configurations. This method is automatically called when the associated FXML
	 * is loaded.
	 *
	 * @param location  The URL location used to resolve relative paths.
	 * @param resources The ResourceBundle containing localizable resources.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setDefaults(); // this is a function so it can also be called from menu button

	}

	/**
	 * Sets default values for various controls in the window and disables certain controls as specified in the Balsamiq mock-ups.
	 * The default values and control states are as follows:
	 * @formatter:off
	 * 		- becZone: Default value set to "IDF - Interior Douglas Fir" with options from predefined BEC Zones.
	 * 		- ecoZone: Default value set to "Select Species" with options from predefined Eco Zones.
	 * 		- ageType: Default value set to "Total" with options "Total" and "Breast".
	 * 		- standAge: Default value set to 60.0, with a range from 0.00 to 500.00 and an increment of 10.0.
	 * 		- standHeight: Default value set to 17.00, with a range from 0.00 to 99.9 and an increment of 1.0.
	 * 		- bha50SiteIndex: Default value set to 16.30, with a range from 0.00 to 60.0 and an increment of 1.0.
	 * 						  The control is disabled by default.
	 * 		- runButton: The runButton control is disabled by default.
	 * 		- runButtonReport: Disabled by default- loggingOn: Sets the choices Yes and No. Yes by default
	 * 		- loggingType: Set the choices as Basic,Intermediate and Advanced. Basic by Default
	 * 		- loggingOn: Sets the choices Yes and No. Yes by default
	 * @formatter:on
	 */
	public void setDefaults() {
		// Arrays containing the options for different Bec & Eco Zones
		final String[] becZones = { "AT - Alpine Tundra", "BG - Bunch Grass", "BWBS - Boreal White and Black Spruce",
				"CDF - Coastal Douglas Fir", "CWH - Coastal Western Hemlock", "ESSF - Engelmann Spruce",
				"ICH - Interior Cedar Hemlock", "IDF - Interior Douglas Fir", "MH - Mountain Hemlock",
				"MS - Montane Spruce", "pp = Ponderosa Pine", "SBPS - Sub-Boreal Pine-Spruce",
				"SBS - Sub-Boreal Spruce", "SWB - Spruce-Willow-Birch" };
		final String[] ecoZones = { "Boreal Cordillera", "Boreal Plains", "Montane Cordillera", "Pacific Maritime",
				"Taiga Plains" };

		// An array containing the options for AgeType
		final String[] ageTypes = { "Total", "Breast" };

		// Add choice box options and set defaults
		becZone.getItems().addAll(becZones); // these are defined below in the scene#2 section
		ecoZone.getItems().addAll(ecoZones);
		ageType.getItems().addAll(ageTypes);

		becZone.setValue("IDF - Interior Douglas Fir");
		ecoZone.setValue("Select Species");
		ageType.setValue("Total");

		SpinnerValueFactory<Double> standAgeValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(
				0.00, 500.00, 60.00, 10
		); // min, max, default, increment
			// Currently the default when WinVDYP is opened is 60
		standAge.setValueFactory(standAgeValueFactory);

		SpinnerValueFactory<Double> standHeightValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(
				0.00, 99.9, 17.00, 1
		);
		standHeight.setValueFactory(standHeightValueFactory);

		SpinnerValueFactory<Double> bha50SiteIndexValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(
				0.00, 60.0, 16.30, 1
		);
		bha50SiteIndex.setValueFactory(bha50SiteIndexValueFactory);
		bha50SiteIndex.setDisable(true); // Real WinVDYP uses the other two to calculate this, so it should be disable
											// by default

		runButton.setDisable(true);
		runButtonReport.setDisable(true);

		loggingOn.getItems().addAll("Yes", "No");
		loggingOn.setValue("Yes");

		loggingType.getItems().addAll("Basic", "Intermediate", "Advanced");
		loggingType.setValue("Basic");
	}

	// Radio button selections
	/**
	 * Handles the action event when the "Age (years)" option is selected in the
	 * float radiobutton. Disables the standAge choicebox and enables the
	 * bha50SiteIndex and standHeight choiceboxes.
	 *
	 * @param event The ActionEvent generated by the user action.
	 */
	public void standAgeSelected(ActionEvent event) {
		standAge.setDisable(true);
		bha50SiteIndex.setDisable(false);
		standHeight.setDisable(false); // since we don't know from where it's being called
	}

	/**
	 * Handles the action event when the "Height (meters)" option is selected in the
	 * float radiobutton. Disables the standHeight choicebox and enables the
	 * standAge and bha50SiteIndex choiceboxes.
	 *
	 * @param event The ActionEvent generated by the user action.
	 */
	public void standHeightSelected(ActionEvent event) {
		standHeight.setDisable(true);
		standAge.setDisable(false); // since we don't know from where it's being called
		bha50SiteIndex.setDisable(false);
	}

	/**
	 * Handles the action event when the "BHA 50 Site Index" option is selected in
	 * the float radiobutton. Disables the bha50SiteIndex choicebox and enables the
	 * standAge and standHeight choiceboxes .
	 *
	 * @param event The ActionEvent generated by the user action.
	 */
	public void siteIndexSelected(ActionEvent event) {
		bha50SiteIndex.setDisable(true);
		standAge.setDisable(false);
		standHeight.setDisable(false); // since we don't know from where it's being called
	}
	// End of radio button selections

	// Bottom Menu Bar functionality
	/**
	 * Handles the cancel button action event. This method is triggered when the
	 * cancel button is clicked in the site information table window. It closes this
	 * window.
	 *
	 * @param event The ActionEvent triggered by the cancel button click.
	 */
	public void cancelButtonAction(ActionEvent event) {
		MainController.getNewWindow().close();
	}

	/**
	 * Switches the application to Scene 1 - NewTableScene.
	 *
	 * @param event The ActionEvent triggering the scene switch.
	 * @throws IOException If an I/O error occurs during scene loading.
	 */
	public void switchToScene1(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/NewTableScene.fxml"));
		Parent root = loader.load();
		Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}

	/**
	 * Switches the application to Scene 3 - StandDensityTableScene.
	 *
	 * @param event The ActionEvent triggering the scene switch.
	 * @throws IOException If an I/O error occurs during scene loading.
	 */
	public void switchToScene3(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/StandDensityTableScene.fxml"));
		Parent root = loader.load();
		Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}
	// Bottom Menu bar end


	public void displayChoicesPassedIn(List<ChoiceBox<String>> speciesChoiceBoxes) throws IOException {
		if(speciesChoiceBoxes.isEmpty()) {
			return;
		}
		
		String[] dbhLabels = new String[4];
		int i = 0;
		 for(ChoiceBox<String> species : speciesChoiceBoxes) {
			String speciesText = species.getValue();
			
			if(!speciesText.equals("Select Species")) {
				siteSpecies.getItems().add(speciesText.substring(0,2)); //add to choicebox in this scene
				
				if(i < 4) {
					dbhLabels[i] = speciesText;
					i++;
				}
			}
		}
		siteSpecies.setValue(speciesChoiceBoxes.get(0).getValue().substring(0,2)); //set default
		

      
        this.dbhLabels = dbhLabels; //TODO make this better :)

	}

	private static String[] dbhLabels;
	
	public static String[] getdbhLabels() {
		if (dbhLabels == null) {
			throw new IllegalStateException("newWindow is not set yet. Make sure to initialize it before accessing.");
		}
		return dbhLabels;
	}


}
