package application;
	
import java.io.IOException;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			// Load the FXML file and create the scene and controller
			FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
			Parent root = loader.load();
			MainController controller = loader.getController();
			Scene scene = new Scene(root,1000,700);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

			// Set up event handler to trigger on shortcut button presses
	        setupEventHandlers(scene, controller);
			
	        // Add icon image and title to the primary stage
	        setStageIconAndTitle(primaryStage);
			
			// Show the scene
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
     * Sets up the event handlers for the specified scene and controller.
     * The event handler listens for "Ctrl + T" key press to trigger the opening of a secondary window.
     * 
     *
     * @param scene The scene to which the event handler will be attached.
     * @param controller The MainController instance responsible for handling events.
     */
	private void setupEventHandlers(Scene scene, MainController controller) {
	    scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
	        @Override
	        public void handle(KeyEvent event) {
	            if (event.isControlDown() && event.getCode() == KeyCode.T) {
	                try {
	                    controller.openSecondaryWindow(event, false); //Setting to false since not trigger by button push
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	    });
	}
	
	/**
     * Sets the icon image and title for the given stage.
     *
     * @param stage The stage to which the icon image and title will be set.
     */
	private void setStageIconAndTitle(Stage stage) {
	    Image icon = new Image(getClass().getResource("icon.png").toExternalForm());
	    stage.getIcons().add(icon);
	    stage.setTitle("WinVDYP7.9 POC");
	}

	public static void main(String[] args) {
		launch(args);
	}
}
