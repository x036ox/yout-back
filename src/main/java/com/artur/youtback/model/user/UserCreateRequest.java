package com.artur.youtback.model.user;

import org.springframework.web.multipart.MultipartFile;

public record UserCreateRequest(String email, String username, String password, MultipartFile picture) {

}
