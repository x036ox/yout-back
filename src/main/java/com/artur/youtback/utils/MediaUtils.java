package com.artur.youtback.utils;

import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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




}
