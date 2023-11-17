package com.artur.youtback.utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeOperations {

    public static Long toSeconds(Long timeMillis){
        return timeMillis / 1000L;
    }

    public static int toSeconds(String time, String format){
        return LocalTime.parse(time, DateTimeFormatter.ofPattern(format)).toSecondOfDay();
    }

    public static String seccondsToString(int seconds, String format){
        return LocalTime.ofSecondOfDay(seconds).format(DateTimeFormatter.ofPattern(format));
    }
}
