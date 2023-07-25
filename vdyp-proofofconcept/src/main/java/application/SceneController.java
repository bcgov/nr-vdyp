package application;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

public class SceneController implements Initializable {

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

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// Add species choice boxes to the array
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

		// Add "Select species" as the default item for each ChoiceBox and set items
		for (ChoiceBox<String> choiceBox : speciesChoiceBoxes) {
			choiceBox.getItems().addAll(treeSpecies);
			choiceBox.setValue("Select species");
		}

		// Set up listeners for each choice box
		for (int i = 0; i < speciesChoiceBoxes.size(); i++) {
			final int index = i; // Need to capture the correct index in the lambda

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
	}

}
