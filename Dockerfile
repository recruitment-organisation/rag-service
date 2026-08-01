FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --chown=10001:10001 target/*.jar app.jar

USER 10001:10001

EXPOSE 8094

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]