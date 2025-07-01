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
        FileChooser.ExtensionFilter allSupportedFilter = new FileChooser.ExtensionFilter(
                "All Supported Images (.jpeg, .jpg, .png)",
                "*.jpeg", "*.jpg", "*.png");
        fileChooser.getExtensionFilters().addAll(allSupportedFilter);
        File image = fileChooser.showOpenDialog(new Stage());
        if (image != null)
            imagePath.setText(image.toString());
    }

    @FXML
    protected void onCompressClick(MouseEvent event) throws IOException {

        compressButton.setDisable(true);

        if (imagePath.getText().isEmpty()) {
            setDoneText("Please choose an image file.");
            return;
        }

        String srcImg = imagePath.getText();
        int dotpos = srcImg.lastIndexOf(".");
        String extension = srcImg.substring(dotpos);
        String destImg = srcImg.substring(0, dotpos) + "_compressed" + extension;
        System.out.println("File extension: " + extension);

        // Determine compression method based on file extension
        if (extension.toLowerCase().equals(".png")) {
            convertPngToJpg(srcImg, srcImg.substring(0, dotpos) + "_converted.jpg");
        } else {
            if (fileSize.getText().isEmpty()) {
                setDoneText("Please set the target file size in KB for compression.");
                return;
            }
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

    private void convertPngToJpg(String srcImg, String destImg) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    // Read the PNG image
                    BufferedImage pngImage = ImageIO.read(new File(srcImg));

                    // Create a new BufferedImage with RGB color model (no transparency)
                    BufferedImage jpgImage = new BufferedImage(
                            pngImage.getWidth(),
                            pngImage.getHeight(),
                            BufferedImage.TYPE_INT_RGB);

                    // Draw the PNG onto the JPG image with white background
                    Graphics2D g2d = jpgImage.createGraphics();
                    g2d.setColor(java.awt.Color.WHITE);
                    g2d.fillRect(0, 0, jpgImage.getWidth(), jpgImage.getHeight());
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2d.drawImage(pngImage, 0, 0, null);
                    g2d.dispose();

                    // Write the JPG image
                    File output = new File(destImg);
                    ImageIO.write(jpgImage, "jpg", output);

                    return "PNG converted to JPG successfully!";
                } catch (IOException e) {
                    return "Error converting PNG to JPG: " + e.getMessage();
                }
            }
        };

        task.setOnSucceeded(e -> setDoneText(task.getValue()));
        new Thread(task).start();
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
