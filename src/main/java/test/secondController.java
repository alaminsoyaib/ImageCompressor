package test;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.util.Optional;

public class secondController {

    @FXML
    private Button image;

    @FXML
    private Button ProcessButton;

    @FXML
    private Label Modal;

    private File selectedImageFile;

    @FXML
    public void initialize() {
        Modal.setText("Ready to process images");
        ProcessButton.setDisable(true);

        // Set up button click handlers
        image.setOnMouseClicked(this::onSelectImageClick);
        ProcessButton.setOnMouseClicked(this::onProcessClick);
    }

    @FXML
    protected void onSelectImageClick(MouseEvent event) {
        Modal.setText("Selecting image...");

        selectedImageFile = ImageProcessingService.selectImageFile(); // important

        if (selectedImageFile != null) {
            image.setText("Selected: " + selectedImageFile.getName());
            Modal.setText("Image selected: " + selectedImageFile.getName());
            ProcessButton.setDisable(false);
        } else {
            image.setText("Select Image");
            Modal.setText("No image selected");
            ProcessButton.setDisable(true);
        }
    }

    @FXML
    protected void onProcessClick(MouseEvent event) {
        if (selectedImageFile == null) {
            Modal.setText("Please select an image first");
            return;
        }

        ProcessButton.setDisable(true);

        // Determine if we need target size for JPEG compression
        Integer targetSize = null;
        String extension = getFileExtension(selectedImageFile.getName());

        if (!extension.toLowerCase().equals(".png")) {
            // For JPEG files, ask for target size
            TextInputDialog dialog = new TextInputDialog("100");
            dialog.setTitle("JPEG Compression");
            dialog.setHeaderText("Target File Size");
            dialog.setContentText("Enter target file size in KB:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    targetSize = Integer.parseInt(result.get());
                } catch (NumberFormatException e) {
                    Modal.setText("Invalid file size entered");
                    ProcessButton.setDisable(false);
                    return;
                }
            } else {
                Modal.setText("Processing cancelled");
                ProcessButton.setDisable(false);
                return;
            }
        }

        // Process the image using the service
        Modal.setText("Processing started - check terminal for progress...");
        ImageProcessingService.processImage(selectedImageFile.getAbsolutePath(), targetSize);
        ProcessButton.setDisable(false);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex);
        }
        return "";
    }
}
