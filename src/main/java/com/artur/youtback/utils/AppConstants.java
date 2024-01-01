package com.artur.youtback.utils;

public class AppConstants {
    //options
    public static final int MAX_FIND_ELEMENTS = 10;
    public static final int HLS_FRAGMENT_TIME = 5;
    public static final int POPULARITY_DAYS = 30;
    public static final int MAX_POPULARITY_EXTENSION = 2;
    public static final int MAX_SEARCH_HISTORY_OPTIONS = 10;
    public static final int MAX_VIDEOS_PER_REQUEST = 15;

    //path
    public static final String IMAGE_PATH = "image/";
    public static final String THUMBNAIL_PATH = "thumbnail/";
    public static final String VIDEO_PATH = "video/";

    //profiles
    public static final String SPRING_DEV_PROFILE = "dev";

    //other
    public static final String CLIENT_DOMAIN = "http://localhost:3000/";
    public static final String CLIENT_CONFIRMATION_LINK = CLIENT_DOMAIN + "confirm-email?u=";
}
