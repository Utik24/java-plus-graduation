FROM amazoncorretto:21
COPY /stats-server/target/*.jar stat.jar
ENTRYPOINT ["java", "-jar", "/stat.jar"]