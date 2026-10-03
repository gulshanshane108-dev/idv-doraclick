# ==============================
# Frontend build stage (React -> Spring static)
# ==============================
FROM node:22-alpine AS frontend-build

WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./frontend/
RUN cd frontend && npm ci

COPY frontend/ ./frontend/

# Vite outDir is ../src/main/resources/static, so the React build
# lands in /app/src/main/resources/static (created automatically).
RUN cd frontend && npm run build


# ==============================
# Backend build stage (single full-stack JAR)
# ==============================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

# Use the freshly built React UI (overwrites any local static copy).
COPY --from=frontend-build /app/src/main/resources/static ./src/main/resources/static

RUN mvn clean package -DskipTests


# ==============================
# Runtime stage
# ==============================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install FFmpeg and yt-dlp
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       ffmpeg \
       python3 \
       python3-pip \
    && pip3 install --break-system-packages yt-dlp \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copy Spring Boot JAR (contains both API + React UI)
COPY --from=build /app/target/*.jar app.jar

# Download directory
RUN mkdir -p /app/downloads

# Linux tool locations inside this image. These override the Windows
# defaults in application.properties, so Railway needs zero env config.
ENV YTDLP_PATH=/usr/local/bin/yt-dlp \
    FFMPEG_PATH=/usr/bin/ffmpeg \
    YTDLP_DOWNLOAD_DIR=/app/downloads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
