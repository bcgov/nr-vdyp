package application.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

public class AdditionalStandAtrributesTableSceneController implements Initializable {

	@FXML
	Spinner<Double> loreyHeight;
	@FXML
	Spinner<Double> basalArea12;
	@FXML
	Spinner<Double> cuVolume; // cu = Close Utilization
	@FXML
	Spinner<Double> cuNetDecayWasteVolume;
	@FXML
	Spinner<Double> wholeStemVolume7;
	@FXML
	Spinner<Double> wholeStemVolume12;
	@FXML
	Spinner<Double> cuNetDecayVolume;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setDefaults();
	}
	
	public void useComputedValues() {
		loreyHeight.setDisable(true);
		basalArea12.setDisable(true);
		cuVolume.setDisable(true);
		cuNetDecayWasteVolume.setDisable(true);
		wholeStemVolume7.setDisable(true);
		wholeStemVolume12.setDisable(true);
		cuNetDecayVolume.setDisable(true);
	}
	
	public void modifyComputedValues() {
		loreyHeight.setDisable(false);
		basalArea12.setDisable(false);
		cuVolume.setDisable(false);
		cuNetDecayWasteVolume.setDisable(false);
		wholeStemVolume7.setDisable(false);
		wholeStemVolume12.setDisable(false);
		cuNetDecayVolume.setDisable(false);
	}
	
	public void setDefaults() {
		SpinnerValueFactory<Double> loreyHeightValueFactory =
				new SpinnerValueFactory.DoubleSpinnerValueFactory(0.00, 99.90, 21.83, 1);
		loreyHeight.setValueFactory(loreyHeightValueFactory);
		loreyHeight.setDisable(true);
		
		SpinnerValueFactory<Double> basalArea12ValueFactory = 
				new SpinnerValueFactory.DoubleSpinnerValueFactory(0.10, 250.00, 39.3337, 1);
		basalArea12.setValueFactory(basalArea12ValueFactory);
		basalArea12.setDisable(true);
		
		SpinnerValueFactory<Double> cuVolumeValueFactory = 
				new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 2500.00, 39.3337, 1);
		cuVolume.setValueFactory(cuVolumeValueFactory);
		cuVolume.setDisable(true);
		
		SpinnerValueFactory<Double> cuNetDecayWasteVolumeValueFactory = 
				new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 2500.00, 245.5, 1);
		cuNetDecayWasteVolume.setValueFactory(cuNetDecayWasteVolumeValueFactory);
		cuNetDecayWasteVolume.setDisable(true);
		
		SpinnerValueFactory<Double> wholeStemVolume7ValueFactory = 
				new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 2500.00, 332.4, 1);
		wholeStemVolume7.setValueFactory(wholeStemVolume7ValueFactory);
		wholeStemVolume7.setDisable(true);
		
		SpinnerValueFactory<Double> wholeStemVolume12ValueFactory = 
				new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 2500.00, 332.4, 1);
		wholeStemVolume12.setValueFactory(wholeStemVolume12ValueFactory);
		wholeStemVolume12.setDisable(true);
		
		SpinnerValueFactory<Double> cuNetDecayVolumeValueFactory = 
				new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 2500.00, 245.5, 1);
		cuNetDecayVolume.setValueFactory(cuNetDecayVolumeValueFactory);
		cuNetDecayVolume.setDisable(true);		
	}
	
	/**
	 * Switches the application to Scene 5 - ReportInformationTableScene.
	 *
	 * @param event The ActionEvent triggering the scene switch.
	 * @throws IOException If an I/O error occurs during scene loading.
	 */
	public void switchToScene5(ActionEvent event) throws IOException {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/ReportInformationTableScene.fxml"));
		
		ReportInformationTableSceneController controller = new ReportInformationTableSceneController();
	    loader.setController(controller);
	    
		NewTableSceneController.root = loader.load();
		NewTableSceneController.stage = (Stage)((Node)event.getSource()).getScene().getWindow();
		NewTableSceneController.scene = new Scene(NewTableSceneController.root);
		NewTableSceneController.stage.setScene(NewTableSceneController.scene);
		NewTableSceneController.stage.show();
	}
	
	/**
	 * Switches the application to Scene 3 - StandDensityTableScene.
	 *
	 * @param event The ActionEvent triggering the scene switch.
	 * @throws IOException If an I/O error occurs during scene loading.
	 */
	public void switchToScene3(ActionEvent event) throws IOException {
		SiteInformationTableSceneController siteInformationTableSceneController = new SiteInformationTableSceneController();
		siteInformationTableSceneController.switchToScene3(event);
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
