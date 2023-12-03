package com.artur.youtback.utils;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Arrays;

public class MediaUtils {


    public static Metadata getMetadata(File file) throws IOException, TikaException, SAXException {
        try(InputStream inputStream = new FileInputStream(file)) {
            return getMetadata(inputStream);
        }
    }

    public static Metadata getMetadata(MultipartFile file) throws IOException, TikaException, SAXException {
        try(InputStream inputStream = file.getInputStream()) {
            return getMetadata(inputStream);
        }
    }

    public static Metadata getMetadata(InputStream inputStream) throws TikaException, IOException, SAXException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        parser.parse(inputStream, handler, metadata);
        return metadata;
    }

    public static String getDuration(Metadata metadata){
        return metadata.get("xmpDM:duration");
    }

    public static String getDuration(File file) throws TikaException, IOException, SAXException {
        return getMetadata(file).get("xmpDM:duration");
    }

    public static String getDuration(MultipartFile file) throws TikaException, IOException, SAXException {
        return getMetadata(file).get("xmpDM:duration");
    }

    public static File convertVideoToHls(File video) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(buildFfmpegCommand(video).split(" "));
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        return new File( StringUtils.stripFilenameExtension(video.getPath()) + ".m3u8");
    }

    private static String buildFfmpegCommand(File video){
        //result string is going to look like:
        // ffmpeg -i input.mp4 -c:a aac -b:a 128k -c:v libx264 -b:v 1500k -hls_time 10 -hls_list_size 0 -hls_segment_filename "output_%03d.ts" output.m3u8

        StringBuilder command = new StringBuilder();
        command.append("ffmpeg -i ")
                .append(video.getAbsolutePath())
                .append(" -c:a aac -b:a 128k -c:v libx264 -b:v 1500k -hls_time ")
                .append(AppConstants.HLS_FRAGMENT_TIME)
                .append(" -hls_list_size 0 -hls_segment_filename ")
                .append(StringUtils.stripFilenameExtension(video.getAbsolutePath()))
                .append("_%03d.ts ")
                .append(StringUtils.stripFilenameExtension(video.getAbsolutePath()))
                .append(".m3u8");
        return command.toString();
    }
}