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


        private Long id;
        private String title;
        private String duration;
        private String thumbnail;
        private String views;
        private Integer likes;
        private String uploadDate;
        private String description;
        private Long channelId;
        private String creatorPicture;
        private String creatorName;

        public Video(Long id, String title, String duration,String thumbnail, String views, Integer likes, String uploadDate, String description, Long channelId, String creatorPicture, String creatorName) {
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
        }

        public Video(){

        }

        public static Video toModel(VideoEntity videoEntity){
                Integer duration = videoEntity.getVideoMetadata().getDuration();

                return Video.newBuilder()
                        .id(videoEntity.getId())
                        .title(videoEntity.getTitle())
                        .duration(TimeOperations.seccondsToString(duration,  duration >= 3600 ? "HH:mm:ss" : "mm:ss"))
                        .thumbnail(ImageUtils.encodeImageBase64(AppConstants.VIDEO_PATH + videoEntity.getId() + "/" + AppConstants.THUMBNAIL_FILENAME))
                        .views(handleViews(videoEntity.getViews()))
                        .likes(videoEntity.getLikes().size())
                        .uploadDate(handleDate(videoEntity.getUploadDate()))
                        .description(videoEntity.getDescription())
                        .channelId(videoEntity.getUser().getId())
                        .creatorPicture(ImageUtils.encodeImageBase64(videoEntity.getUser().picturePath()))
                        .creatorName(videoEntity.getUser().getUsername())
                        .build();
        }


        public static VideoEntity toEntity(Video video, UserEntity channel){
                return toEntity(video.getTitle(), video.getDescription(), channel);

        }

        public static VideoEntity toEntity(String title, String description, UserEntity channel){
                return new VideoEntity(
                        null,
                        title,
                        0,
                        LocalDateTime.now(),
                        description,
                        channel
                );
        }


        public static VideoBuilder newBuilder(){
                return new DefaultVideoBuilder();
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


        public interface VideoBuilder{

                VideoBuilder id(Long id);
                VideoBuilder title(String title);
                VideoBuilder description(String description);
                VideoBuilder duration(String duration);
                VideoBuilder thumbnail(String thumbnail);
                VideoBuilder views(String views);
                VideoBuilder likes(Integer likes);
                VideoBuilder uploadDate(String uploadDate);
                VideoBuilder channelId(Long channelId);
                VideoBuilder creatorName(String creatorName);
                VideoBuilder creatorPicture(String creatorPicture);
                Video build();
        }

        private static class DefaultVideoBuilder implements VideoBuilder{
                private final Video video;

                public DefaultVideoBuilder(){
                        this.video = new Video();
                }

                @Override
                public VideoBuilder id(Long id) {
                        this.video.id = id;
                        return this;
                }

                @Override
                public VideoBuilder title(String title) {
                        this.video.title = title;
                        return this;
                }

                @Override
                public VideoBuilder description(String description) {
                        this.video.description = description;
                        return this;
                }

                @Override
                public VideoBuilder duration(String duration) {
                        this.video.duration = duration;
                        return this;
                }

                @Override
                public VideoBuilder thumbnail(String thumbnail) {
                        this.video.thumbnail = thumbnail;
                        return this;
                }

                @Override
                public VideoBuilder views(String views) {
                        this.video.views = views;
                        return this;
                }

                @Override
                public VideoBuilder likes(Integer likes) {
                        this.video.likes = likes;
                        return this;
                }

                @Override
                public VideoBuilder uploadDate(String uploadDate) {
                        this.video.uploadDate = uploadDate;
                        return this;
                }

                @Override
                public VideoBuilder channelId(Long channelId) {
                        this.video.channelId = channelId;
                        return this;
                }

                @Override
                public VideoBuilder creatorName(String creatorName) {
                        this.video.creatorName = creatorName;
                        return this;
                }

                @Override
                public VideoBuilder creatorPicture(String creatorPicture) {
                        this.video.creatorPicture = creatorPicture;
                        return this;
                }

                @Override
                public Video build() {
                        return video;
                }


        }
}


