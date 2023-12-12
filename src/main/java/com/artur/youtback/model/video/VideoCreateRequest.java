package com.artur.youtback.model.video;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;


public record VideoCreateRequest(String title, String description, String category, MultipartFile thumbnail, MultipartFile video) {

    @Override
    public String toString() {
        return "VideoCreateRequest{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", thumbnail=" + thumbnail +
                ", video=" + video +
                '}';
    }
}
