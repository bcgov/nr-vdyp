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
	
    public void handleButtonAction(ActionEvent event) throws IOException {
    		openSecondaryWindow(event, true);
    }
   
    public void openSecondaryWindow(Event event, Boolean button) throws IOException {
    	// Set up secondary window
    	FXMLLoader loader = new FXMLLoader(getClass().getResource("scene.fxml"));
        Parent secondaryLayout = loader.load();
        Scene secondScene = new Scene(secondaryLayout);
        secondScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

        // New window (Stage)
        Stage newWindow = new Stage();
        newWindow.setTitle("Model 1");
        Image icon = new Image("icon.png");
		newWindow.getIcons().add(icon);
        newWindow.setScene(secondScene);

        // Load in primary window
        Stage primaryStage;
        if(button) { // Conditional based on how it's called
        	primaryStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        } else {
        	primaryStage = (Stage) ((javafx.scene.Scene) event.getSource()).getWindow();
        }
        
        // Adjust the size of the second scene based on the primary stage's size
        double scaleFactor = 0.6; // Set the desired scale factor (e.g., 80%)
        double primaryWidth = primaryStage.getWidth();
        double primaryHeight = primaryStage.getHeight();
        double secondaryWidth = primaryWidth * scaleFactor;
        double secondaryHeight = primaryHeight * scaleFactor;
        
        // Calculate position of second window, related to primary window.
        double primaryX = primaryStage.getX();
        double primaryY = primaryStage.getY();
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double centerX = primaryX + (primaryWidth - secondaryWidth) / 2;
        double centerY = primaryY + (primaryHeight - secondaryHeight) / 2;
        double maxX = screenBounds.getMaxX();
        double maxY = screenBounds.getMaxY();
        double adjustedX = Math.min(Math.max(centerX, 0), maxX - secondaryWidth);
        double adjustedY = Math.min(Math.max(centerY, 0), maxY - secondaryHeight);
        
        //Set Position
        newWindow.setX(adjustedX);
        newWindow.setY(adjustedY);
        
        // Set Relative Size
        newWindow.setWidth(secondaryWidth);
        newWindow.setHeight(secondaryHeight);


        newWindow.show();
    }
}
