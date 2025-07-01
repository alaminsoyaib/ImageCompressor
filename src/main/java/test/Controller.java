package test;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.io.File;

public class Controller {
    @FXML
    private TextField imagePath;
    @FXML
    private Button compressButton;

    @FXML
    protected void onImagePathClick(MouseEvent event) {
        File image = ImageProcessingService.selectImageFile();
        if (image != null) {
            imagePath.setText(image.toString());
        }
    }

    @FXML
    protected void onCompressClick(MouseEvent event) {
        Integer targetSize = 500;
        ImageProcessingService.processImage(
                imagePath.getText(),
                targetSize,
                message -> {
                }, // onProgress - currently ignored
                result -> {
                }, // onComplete - currently ignored
                error -> {
                } // onError - currently ignored
        );
    }
}