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
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;

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
	@FXML
	private TabPane tabPane;
	@FXML
	private TextArea modelReportText;
	@FXML 
	private Button runButton;
	@FXML
	private Button runButtonReport;

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
	 * @throws IOException 
	 */
	public void runButtonAction(ActionEvent event) throws IOException {
		String text = "                                                           VDYP7 Yield Table\r\n"
				+ "                        Lodgepole Pine 30.0%, Poplar 30.0%, Hemlock 30.0%, Spruce 10.0%\r\n"
				+ "\r\n"
				+ "\r\n"
				+ "                 Quad                      |   Whole   \r\n"
				+ "     Site Lorey  Stnd                      |    Stem   \r\n"
				+ "TOT   HT    HT    DIA     BA        TPH    |  VOLUME   \r\n"
				+ "AGE   (m)   (m)  (cm) (m**2/ha) (trees/ha) | (m**3/ha) \r\n"
				+ "-------------------------------------------+-----------\r\n"
				+ "                                           |           \r\n"
				+ "  0   0.0                                  |           \r\n"
				+ " 25   7.7                                  |           \r\n"
				+ " 50  15.0  10.9  12.3      10.6        895 |       44.2\r\n"
				+ " 75  19.4  16.3  17.8      26.1       1054 |      165.0\r\n"
				+ "100  22.0  20.1  20.6      32.0        959 |      253.2\r\n"
				+ "125  23.8  23.0  23.6      35.8        815 |      325.7\r\n"
				+ "150  25.0  25.3  27.4      37.1        631 |      369.5\r\n"
				+ "175  25.8  26.6  30.4      36.7        507 |      382.2\r\n"
				+ "200  26.5  27.2  32.4      36.5        444 |      385.7\r\n"
				+ "225  26.9  27.2  33.5      36.6        416 |      385.8\r\n"
				+ "250  27.3  27.3  34.4      36.7        394 |      386.3\r\n"
				+ "\r\n"
				+ "\r\n"
				+ "NOTE: Height 7.7 at Projection Age 25.0 is too short to generate yields for species 'PL'\r\n"
				+ "NOTE: Projected data for species 'PL' was not generated at stand age 25.0\r\n"
				+ "NOTE: Basal Area and Trees per HA computed using Default CC of 50%\r\n"
				+ "NOTE: Yields are not predicted prior to age 50.\r\n"
				+ "\r\n"
				+ "\r\n"
				+ "TABLE PROPERTIES:\r\n"
				+ "\r\n"
				+ "WinVDYP Version Number... 7.33b         % Stockable Area Supplied 55\r\n"
				+ "VDYP7 Core DLL Version... 7.19h         CFS Eco Zone............. \r\n"
				+ "VDYP SI Version Number... 7.13c         Trees Per Hectare........ <Not Used>\r\n"
				+ "SINDEX Version Number.... 1.51          Measured Basal Area...... <Not Used>\r\n"
				+ "Species 1................ PL (30.0%)    Starting Total Age....... 0\r\n"
				+ "Species 2................ AC (30.0%)    Finishing Total Age...... 250\r\n"
				+ "Species 3................ H  (30.0%)    Age Increment............ 25\r\n"
				+ "Species 4................ S  (10.0%)    Projected Values......... Volume\r\n"
				+ "FIP Calc Mode............ 1             Min DBH Limit: PL........ 7.5 cm+\r\n"
				+ "BEC Zone................. IDF           Min DBH Limit: AC........ 7.5 cm+\r\n"
				+ "Incl Second Species Ht... N/A           Min DBH Limit: H ........ 7.5 cm+\r\n"
				+ "% Crown Closure Supplied. 0             Min DBH Limit: S ........ 7.5 cm+\r\n"
				+ "\r\n"
				+ "Species Parameters...\r\n"
				+ " Species |  % Comp | Tot Age |  BH Age |  Height |    SI   |  YTBH   \r\n"
				+ " --------+---------+---------+---------+---------+---------+---------\r\n"
				+ "    PL   |  30.0   |    60   |    54   |  17.00  |  16.30  |  6.80   \r\n"
				+ "    AC   |  30.0   |   N/A   |   N/A   |   N/A   |   N/A   |   N/A   \r\n"
				+ "     H   |  30.0   |   N/A   |   N/A   |   N/A   |  12.52  |  8.80   \r\n"
				+ "     S   |  10.0   |   N/A   |   N/A   |   N/A   |   N/A   |   N/A   \r\n"
				+ "\r\n"
				+ "Site Index Curves Used... \r\n"
				+ "  Age Range | Species | SI Curve Name                                     \r\n"
				+ " -----------+---------+-------------------------------------------------\r\n"
				+ "   50 -  250|   Hwi   | 37 - Nigh (1998)\r\n"
				+ "\r\n"
				+ "\r\n"
				+ "Additional Stand Attributes:\r\n"
				+ "----------------------------\r\n"
				+ "\r\n"
				+ "None Applied.";
		
		runButtonReport.setDisable(true); //TODO make this dynamic where it checks pane
		modelReportText.setText(text);
		tabPane.getSelectionModel().selectNext();
	}
}
