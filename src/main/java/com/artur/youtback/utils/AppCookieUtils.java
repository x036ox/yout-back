package com.artur.youtback.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class AppCookieUtils {
    public static final String REFRESH_TOKEN = "RefreshToken";

    public static Cookie refreshCookie(String token){
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN, token);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setMaxAge((int)Duration.between(Instant.now(),Instant.now().plus(30, ChronoUnit.DAYS)).toSeconds());
        refreshCookie.setSecure(true);
        refreshCookie.setAttribute("SameSite", "None");
        return refreshCookie;
    }

    public static void removeRefreshCookie(HttpServletRequest request, HttpServletResponse response) throws Exception{
        Cookie refreshCookie = Arrays.stream(request.getCookies()).filter(cookie -> cookie.getName().equals(REFRESH_TOKEN)).findFirst().orElseThrow(() -> new Exception("Cannot find refresh cookie"));
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }
}
