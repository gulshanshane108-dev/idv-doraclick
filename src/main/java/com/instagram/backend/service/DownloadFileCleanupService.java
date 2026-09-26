package com.instagram.backend.service;

import com.instagram.backend.config.YtDlpConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@Service
public class DownloadFileCleanupService {

    private final YtDlpConfig config;
    private final long maxAgeMinutes;

    public DownloadFileCleanupService(
            YtDlpConfig config,
            @Value("${yt-dlp.cleanup.max-age-minutes:30}") long maxAgeMinutes) {

        this.config = config;
        this.maxAgeMinutes = Math.max(1, maxAgeMinutes);
    }

    /**
     * Runs automatically every 5 minutes.
     *
     * Deletes downloaded files that are older than
     * the configured maximum age.
     */
    @Scheduled(fixedDelayString = "${yt-dlp.cleanup.interval-ms:300000}")
    public void deleteExpiredFiles() {

        Path downloadDirectory;

        try {
            downloadDirectory = Path.of(config.getDownloadDir())
                    .toAbsolutePath()
                    .normalize();
        } catch (Exception e) {
            System.err.println(
                    "[CLEANUP] Invalid download directory: "
                            + e.getMessage()
            );
            return;
        }

        if (!Files.exists(downloadDirectory)) {
            System.out.println(
                    "[CLEANUP] Download directory does not exist: "
                            + downloadDirectory
            );
            return;
        }

        if (!Files.isDirectory(downloadDirectory)) {
            System.err.println(
                    "[CLEANUP] Download path is not a directory: "
                            + downloadDirectory
            );
            return;
        }

        Instant cutoffTime = Instant.now()
                .minus(Duration.ofMinutes(maxAgeMinutes));

        System.out.println(
                "[CLEANUP] Checking download directory: "
                        + downloadDirectory
        );

        System.out.println(
                "[CLEANUP] Files older than "
                        + maxAgeMinutes
                        + " minutes will be deleted."
        );

        try (Stream<Path> files = Files.list(downloadDirectory)) {

            files.filter(Files::isRegularFile)
                    .forEach(file -> deleteIfExpired(file, cutoffTime));

        } catch (IOException e) {

            System.err.println(
                    "[CLEANUP] Failed to scan download directory: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Deletes one file if it is older than the configured cutoff time.
     */
    private void deleteIfExpired(
            Path file,
            Instant cutoffTime) {

        try {

            FileTime lastModifiedTime =
                    Files.getLastModifiedTime(file);

            Instant lastModified =
                    lastModifiedTime.toInstant();

            if (lastModified.isBefore(cutoffTime)) {

                boolean deleted =
                        Files.deleteIfExists(file);

                if (deleted) {

                    System.out.println(
                            "[CLEANUP] Deleted expired file: "
                                    + file.getFileName()
                    );
                }

            }

        } catch (IOException e) {

            System.err.println(
                    "[CLEANUP] Could not delete file: "
                            + file.getFileName()
                            + " | "
                            + e.getMessage()
            );
        }
    }
}