package test;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;

public class ImageProcessingService {

    /**
     * Opens a file chooser dialog to select an image file
     * @return Selected File or null if cancelled
     */
    public static File selectImageFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.jpeg", "*.jpg", "*.png"));
        return fileChooser.showOpenDialog(new Stage());
    }

    /**
     * Processes an image file (compression for JPEG, conversion for PNG)
     * @param imagePath Path to the input image
     * @param targetSizeKB Target size in KB (only used for JPEG compression, can be null for PNG)
     * @param onProgress Callback for progress updates
     * @param onComplete Callback for completion with result message
     * @param onError Callback for error handling
     */
    public static void processImage(String imagePath, Integer targetSizeKB, 
                                  Consumer<String> onProgress, 
                                  Consumer<String> onComplete, 
                                  Consumer<String> onError) {
        if (imagePath == null || imagePath.isEmpty()) {
            onError.accept("Please select an image file.");
            return;
        }

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                onProgress.accept("Processing image...");

                String srcImg = imagePath;
                int dotpos = srcImg.lastIndexOf(".");
                String extension = srcImg.substring(dotpos);

                // Determine processing method based on file extension
                if (extension.toLowerCase().equals(".png")) {
                    return convertPngToJpg(srcImg, onProgress);
                } else {
                    if (targetSizeKB == null) {
                        return "Please set the target file size in KB for JPEG compression.";
                    }
                    return compressJpeg(srcImg, targetSizeKB, onProgress);
                }
            }
        };

        task.setOnSucceeded(e -> onComplete.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            onError.accept("Error: " + (exception != null ? exception.getMessage() : "Unknown error"));
        });

        new Thread(task).start();
    }

    private static String convertPngToJpg(String srcImg, Consumer<String> onProgress) {
        try {
            onProgress.accept("Converting PNG to JPG...");

            // Read the PNG image
            BufferedImage pngImage = ImageIO.read(new File(srcImg));

            // Create a new BufferedImage with RGB color model (no transparency)
            BufferedImage jpgImage = new BufferedImage(
                pngImage.getWidth(), 
                pngImage.getHeight(), 
                BufferedImage.TYPE_INT_RGB
            );

            // Draw the PNG onto the JPG image with white background
            Graphics2D g2d = jpgImage.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, jpgImage.getWidth(), jpgImage.getHeight());
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(pngImage, 0, 0, null);
            g2d.dispose();

            // Generate output filename
            int dotpos = srcImg.lastIndexOf(".");
            String destImg = srcImg.substring(0, dotpos) + "_converted.jpg";

            // Write the JPG image
            File output = new File(destImg);
            ImageIO.write(jpgImage, "jpg", output);

            onProgress.accept("Conversion completed!");
            return "PNG converted to JPG successfully!\nSaved as: " + output.getName();
        } catch (IOException e) {
            return "Error converting PNG to JPG: " + e.getMessage();
        }
    }

    private static String compressJpeg(String srcImg, int sizeThreshold, Consumer<String> onProgress) throws IOException {
        onProgress.accept("Starting JPEG compression...");
        
        File file = new File(srcImg);
        long fileSize = file.length();

        if (fileSize / 1024 <= sizeThreshold) {
            return "Image file size is already under threshold (" + fileSize / 1024 + "KB ≤ " + sizeThreshold + "KB)";
        }

        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = iter.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

        BufferedImage originalImage = ImageIO.read(new FileInputStream(file));
        IIOImage image = new IIOImage(originalImage, null, null);

        float quality = 1.0f;
        float step = 0.1f;
        
        // Generate output filename
        int dotpos = srcImg.lastIndexOf(".");
        String destImg = srcImg.substring(0, dotpos) + "_compressed" + srcImg.substring(dotpos);

        while (fileSize / 1024 > sizeThreshold && quality > 0.1f) {
            quality -= step;
            
            onProgress.accept("Compressing... Quality: " + String.format("%.1f", quality * 100) + "%");
            
            File fileOut = new File(destImg);
            if (fileOut.exists()) fileOut.delete();

            try (FileImageOutputStream output = new FileImageOutputStream(fileOut)) {
                writer.setOutput(output);
                params.setCompressionQuality(quality);
                writer.write(null, image, params);
            }

            long newFileSize = fileOut.length();
            if (newFileSize == fileSize) break;
            fileSize = newFileSize;
            
            if (quality <= step) step *= 0.1f;
        }

        writer.dispose();
        onProgress.accept("Compression completed!");
        return "JPEG compressed successfully!\nFinal size: " + fileSize / 1024 + "KB\nSaved as: " + new File(destImg).getName();
    }
}
