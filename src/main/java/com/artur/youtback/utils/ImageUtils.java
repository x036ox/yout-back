package com.artur.youtback.utils;

import jakarta.validation.constraints.NotNull;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ImageUtils {
    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

    public static byte[] compressAndSave(@NotNull InputStream inputStream) throws IOException {
        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ){
            Thumbnails.of(inputStream)
                    .size(240, 320)
                    .outputQuality(0.6)
                    .allowOverwrite(true)
                    .toOutputStream(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }


    public static String encodeImageBase64(InputStream inputStream){
        try(inputStream){
            return encodeImageBase64(inputStream.readAllBytes());
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public static String encodeImageBase64(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        sb.append("data:image/");
        sb.append(AppConstants.IMAGE_FORMAT);
        sb.append(";base64, ");
        sb.append(Base64.getEncoder().encodeToString(bytes));
        return sb.toString();
    }
}
