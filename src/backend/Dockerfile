FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
    CMD curl --fail --silent --show-error --output /dev/null http://localhost:8080/actuator/health
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
