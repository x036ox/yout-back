package com.artur.youtback.utils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ImageUtils {
    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);
    public final static String IMAGE_FORMAT = "jpg";

    public static void compressAndSave(@NotNull byte[] imageBytes, File outputFile){
        if(outputFile.isDirectory()) return;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
            Thumbnails.of(ImageIO.read(byteArrayInputStream))
                    .size(240, 320)
                    .outputQuality(0.6)
                    .allowOverwrite(true)
                    .toFile(outputFile);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static String encodeImageBase64(String pathname){
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("data:image/");
            sb.append(pathname.substring(pathname.lastIndexOf(".") + 1));
            sb.append(";base64, ");
            byte[] imageBytes = Files.readAllBytes(Path.of(pathname));
            sb.append(Base64.getEncoder().encodeToString(imageBytes));
            return sb.toString();
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }
}
