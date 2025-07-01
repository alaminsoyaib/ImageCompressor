# Image Processing Service Usage Guide

## Overview
The `ImageProcessingService` class provides a reusable way to handle image processing functionality across multiple screens/controllers in your JavaFX application.

## Features
- **File Selection**: Unified image file selection dialog
- **PNG to JPG Conversion**: Converts PNG files to JPG format with white background
- **JPEG Compression**: Compresses JPEG files to target size
- **Progress Callbacks**: Real-time progress updates and result notifications
- **Error Handling**: Comprehensive error handling with user-friendly messages

## Basic Usage

### 1. File Selection
```java
File selectedFile = ImageProcessingService.selectImageFile();
if (selectedFile != null) {
    // File was selected
    System.out.println("Selected: " + selectedFile.getName());
}
```

### 2. Processing Images
```java
ImageProcessingService.processImage(
    imagePath,                    // String: Path to the image file
    targetSizeKB,                // Integer: Target size in KB (null for PNG conversion)
    new ImageProcessingService.ProcessingCallback() {
        @Override
        public void onProgress(String message) {
            // Update UI with progress message
            statusLabel.setText(message);
        }
        
        @Override
        public void onComplete(String result) {
            // Handle successful completion
            statusLabel.setText(result);
        }
        
        @Override
        public void onError(String error) {
            // Handle errors
            statusLabel.setText("Error: " + error);
        }
    }
);
```

## Complete Controller Example

```java
public class YourController {
    @FXML private Button selectButton;
    @FXML private Label statusLabel;
    
    private File selectedFile;
    
    @FXML
    protected void onSelectClick() {
        selectedFile = ImageProcessingService.selectImageFile();
        if (selectedFile != null) {
            statusLabel.setText("Selected: " + selectedFile.getName());
        }
    }
    
    @FXML
    protected void onProcessClick() {
        if (selectedFile == null) {
            statusLabel.setText("Please select a file first");
            return;
        }
        
        // For JPEG files, specify target size; for PNG, use null
        Integer targetSize = selectedFile.getName().toLowerCase().endsWith(".png") ? null : 100;
        
        ImageProcessingService.processImage(
            selectedFile.getAbsolutePath(),
            targetSize,
            new ImageProcessingService.ProcessingCallback() {
                @Override
                public void onProgress(String message) {
                    Platform.runLater(() -> statusLabel.setText(message));
                }
                
                @Override
                public void onComplete(String result) {
                    Platform.runLater(() -> statusLabel.setText(result));
                }
                
                @Override
                public void onError(String error) {
                    Platform.runLater(() -> statusLabel.setText("Error: " + error));
                }
            }
        );
    }
}
```

## File Processing Behavior

### PNG Files
- Automatically converted to JPG format
- Transparency removed (replaced with white background)
- Output file: `originalname_converted.jpg`
- No compression applied
- `targetSizeKB` parameter is ignored

### JPEG Files
- Compressed to target file size
- Quality reduced iteratively until target size is reached
- Output file: `originalname_compressed.jpg`
- `targetSizeKB` parameter is required

## Thread Safety
- All processing runs in background threads
- Use `Platform.runLater()` when updating UI from callbacks
- Callbacks are called from background threads

## Error Handling
The service handles various error conditions:
- Invalid file paths
- Unsupported file formats
- IO exceptions during processing
- Invalid target size values

## Benefits of Using This Service
1. **Reusability**: Use the same functionality across multiple screens
2. **Consistency**: Unified behavior and error handling
3. **Maintainability**: Single place to update image processing logic
4. **Progress Feedback**: Real-time updates to keep users informed
5. **Thread Safety**: Background processing doesn't block the UI
