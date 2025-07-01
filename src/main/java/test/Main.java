package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        controller.setHostServices(getHostServices());
        primaryStage.setTitle("Image Compression (JPEG/PNG)");
        primaryStage.setScene(new Scene(root, 350, 275));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}