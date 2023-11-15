package com.artur.youtback.utils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ImageUtils {
    public final static String IMAGE_FORMAT = "jpg";

    public static void compressAndSave(@NotNull byte[] imageBytes, File outputFile){
        if(outputFile.isDirectory()) return;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
            Thumbnails.of(ImageIO.read(byteArrayInputStream))
                    .size(240, 320)
                    .outputQuality(0.6)
                    .toFile(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
