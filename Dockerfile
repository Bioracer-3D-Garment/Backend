FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/Backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 3000

CMD ["java", "-jar", "app.jar"]