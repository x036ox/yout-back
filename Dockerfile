FROM openjdk:21-jdk-slim
WORKDIR /yout-back
COPY ./target/yout-back-1.0.jar /yout-back
COPY ./videos-to-create /yout-back/videos-to-create
COPY ./user-pictures-to-create /yout-back/user-pictures-to-create
COPY ./video-thumbnails-to-create /yout-back/video-thumbnails-to-create
EXPOSE 8080
CMD ["java", "-jar", "yout-back-1.0.jar"]