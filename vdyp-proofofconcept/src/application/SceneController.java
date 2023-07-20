package application;


import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;



public class SceneController implements Initializable {
	
	//Set up species choice boxes
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
	
	//Set of trees
	String[] treeSpecies = {"AC - Popular", "AT - Aspen", "B - True Fir", "BA - Amabilis Fir","BG - Grand Fir", "BL - Alpine Fir", "CW - Western Red Cedar", "DR - Red Alder",
							"E - Birch", "EA - Alaska Paper Birch", "EP - Common Paper Birch", "FD - Douglas Fir", "H - Hemlock", "HM - Mountain Hemlock", "HW - Western Hemlock",
							"L - Larch","LA - Alpine Larch", "LT - Tamarack", "LW - Western Larch", "MB - Bigleaf Maple","PA - Whitebark Pine", "PF - Limber Pine", "PJ - Jack Pine",
							"PL - Lodgepole Pine", "PY - Western White Pine", "PY - Ponderosa (Yellow) Pine", "S - Spruce", "SB - Black Spruce","SE - Engelmann Spruce", "SS - Sitka Spruce",
							"SW - White Spruce", "YC - Yellow Cedar"
							};
	
	String currentSpecies_1;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// Set Choices for choice box
		species_1.getItems().addAll(treeSpecies);
		species_2.getItems().addAll(treeSpecies);
		species_3.getItems().addAll(treeSpecies);
		species_4.getItems().addAll(treeSpecies);
		species_5.getItems().addAll(treeSpecies);
		species_6.getItems().addAll(treeSpecies);
		
	    // Add "Select species" as the default item for each ChoiceBox
	    species_1.setValue("Select species");
	    species_2.setValue("Select species");
	    species_3.setValue("Select species");
	    species_4.setValue("Select species");
	    species_5.setValue("Select species");
	    species_6.setValue("Select species");
	    

	    species_1.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				currentSpecies_1 = species_1.getSelectionModel().getSelectedItem();
			}
			
		}); 
	}
	
}

  