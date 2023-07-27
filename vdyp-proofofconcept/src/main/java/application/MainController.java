package application;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainController {
	/**
	 * Handles the button action event. This method is triggered when a button is
	 * clicked in the main window.
	 *
	 * @param event The ActionEvent triggered by the button click.
	 * @throws IOException if an I/O error occurs while opening the secondary
	 *                     window.
	 */
	public void handleButtonAction(ActionEvent event) throws IOException {
		openSecondaryWindow(event, true);
	}
	
	public void handleMenuNewFileClick(ActionEvent event) throws IOException {
		// Set up secondary window
		FXMLLoader loader = new FXMLLoader(getClass().getResource("NewTableScene.fxml"));
		Parent secondaryLayout = loader.load();
		Scene secondScene = new Scene(secondaryLayout);			
		secondScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

		// New window (Stage)
		Stage newWindow = new Stage();
		newWindow.setTitle("Model 1");
		Image icon = new Image(getClass().getResource("icon.png").toExternalForm()); // This had to be changed??
		newWindow.getIcons().add(icon);
		newWindow.setScene(secondScene);

		newWindow.show();
	}

	/**
	 * Opens the secondary window based on the given event. The secondary window
	 * will be centered within the primary stage and adjusted to fit within the
	 * screen bounds.
	 *
	 * @param event      The Event that triggers the opening of the secondary
	 *                   window.
	 * @param fromButton A boolean indicating whether the event is from a button
	 *                   click (true) or scene event (false).
	 * @throws IOException if an I/O error occurs while opening the secondary
	 *                     window.
	 */
	public void openSecondaryWindow(Event event, Boolean fromButton) throws IOException {
		// Set up secondary window
		FXMLLoader loader = new FXMLLoader(getClass().getResource("NewTableScene.fxml"));
		Parent secondaryLayout = loader.load();
		Scene secondScene = new Scene(secondaryLayout);
		secondScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

		// New window (Stage)
		Stage newWindow = new Stage();
		newWindow.setTitle("Model 1");
		Image icon = new Image(getClass().getResource("icon.png").toExternalForm()); // This had to be changed??
		newWindow.getIcons().add(icon);
		newWindow.setScene(secondScene);

		// Load in primary window
		Stage primaryStage;
		if (fromButton) { // Conditional based on how it's called
			primaryStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
			

		} else {
			primaryStage = (Stage) ((javafx.scene.Scene) event.getSource()).getWindow();
		}
		/*
		 * Could be encapsulated further : Stage primaryStage = fromButton ?
		 * getPrimaryStageFromButtonEvent(event) : getPrimaryStageFromSceneEvent(event);
		 */

		// Set position and relative size
		setSecondaryWindowPosition(newWindow, primaryStage);
		setSecondaryWindowSize(newWindow, primaryStage);

		newWindow.show();
	}

	/**
	 * Sets the position of the secondary window relative to the primary stage. The
	 * secondary window will be centered within the primary stage and adjusted to
	 * fit within the screen bounds.
	 *
	 * @param newWindow    The Stage representing the secondary window to be
	 *                     positioned.
	 * @param primaryStage The primary Stage that serves as the reference for
	 *                     positioning the secondary window.
	 */
	private void setSecondaryWindowPosition(Stage newWindow, Stage primaryStage) {
		double scaleFactor = 0.6; // Set the desired scale factor (e.g., 80%)

		double primaryWidth = primaryStage.getWidth();
		double primaryHeight = primaryStage.getHeight();

		double secondaryWidth = primaryWidth * scaleFactor;
		double secondaryHeight = primaryHeight * scaleFactor;

		double primaryX = primaryStage.getX();
		double primaryY = primaryStage.getY();
		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds(); // get screen bounds

		double centerX = primaryX + (primaryWidth - secondaryWidth) / 2;
		double centerY = primaryY + (primaryHeight - secondaryHeight) / 2;

		double maxX = screenBounds.getMaxX();
		double maxY = screenBounds.getMaxY();
		double adjustedX = Math.min(Math.max(centerX, 0), maxX - secondaryWidth);
		double adjustedY = Math.min(Math.max(centerY, 0), maxY - secondaryHeight);

		// Set the position of the secondary window
		newWindow.setX(adjustedX);
		newWindow.setY(adjustedY);
	}

	/**
	 * Sets the size of the secondary window based on the scale factor relative to
	 * the primary stage size.
	 *
	 * @param newWindow    The Stage representing the secondary window to set the
	 *                     size.
	 * @param primaryStage The primary Stage used as a reference for sizing the
	 *                     secondary window.
	 */
	private void setSecondaryWindowSize(Stage newWindow, Stage primaryStage) {
		double scaleFactor = 0.6; // Set the desired scale factor (e.g., 80%)

		double primaryWidth = primaryStage.getWidth();
		double primaryHeight = primaryStage.getHeight();

		double secondaryWidth = primaryWidth * scaleFactor;
		double secondaryHeight = primaryHeight * scaleFactor;

		// Set the size of the secondary window
		newWindow.setWidth(secondaryWidth);
		newWindow.setHeight(secondaryHeight);
	}
}
