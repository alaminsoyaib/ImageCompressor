package test;

import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
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

        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter jpegExtentionFilter = new FileChooser.ExtensionFilter(
                "Jpeg Images (.jpeg, .jpg)",
                "*.jpeg", "*.jpg");
        FileChooser.ExtensionFilter pngExtentionFilter = new FileChooser.ExtensionFilter(
                "PNG Images (.png)",
                "*.png");
        FileChooser.ExtensionFilter allSupportedFilter = new FileChooser.ExtensionFilter(
                "All Supported Images (.jpeg, .jpg, .png)",
                "*.jpeg", "*.jpg", "*.png");
        fileChooser.getExtensionFilters().addAll(allSupportedFilter, jpegExtentionFilter, pngExtentionFilter);
        File image = fileChooser.showOpenDialog(new Stage());
        if (image != null)
            imagePath.setText(image.toString());
    }

    @FXML
    protected void onCompressClick(MouseEvent event) throws IOException {

        compressButton.setDisable(true);

        if (imagePath.getText().isEmpty() || fileSize.getText().isEmpty()) {
            setDoneText("Choose Image and set Max Size.");
            return;
        }

        String srcImg = imagePath.getText();
        int dotpos = srcImg.lastIndexOf(".");
        String extension = srcImg.substring(dotpos);
        String destImg = srcImg.substring(0, dotpos) + "_compressed" + extension;
        System.out.println("File extension: " + extension);

        // Determine compression method based on file extension
        if (extension.toLowerCase().equals(".png")) {
            compressPngImage(imagePath.getText(), destImg, Integer.parseInt(fileSize.getText()));
        } else {
            reduceImageQuality(imagePath.getText(), destImg, Integer.parseInt(fileSize.getText()));
        }
        compressButton.setDisable(false);
    }

    private void reduceImageQuality(String srcImg, String destImg, int sizeThreshold) throws IOException {

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                float quality = 1.0f;

                File file = new File(srcImg);
                long fileSize = file.length();

                if (fileSize / 1024 <= sizeThreshold) {
                    return "Image file size is under threshold";
                }

                Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
                ImageWriter writer = iter.next();
                ImageWriteParam params = writer.getDefaultWriteParam();
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

                FileInputStream inputStream = new FileInputStream(file);
                BufferedImage originalImage = ImageIO.read(inputStream);
                IIOImage image = new IIOImage(originalImage, null, null);

                double percent = 0.1f;

                while (fileSize / 1024 > sizeThreshold) {
                    if (percent >= quality) {
                        percent = percent * 0.1f;
                    }

                    quality -= percent;

                    File fileOut = new File(destImg);
                    if (fileOut.exists()) {
                        fileOut.delete();
                    }

                    FileImageOutputStream output = new FileImageOutputStream(fileOut);
                    writer.setOutput(output);
                    params.setCompressionQuality(quality);
                    writer.write(null, image, params);

                    File fileOut2 = new File(destImg);
                    long newFileSize = fileOut2.length();
                    if (newFileSize == fileSize) {
                        // cannot reduce more, return
                        break;
                    } else {
                        fileSize = newFileSize;
                    }

                    System.out.println("Quality = " + quality + ", New file size = " + fileSize / 1024 + "KB");
                    output.close();
                }

                writer.dispose();
                return "DONE!";
            }
        };

        task.setOnSucceeded(e -> {
            setDoneText(task.getValue());
        });

        new Thread(task).start();

    }

    private void compressPngImage(String srcImg, String destImg, int sizeThreshold) throws IOException {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                File file = new File(srcImg);
                long fileSize = file.length();

                if (fileSize / 1024 <= sizeThreshold) {
                    return "Image file size is under threshold";
                }

                BufferedImage originalImage = ImageIO.read(file);

                // Try different compression strategies for PNG
                // Strategy 1: Reduce image dimensions while maintaining aspect ratio
                double scaleFactor = Math.sqrt((double) (sizeThreshold * 1024) / fileSize);
                scaleFactor = Math.min(scaleFactor, 1.0); // Don't upscale

                int newWidth = (int) (originalImage.getWidth() * scaleFactor);
                int newHeight = (int) (originalImage.getHeight() * scaleFactor);

                // If scaling won't help much, try other strategies
                if (scaleFactor > 0.9) {
                    // Strategy 2: Convert to indexed color (reduce color palette)
                    return compressPngWithColorReduction(originalImage, destImg, sizeThreshold);
                } else {
                    // Strategy 1: Scale down the image
                    return compressPngWithScaling(originalImage, destImg, newWidth, newHeight, sizeThreshold);
                }
            }
        };

        task.setOnSucceeded(e -> {
            setDoneText(task.getValue());
        });

        new Thread(task).start();
    }

    private String compressPngWithScaling(BufferedImage originalImage, String destImg, int newWidth, int newHeight,
            int sizeThreshold) throws IOException {
        // Create scaled image
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Write the scaled image
        File outputFile = new File(destImg);
        if (outputFile.exists()) {
            outputFile.delete();
        }

        ImageIO.write(scaledImage, "png", outputFile);

        long newFileSize = outputFile.length();
        System.out.println("Scaled PNG - New file size = " + newFileSize / 1024 + "KB");

        if (newFileSize / 1024 <= sizeThreshold) {
            return "DONE! Image scaled to " + newWidth + "x" + newHeight;
        } else {
            // If still too large, try color reduction on the scaled image
            return compressPngWithColorReduction(scaledImage, destImg, sizeThreshold);
        }
    }

    private String compressPngWithColorReduction(BufferedImage originalImage, String destImg, int sizeThreshold)
            throws IOException {
        // Convert to indexed color with different palette sizes
        int[] paletteSizes = { 256, 128, 64, 32, 16, 8 };

        for (int paletteSize : paletteSizes) {
            try {
                // Create indexed color image
                BufferedImage indexedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
                        BufferedImage.TYPE_BYTE_INDEXED);
                Graphics2D g2d = indexedImage.createGraphics();
                g2d.drawImage(originalImage, 0, 0, null);
                g2d.dispose();

                File outputFile = new File(destImg);
                if (outputFile.exists()) {
                    outputFile.delete();
                }

                ImageIO.write(indexedImage, "png", outputFile);

                long newFileSize = outputFile.length();
                System.out.println("Color reduced PNG (palette: " + paletteSize + ") - New file size = "
                        + newFileSize / 1024 + "KB");

                if (newFileSize / 1024 <= sizeThreshold) {
                    return "DONE! Image compressed with " + paletteSize + " color palette";
                }
            } catch (Exception e) {
                System.out.println(
                        "Failed to create indexed image with palette size " + paletteSize + ": " + e.getMessage());
                continue;
            }
        }

        // If color reduction didn't work, try scaling down further
        double scaleFactor = 0.8;
        int newWidth = (int) (originalImage.getWidth() * scaleFactor);
        int newHeight = (int) (originalImage.getHeight() * scaleFactor);

        while (newWidth > 50 && newHeight > 50) {
            try {
                String result = compressPngWithScaling(originalImage, destImg, newWidth, newHeight, sizeThreshold);
                if (result.startsWith("DONE!")) {
                    return result;
                }
            } catch (Exception e) {
                System.out.println("Error during aggressive scaling: " + e.getMessage());
            }

            scaleFactor *= 0.8;
            newWidth = (int) (originalImage.getWidth() * scaleFactor);
            newHeight = (int) (originalImage.getHeight() * scaleFactor);
        }

        return "Could not compress PNG to target size. Try a larger size threshold.";

    }

    private void setDoneText(String s) {
        done.setVisible(true);
        done.setText(s);
    }

    protected void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private HostServices getHostServices() {
        return hostServices;
    }

    @FXML
    protected void openFollowMeLink() {
        getHostServices().showDocument("https://twitter.com/hamidInventions");
    }
}
