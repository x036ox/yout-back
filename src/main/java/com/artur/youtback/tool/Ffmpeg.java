package com.artur.youtback.tool;

import com.artur.youtback.utils.AppConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

@Component
public class Ffmpeg {


    public File convertVideoToHls(File video) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(buildFfmpegCommand(video));
        Path ffmpegLog = Path.of("logging/ffmpeg.log");
        if(!Files.exists(ffmpegLog)){
            Files.createFile(ffmpegLog);
        }
        processBuilder.redirectError(ffmpegLog.toFile())
                .redirectOutput(ffmpegLog.toFile());
        processBuilder.start().waitFor();
        return new File( StringUtils.stripFilenameExtension(video.getPath()) + ".m3u8");
    }
    private String[] buildFfmpegCommand(File video){
        //result string is going to look like:
        // ffmpeg -i input.mp4 -c:a aac -b:a 128k -c:v libx264 -b:v 1500k -hls_time 10 -hls_list_size 0 -hls_segment_filename "output_%03d.ts" output.m3u8

        String pathWithoutExtension = StringUtils.stripFilenameExtension(video.getAbsolutePath());
        return new String[]{
                "ffmpeg", "-i",
                video.getAbsolutePath(),
                "-c:a", "aac", "-b:a", "128k", "-c:v", "libx264", "-b:v", "1500k", "-hls_time",
                Integer.toString(AppConstants.HLS_FRAGMENT_TIME),
                "-hls_list_size", "0", "-hls_segment_filename",
                pathWithoutExtension + "%03d.ts",
                pathWithoutExtension + ".m3u8"
        };
    }
}
