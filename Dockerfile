FROM openjdk:21
WORKDIR /youtback
COPY "./target/yout-back-0.0.1-SNAPSHOT.jar" /youtback
EXPOSE 8080
CMD ["java", "-jar",  "yout-back-0.0.1-SNAPSHOT.jar"]

