package com.tourguide.image;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagePreprocessorTest {

    private final ImagePreprocessor imagePreprocessor = new ImagePreprocessor();

    @Test
    void preprocess_shouldConvertPngToJpeg() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        byte[] pngBytes = output.toByteArray();

        byte[] result = imagePreprocessor.preprocess(pngBytes);

        assertThat(result).isNotEmpty();
        assertThat(ImageIO.read(new ByteArrayInputStream(result))).isNotNull();
    }

    @Test
    void preprocess_shouldRejectNullInput() {
        assertThatThrownBy(() -> imagePreprocessor.preprocess(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imageBytes must not be null or empty");
    }

    @Test
    void preprocess_shouldRejectEmptyInput() {
        assertThatThrownBy(() -> imagePreprocessor.preprocess(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imageBytes must not be null or empty");
    }
}
