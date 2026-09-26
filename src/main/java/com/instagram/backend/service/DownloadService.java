package com.instagram.backend.service;

import com.instagram.backend.config.YtDlpConfig;
import com.instagram.backend.dto.*;
import com.instagram.backend.util.Platform;
import com.instagram.backend.util.UrlValidator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Service
public class DownloadService {

    private final YtDlpService ytDlpService;
    private final YtDlpConfig ytDlpConfig;
    private final VideoInfoService videoInfoService;

    public DownloadService(
            YtDlpService ytDlpService,
            YtDlpConfig ytDlpConfig,
            VideoInfoService videoInfoService) {

        this.ytDlpService = ytDlpService;
        this.ytDlpConfig = ytDlpConfig;
        this.videoInfoService = videoInfoService;
    }

    /**
     * Detect the platform from the supplied URL.
     */
    public PlatformInfoResponse detectPlatform(String url) {

        Platform platform = UrlValidator.detectPlatform(url);

        PlatformInfoResponse response = new PlatformInfoResponse();

        response.setPlatform(
                platform.name().toLowerCase()
        );

        response.setPlatformName(
                platform.getDisplayName()
        );

        response.setSupported(
                platform != Platform.UNKNOWN
        );

        response.setMessage(
                platform == Platform.UNKNOWN
                        ? "Unsupported or invalid URL. Supported platforms: Instagram, YouTube, Facebook, TikTok and X."
                        : platform.getDisplayName() + " URL detected."
        );

        response.setSupportedPlatforms(
                Arrays.stream(Platform.values())
                        .filter(p -> p != Platform.UNKNOWN)
                        .map(Platform::getDisplayName)
                        .toList()
        );

        return response;
    }

    /**
     * Get structured video information.
     *
     * YtDlpService returns raw JSON as String.
     * VideoInfoService converts that JSON into VideoInfoResponse.
     */
    public VideoInfoResponse getVideoInfo(String url) {

        requireSupportedUrl(url);

        return videoInfoService.getVideoInfo(url);
    }

    /**
     * Validate a URL before downloading.
     */
    public DownloadResponse validateUrl(String url) {

        PlatformInfoResponse info = detectPlatform(url);

        return new DownloadResponse(
                info.isSupported() ? "success" : "error",
                info.getMessage(),
                null
        );
    }

    /**
     * Download media using yt-dlp and FFmpeg.
     */
    public DownloadResponse downloadMedia(
            String url,
            String mode,
            String quality,
            String audioFormat) {

        requireSupportedUrl(url);

        String normalizedMode =
                mode == null || mode.isBlank()
                        ? "video"
                        : mode.trim().toLowerCase();

        String normalizedQuality =
                quality == null || quality.isBlank()
                        ? "best"
                        : quality.trim().toLowerCase();

        String normalizedAudioFormat =
                audioFormat == null || audioFormat.isBlank()
                        ? "mp3"
                        : audioFormat.trim().toLowerCase();

        try {

            File downloadedFile =
                    ytDlpService.downloadMedia(
                            url,
                            normalizedMode,
                            normalizedQuality,
                            normalizedAudioFormat
                    );

            if (downloadedFile == null
                    || !downloadedFile.exists()
                    || !downloadedFile.isFile()) {

                return new DownloadResponse(
                        "error",
                        "Media download failed",
                        null
                );
            }

            String fileName =
                    downloadedFile.getName();

            String encoded =
                    URLEncoder.encode(
                            fileName,
                            StandardCharsets.UTF_8
                    );

            DownloadData data =
                    new DownloadData(
                            fileName,
                            downloadedFile.getAbsolutePath(),
                            "/api/download/file?fileName=" + encoded,
                            normalizedMode,
                            normalizedQuality,
                            normalizedAudioFormat
                    );

            return new DownloadResponse(
                    "success",
                    "Media downloaded successfully",
                    data
            );

        } catch (Exception e) {

            String message =
                    e.getMessage() == null
                            ? "Media download failed"
                            : e.getMessage();

            return new DownloadResponse(
                    "error",
                    message,
                    null
            );
        }
    }

    /**
     * Locate a downloaded file safely.
     *
     * Path traversal such as ../ is rejected.
     */
    public Resource getDownloadedFile(String fileName) {

        if (fileName == null
                || fileName.isBlank()
                || fileName.contains("\0")) {

            throw new RuntimeException(
                    "Invalid file name"
            );
        }

        try {

            Path directory =
                    Paths.get(
                                    ytDlpConfig.getDownloadDir()
                            )
                            .toAbsolutePath()
                            .normalize();

            Path requested =
                    directory
                            .resolve(fileName)
                            .normalize();

            /*
             * Prevent path traversal attacks.
             */
            if (!requested.startsWith(directory)
                    || !requested.getFileName()
                    .toString()
                    .equals(fileName)) {

                throw new RuntimeException(
                        "Invalid file path"
                );
            }

            Resource resource =
                    new FileSystemResource(requested);

            if (!resource.exists()
                    || !resource.isReadable()) {

                throw new RuntimeException(
                        "File not found"
                );
            }

            return resource;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to download file: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Check whether the supplied URL belongs
     * to one of the supported platforms.
     */
    private void requireSupportedUrl(String url) {

        if (!UrlValidator.isSupportedUrl(url)) {

            throw new IllegalArgumentException(
                    "Unsupported or invalid URL. "
                            + "Supported platforms: "
                            + "Instagram, YouTube, Facebook, "
                            + "TikTok and X."
            );
        }
    }
}