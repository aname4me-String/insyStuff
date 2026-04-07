# Stage 1: Build
FROM gradle:9.2.1-jdk17 AS build
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk update && \
    apk add --no-cache curl dumb-init

RUN adduser -D -s /bin/bash javauser
USER javauser

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["dumb-init", "java", "-jar", "/app/app.jar"]
