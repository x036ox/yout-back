package com.artur.youtback.model;

import com.artur.youtback.entity.user.UserEntity;
import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.entity.VideoMetadata;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.ImageUtils;
import com.artur.youtback.utils.TimeOperations;

import java.io.Serializable;
import java.time.*;


public record Video(
        Long id,
        String title,
        String duration,
        String thumbnail,
        String views,
        Integer likes,
        String uploadDate,
        String description,
        Long channelId,
        String creatorPicture,
        String creatorName
) implements Serializable {
        public static final String  DEFAULT_THUMBNAIL = "Prewievs/thumbnail-1.webp";

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
                        videoEntity.getUser().getUsername()
                );
        }


        public static VideoEntity toEntity(Video video, String videoPath, UserEntity channel){
                return toEntity(video.title(), video.description(), video.thumbnail, videoPath, channel);

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
}


