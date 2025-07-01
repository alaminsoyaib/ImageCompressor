package test;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.io.File;

public class Controller {
    @FXML
    private TextField imagePath;
    @FXML
    private TextField fileSize;
    @FXML
    private TextField done;
    @FXML
    private Button compressButton;

    private HostServices hostServices;

    @FXML
    protected void onImagePathClick(MouseEvent event) {
        done.setVisible(false);
        File image = ImageProcessingService.selectImageFile();
        if (image != null) {
            imagePath.setText(image.toString());
        }
    }

    @FXML
    protected void onCompressClick(MouseEvent event) {
        compressButton.setDisable(true);
        
        Integer targetSize = null;
        if (!fileSize.getText().isEmpty()) {
            try {
                targetSize = Integer.parseInt(fileSize.getText());
            } catch (NumberFormatException e) {
                setDoneText("Invalid file size format");
                compressButton.setDisable(false);
                return;
            }
        }
        
        ImageProcessingService.processImage(
            imagePath.getText(),
            targetSize,
            new ImageProcessingService.ProcessingCallback() {
                @Override
                public void onProgress(String message) {
                    setDoneText(message);
                }
                
                @Override
                public void onComplete(String result) {
                    setDoneText(result);
                    compressButton.setDisable(false);
                }
                
                @Override
                public void onError(String error) {
                    setDoneText(error);
                    compressButton.setDisable(false);
                }
            }
        );
    }

    private void setDoneText(String s) {
        done.setVisible(true);
        done.setText(s);
    }

    protected void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @FXML
    protected void openFollowMeLink() {
        hostServices.showDocument("https://twitter.com/hamidInventions");
    }
}
