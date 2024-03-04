package com.artur.youtback.model.user;

import jakarta.validation.constraints.Email;
import org.springframework.web.multipart.MultipartFile;

public record UserCreateRequest(@Email String email, String username, String password, MultipartFile picture) {

}
