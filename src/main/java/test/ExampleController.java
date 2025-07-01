package test;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

import java.io.File;

/**
 * Example of another controller using the ImageProcessingService
 * This demonstrates how easy it is to reuse the image processing functionality
 */
public class ExampleController {
    
    @FXML
    private Button selectBtn;
    
    @FXML
    private Label statusLabel;
    
    private File selectedFile;
    
    @FXML
    public void initialize() {
        statusLabel.setText("Ready");
    }
    
    @FXML
    protected void onSelectImage(MouseEvent event) {
        selectedFile = ImageProcessingService.selectImageFile();
        if (selectedFile != null) {
            statusLabel.setText("Selected: " + selectedFile.getName());
            // Auto-process PNG files, ask for target size for JPEG
            processImage();
        } else {
            statusLabel.setText("No file selected");
        }
    }
    
    private void processImage() {
        if (selectedFile == null) return;
        
        // For this example, we'll auto-process with default settings
        Integer targetSize = selectedFile.getName().toLowerCase().endsWith(".png") ? null : 100;
        
        ImageProcessingService.processImage(
            selectedFile.getAbsolutePath(),
            targetSize,
            new ImageProcessingService.ProcessingCallback() {
                @Override
                public void onProgress(String message) {
                    statusLabel.setText("Processing: " + message);
                }
                
                @Override
                public void onComplete(String result) {
                    statusLabel.setText("Done: " + result);
                }
                
                @Override
                public void onError(String error) {
                    statusLabel.setText("Error: " + error);
                }
            }
        );
    }
}
