package com.artur.youtback.utils;

import java.util.Arrays;


public class FindOptions {

    public enum UserOptions{
        BY_SUBSCRIBERS_MORE_THEN,
        BY_SUBSCRIBERS_LESS_THEN,
        BY_EMAIL,
        BY_ID,
        BY_VIDEO_MORE_THEN,
        BY_VIDEOS_LESS_THEN,
        ADMINS,
        BY_USERNAME,
        MOST_SUBSCRIBERS
    }

    public enum VideoOptions{
        BY_ID,
        MOST_DURATION,
        BY_VIEWS_MORE_THEN,
        BY_VIEWS_LESS_THEN,
        BY_LIKES_MORE_THEN,
        BY_LIKES_LESS_THEN,
        MOST_LIKES,
        MOST_VIEWS,
        BY_TITLE
    }

    public static boolean isUserOptionExists(String option){
        return Arrays.stream(UserOptions.values()).anyMatch((el) -> el.name().equals(option));
    }

    public static boolean isVideoOptionExists(String option){
        return Arrays.stream(VideoOptions.values()).anyMatch((el) -> el.name().equals(option));
    }
}
