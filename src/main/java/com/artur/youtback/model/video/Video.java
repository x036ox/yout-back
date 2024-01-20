package com.artur.youtback.model.video;

import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.VideoMetadata;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.ImageUtils;
import com.artur.youtback.utils.TimeOperations;

import java.io.File;
import java.io.Serializable;
import java.time.*;


public class Video implements Serializable {
        public static final String  DEFAULT_THUMBNAIL = "Prewievs/thumbnail-1.webp";

        private final Long id;
        private final String title;
        private final String duration;
        private final String thumbnail;
        private final String views;
        private final Integer likes;
        private final String uploadDate;
        private final String description;
        private final Long channelId;
        private final String creatorPicture;
        private final String creatorName;
        private final File m3u8;

        public Video(Long id, String title, String duration, String thumbnail, String views, Integer likes, String uploadDate, String description, Long channelId, String creatorPicture, String creatorName, File m3u8) {
                this.id = id;
                this.title = title;
                this.duration = duration;
                this.thumbnail = thumbnail;
                this.views = views;
                this.likes = likes;
                this.uploadDate = uploadDate;
                this.description = description;
                this.channelId = channelId;
                this.creatorPicture = creatorPicture;
                this.creatorName = creatorName;
                this.m3u8 = m3u8;
        }

        public static Video toModel(VideoEntity videoEntity){
                Integer duration = videoEntity.getVideoMetadata().getDuration();

                return new Video(
                        videoEntity.getId(),
                        videoEntity.getTitle(),
                        TimeOperations.seccondsToString(duration,  duration >= 3600 ? "HH:mm:ss" : "mm:ss"),
                        ImageUtils.encodeImageBase64(AppConstants.THUMBNAIL_PATH + videoEntity.getThumbnail()),
                        handleViews(videoEntity.getViews()),
                        videoEntity.getLikes().size(),
                        handleDate(videoEntity.getUploadDate()),
                        videoEntity.getDescription(),
                        videoEntity.getUser().getId(),
                        ImageUtils.encodeImageBase64(AppConstants.IMAGE_PATH + videoEntity.getUser().getPicture()),
                        videoEntity.getUser().getUsername(), null
                );
        }


        public static VideoEntity toEntity(Video video, String videoPath, UserEntity channel){
                return toEntity(video.getTitle(), video.getDescription(), video.thumbnail, videoPath, channel);

        }

        public static VideoEntity toEntity(String title, String description, String thumbnail, String videoPath, UserEntity channel){
                return new VideoEntity(
                        null,
                        title,
                        thumbnail,
                        0,
                        LocalDateTime.now(),
                        description,
                        videoPath,
                        channel
                );
        }


        public static VideoMetadata generateVideoMetadata(){
                return null;
        }


        private static String handleViews(Integer views){
                if(views == 1) return "1 view";
                else return Integer.toString(views).concat(" views");
        }

        private static String handleDate(LocalDateTime uploadDate){
                Period period = Period.between(uploadDate.toLocalDate(), LocalDate.now());
                Duration duration = Duration.between(uploadDate, LocalDateTime.now());
                int years = period.getYears();
                if(years >= 1){
                        return Integer.toString(years).concat(years == 1 ? " year" : " years").concat(" ago");
                }
                int months = period.getMonths();
                if(months >= 1){
                        return Integer.toString(months).concat(months == 1 ? " month" : " months").concat(" ago");
                }
                int days = period.getDays();
                if(days >= 1){
                        return Integer.toString(days).concat(days == 1 ? " day" : " days").concat(" ago");
                }
                int hours = (int)Math.floor(duration.toHours() % 24);
                if(hours >= 1){
                        return Integer.toString(hours).concat(hours == 1 ? " hour" : " hours").concat(" ago");
                }
                int minutes = (int)Math.floor(duration.toMinutes() % 60);
                if(minutes >= 1){
                        return Integer.toString(minutes).concat(minutes == 1 ? " minute" : " minutes").concat(" ago");
                }

                int seconds = (int)Math.ceil(duration.toSeconds() % 60);
                return Integer.toString(seconds).concat(" seconds ago");
        }

        public Long getId() {
                return id;
        }

        public String getTitle() {
                return title;
        }

        public String getDuration() {
                return duration;
        }

        public String getThumbnail() {
                return thumbnail;
        }

        public String getViews() {
                return views;
        }

        public Integer getLikes() {
                return likes;
        }

        public String getUploadDate() {
                return uploadDate;
        }

        public String getDescription() {
                return description;
        }

        public Long getChannelId() {
                return channelId;
        }

        public String getCreatorPicture() {
                return creatorPicture;
        }

        public String getCreatorName() {
                return creatorName;
        }

        public File getM3u8() {
                return m3u8;
        }
}


