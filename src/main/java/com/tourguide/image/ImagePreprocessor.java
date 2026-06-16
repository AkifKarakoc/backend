package com.tourguide.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Preprocesses uploaded images before they are sent to Google Vision and stored in MinIO.
 *
 * <p>Performs the following steps:
 * <ol>
 *   <li>Decodes the input image (JPEG, PNG, WebP, etc. supported by ImageIO).</li>
 *   <li>Scales down images whose width or height exceeds {@link #MAX_DIMENSION}.</li>
 *   <li>Converts the image to JPEG with configurable quality.</li>
 *   <li>If the resulting JPEG is still larger than {@link #MAX_OUTPUT_SIZE_BYTES},
 *       re-compresses with lower quality until it fits.</li>
 * </ol>
 */
@Slf4j
@Component
public class ImagePreprocessor {

    private static final int MAX_DIMENSION = 2048;
    private static final long MAX_OUTPUT_SIZE_BYTES = 4L * 1024 * 1024;
    private static final float DEFAULT_QUALITY = 0.85f;
    private static final float MIN_QUALITY = 0.3f;
    private static final float QUALITY_STEP = 0.1f;

    /**
     * Preprocesses the given image bytes and returns a JPEG byte array suitable for Google Vision.
     *
     * @param imageBytes raw image bytes
     * @return JPEG encoded bytes
     * @throws IllegalArgumentException if the input is null, empty, or cannot be decoded
     * @throws RuntimeException         if encoding fails
     */
    public byte[] preprocess(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBytes must not be null or empty");
        }

        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) {
                throw new IllegalArgumentException("Unsupported image format or corrupt image");
            }

            BufferedImage scaled = scaleIfNeeded(original);
            byte[] jpeg = writeJpeg(scaled, DEFAULT_QUALITY);

            if (jpeg.length > MAX_OUTPUT_SIZE_BYTES) {
                log.debug("Initial preprocessed image is {} bytes, re-compressing to fit limit", jpeg.length);
                jpeg = compressToFit(scaled);
            }

            log.debug("Preprocessed image from {} bytes to {} bytes", imageBytes.length, jpeg.length);
            return jpeg;
        } catch (IOException e) {
            throw new RuntimeException("Failed to preprocess image", e);
        }
    }

    private BufferedImage scaleIfNeeded(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return image;
        }

        double scale = Math.min((double) MAX_DIMENSION / width, (double) MAX_DIMENSION / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.drawImage(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();
        return scaled;
    }

    private byte[] compressToFit(BufferedImage image) throws IOException {
        float quality = DEFAULT_QUALITY - QUALITY_STEP;
        byte[] result = writeJpeg(image, quality);

        while (result.length > MAX_OUTPUT_SIZE_BYTES && quality > MIN_QUALITY) {
            quality -= QUALITY_STEP;
            result = writeJpeg(image, quality);
        }

        if (result.length > MAX_OUTPUT_SIZE_BYTES) {
            int newWidth = image.getWidth() / 2;
            int newHeight = image.getHeight() / 2;
            BufferedImage smaller = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = smaller.createGraphics();
            graphics.drawImage(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
            graphics.dispose();
            return compressToFit(smaller);
        }

        return result;
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        BufferedImage rgbImage = toRgb(image);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(rgbImage, null, null), param);
        }
        writer.dispose();
        return output.toByteArray();
    }

    private BufferedImage toRgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return rgb;
    }
}
