package com.artur.youtback.utils;

public class AppConstants {
    //options
    public static final int MAX_FIND_ELEMENTS = 10;
    public static final int HLS_FRAGMENT_TIME = 5;
    public static final int POPULARITY_DAYS = 90;
    public static final int MAX_POPULARITY_EXTENSION = 2;
    public static final int MAX_SEARCH_HISTORY_OPTIONS = 10;
    public static final int MAX_VIDEOS_PER_REQUEST = 30;

    //path
    public static final String USER_PATH = "user/";
    public static final String VIDEO_PATH = "video/";

    //profiles
    public static final String SPRING_DEV_PROFILE = "dev";

    //extensions
    public static final String PROFILE_PIC_FILENAME_EXTENSION = ".jpg";
    public static final String IMAGE_FORMAT = "jpg";
    private static final String THUMBNAIL_FORMAT = ".jpg";


    //filenames
    private static final String THUMBNAIL_NAME = "thumbnail";
    public static final String THUMBNAIL_FILENAME = THUMBNAIL_NAME + THUMBNAIL_FORMAT;

    //other
    public static final String CLIENT_DOMAIN = "http://localhost:3000/";
    public static final String CLIENT_CONFIRMATION_LINK = CLIENT_DOMAIN + "confirm-email?u=";

    //kafka related constants
    public static final String VIDEO_INPUT_TOPIC = "video_processor.video.input";
    public static final String VIDEO_OUTPUT_TOPIC = "video_processor.video.output";
    public static final String THUMBNAIL_INPUT_TOPIC = "video-processor.thumbnail.input";
    public static final String THUMBNAIL_OUTPUT_TOPIC = "video-processor.thumbnail.output";
    public static final String USER_PICTURE_INPUT_TOPIC = "video-processor.user-picture.input";
    public static final String USER_PICTURE_OUTPUT_TOPIC = "video-processor.user-picture.output";
}
