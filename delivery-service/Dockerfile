FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/delivery-service-1.0.0.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
