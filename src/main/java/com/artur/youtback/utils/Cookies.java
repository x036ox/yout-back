package com.artur.youtback.utils;

import jakarta.servlet.http.Cookie;

public class Cookies {
    public static final String REFRESH_TOKEN = "RefreshToken";

    public static Cookie refreshCookie(String token){
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN, token);
        refreshCookie.setHttpOnly(true);
        return refreshCookie;
    }
}
