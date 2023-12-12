package com.artur.youtback.model.user;

import org.springframework.web.multipart.MultipartFile;

public record UserUpdateRequest(Long userId, String email, String password, String username, MultipartFile picture) {
    @Override
    public String toString() {
        return "UserUpdateRequest{" +
                "id=" + userId +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", username='" + username + '\'' +
                ", picture=" + picture +
                '}';
    }
}
