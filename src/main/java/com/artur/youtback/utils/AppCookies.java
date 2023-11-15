package com.artur.youtback.utils;

import jakarta.servlet.http.Cookie;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AppCookies {
    public static final String REFRESH_TOKEN = "RefreshToken";

    public static Cookie refreshCookie(String token){
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN, token);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setMaxAge((int)Duration.between(Instant.now(),Instant.now().plus(1, ChronoUnit.DAYS)).toSeconds());
        return refreshCookie;
    }
}
