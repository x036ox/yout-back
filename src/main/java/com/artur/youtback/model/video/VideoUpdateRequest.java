package com.artur.youtback.model.video;

import org.springframework.web.multipart.MultipartFile;

public record VideoUpdateRequest(Long videoId, String title, String description, String category, MultipartFile video, MultipartFile thumbnail) {
}
