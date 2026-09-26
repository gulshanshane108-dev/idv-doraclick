package com.instagram.backend.controller;

import com.instagram.backend.dto.DownloadResponse;
import com.instagram.backend.dto.PlatformInfoResponse;
import com.instagram.backend.dto.VideoInfoResponse;
import com.instagram.backend.service.DownloadService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/download")
public class DownloadController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    /**
     * Validate a URL.
     *
     * POST /api/download/validate
     *
     * Request:
     * {
     *   "url": "https://www.youtube.com/watch?v=..."
     * }
     */
    @PostMapping("/validate")
    public ResponseEntity<DownloadResponse> validateUrl(
            @RequestBody Map<String, String> request) {

        String url = request.get("url");

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new DownloadResponse(
                            "error",
                            "URL is required",
                            null
                    ));
        }

        try {
            DownloadResponse response =
                    downloadService.validateUrl(url);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(new DownloadResponse(
                            "error",
                            e.getMessage() == null
                                    ? "Invalid URL"
                                    : e.getMessage(),
                            null
                    ));
        }
    }

    /**
     * Get video/media information.
     *
     * POST /api/download/info
     *
     * Request:
     * {
     *   "url": "https://www.youtube.com/watch?v=..."
     * }
     */
    @PostMapping("/info")
    public ResponseEntity<?> getVideoInfo(
            @RequestBody Map<String, String> request) {

        String url = request.get("url");

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error",
                            "message", "URL is required"
                    ));
        }

        try {

            VideoInfoResponse response =
                    downloadService.getVideoInfo(url);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error",
                            "message",
                            e.getMessage() == null
                                    ? "Unable to get media information"
                                    : e.getMessage()
                    ));
        }
    }

    /**
     * Download media.
     *
     * POST /api/download
     *
     * Request:
     * {
     *   "url": "...",
     *   "mode": "video",
     *   "quality": "1080p",
     *   "audioFormat": "mp3"
     * }
     */
    @PostMapping
    public ResponseEntity<DownloadResponse> downloadMedia(
            @RequestBody Map<String, String> request) {

        String url = request.get("url");
        String mode = request.get("mode");
        String quality = request.get("quality");
        String audioFormat = request.get("audioFormat");

        if (url == null || url.isBlank()) {

            return ResponseEntity.badRequest()
                    .body(new DownloadResponse(
                            "error",
                            "URL is required",
                            null
                    ));
        }

        try {

            DownloadResponse response =
                    downloadService.downloadMedia(
                            url,
                            mode,
                            quality,
                            audioFormat
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(new DownloadResponse(
                            "error",
                            e.getMessage() == null
                                    ? "Media download failed"
                                    : e.getMessage(),
                            null
                    ));
        }
    }

    /**
     * Stream the downloaded file to the visitor.
     *
     * GET /api/download/file?fileName=...
     *
     * The temporary server-side file is deleted after
     * streaming completes.
     *
     * DownloadFileCleanupService acts as a fallback if
     * the file cannot be deleted immediately.
     */
    @GetMapping("/file")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @RequestParam("fileName") String fileName) {

        Resource resource =
                downloadService.getDownloadedFile(fileName);

        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String filename = resource.getFilename();

        if (filename == null || filename.isBlank()) {
            filename = "download.mp4";
        }

        MediaType mediaType =
                mediaTypeFor(filename);

        final String finalFilename = filename;

        StreamingResponseBody body =
                outputStream -> {

                    Path file = null;

                    try {

                        file = resource.getFile()
                                .toPath()
                                .toAbsolutePath()
                                .normalize();

                        try (InputStream inputStream =
                                     resource.getInputStream()) {

                            byte[] buffer =
                                    new byte[8192];

                            int bytesRead;

                            while ((bytesRead =
                                    inputStream.read(buffer)) != -1) {

                                outputStream.write(
                                        buffer,
                                        0,
                                        bytesRead
                                );
                            }

                            outputStream.flush();
                        }

                    } catch (IOException e) {

                        throw new RuntimeException(
                                "Unable to stream downloaded file",
                                e
                        );

                    } finally {

                        if (file != null) {
                            deleteTemporaryFile(file);
                        }
                    }
                };

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(finalFilename)
                                .build()
                                .toString()
                )
                .body(body);
    }

    /**
     * Determine media type from filename.
     */
    private MediaType mediaTypeFor(String filename) {

        String lowerCase =
                filename.toLowerCase();

        if (lowerCase.endsWith(".mp4")) {
            return MediaType.valueOf("video/mp4");
        }

        if (lowerCase.endsWith(".webm")) {
            return MediaType.valueOf("video/webm");
        }

        if (lowerCase.endsWith(".mkv")) {
            return MediaType.valueOf("video/x-matroska");
        }

        if (lowerCase.endsWith(".mp3")) {
            return MediaType.valueOf("audio/mpeg");
        }

        if (lowerCase.endsWith(".m4a")) {
            return MediaType.valueOf("audio/mp4");
        }

        if (lowerCase.endsWith(".wav")) {
            return MediaType.valueOf("audio/wav");
        }

        if (lowerCase.endsWith(".aac")) {
            return MediaType.valueOf("audio/aac");
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /**
     * Delete the temporary server-side file.
     *
     * Failure here does not break the user's download.
     * DownloadFileCleanupService will remove leftover files.
     */
    private void deleteTemporaryFile(Path file) {

        try {

            if (Files.exists(file)) {

                Files.deleteIfExists(file);

                System.out.println(
                        "[DOWNLOAD CLEANUP] "
                                + "Deleted temporary file: "
                                + file.getFileName()
                );
            }

        } catch (IOException e) {

            System.err.println(
                    "[DOWNLOAD CLEANUP] "
                            + "Could not delete file: "
                            + file.getFileName()
                            + " | "
                            + e.getMessage()
            );
        }
    }
}