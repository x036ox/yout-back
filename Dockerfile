FROM openjdk:21-jdk-slim
WORKDIR /yout-back
COPY ./target/yout-back-0.0.2.jar /yout-back
COPY ./videos-to-create /yout-back/videos-to-create
COPY ./user-pictures-to-create /yout-back/user-pictures-to-create
COPY ./video-thumbnails-to-create /yout-back/video-thumbnails-to-create
RUN apt-get update && \
    apt-get install -y ffmpeg
EXPOSE 8080
CMD ["java", "-jar", "yout-back-0.0.2.jar"]