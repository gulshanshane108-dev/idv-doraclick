package com.instagram.backend.service;

import com.instagram.backend.config.YtDlpConfig;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class YtDlpExecutor {

    private final YtDlpConfig config;

    public YtDlpExecutor(YtDlpConfig config) {
        this.config = config;
    }

    public String execute(String url) throws IOException {
        File ytDlp = resolveExecutable(config.getYtDlpPath(), "yt-dlp");

        CommandLine commandLine = new CommandLine(ytDlp);
        commandLine.addArgument("--no-playlist");
        commandLine.addArgument("--dump-single-json");
        commandLine.addArgument(url);

        return run(commandLine);
    }

    public File download(
            String url,
            String outputPath,
            String mode,
            String quality,
            String audioFormat
    ) throws IOException {

        File downloadDirectory = resolveDownloadDirectory();
        ensureDirectory(downloadDirectory);

        File ytDlp = resolveExecutable(config.getYtDlpPath(), "yt-dlp");
        File ffmpeg = resolveExecutable(config.getFfmpegPath(), "FFmpeg");

        String normalizedMode = normalizeMode(mode);
        String normalizedQuality = normalizeQuality(quality);
        String normalizedAudioFormat = normalizeAudioFormat(audioFormat);

        CommandLine commandLine = new CommandLine(ytDlp);

        // Keep the download operation deterministic and prevent playlist downloads.
        commandLine.addArgument("--no-playlist");

        // Do not let a user/global yt-dlp config unexpectedly change this request.
        commandLine.addArgument("--ignore-config");

        commandLine.addArgument("--ffmpeg-location");
        commandLine.addArgument(ffmpeg.getAbsolutePath());

        // Print the final post-processing path so Java can locate the exact file.
        commandLine.addArgument("--print");
        commandLine.addArgument("after_move:filepath");

        if ("audio".equals(normalizedMode)) {
            commandLine.addArgument("-x");
            commandLine.addArgument("--audio-format");
            commandLine.addArgument(normalizedAudioFormat);
            commandLine.addArgument("--audio-quality");
            commandLine.addArgument("0");
        } else {
            commandLine.addArgument("-f");
            commandLine.addArgument(buildVideoFormat(normalizedQuality));
            commandLine.addArgument("--merge-output-format");
            commandLine.addArgument("mp4");
        }

        // Use an absolute output template. This prevents the working directory
        // used by Commons Exec from changing where the file is created.
        String fileNameTemplate = "%(title)s [%(id)s].%(ext)s";
        Path outputTemplate = downloadDirectory.toPath().resolve(fileNameTemplate);

        commandLine.addArgument("-o");
        commandLine.addArgument(outputTemplate.toString());
        commandLine.addArgument(url);

        return executeDownload(commandLine, downloadDirectory);
    }

    private File resolveDownloadDirectory() throws IOException {
        Path path = resolvePath(config.getDownloadDir());
        File directory = path.toFile();

        ensureDirectory(directory);
        return directory;
    }

    private File resolveExecutable(String configuredPath, String name) throws IOException {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IOException(name + " executable path is empty.");
        }

        Path path = resolvePath(configuredPath);
        File executable = path.toFile();

        if (!Files.isRegularFile(path)) {
            throw new IOException(
                    name + " executable not found: " + path.toAbsolutePath()
                            + ". Check application.properties or YTDLP_PATH/FFMPEG_PATH."
            );
        }

        if (!Files.isReadable(path)) {
            throw new IOException(name + " executable is not readable: " + path.toAbsolutePath());
        }

        return executable.getCanonicalFile();
    }

    /**
     * Resolve relative paths against the JVM application directory, not the
     * yt-dlp working directory. This is important because the downloader
     * process runs with the downloads directory as its working directory.
     */
    private Path resolvePath(String configuredPath) {
        Path path = Path.of(configuredPath);

        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir"), configuredPath);
        }

        return path.toAbsolutePath().normalize();
    }

    private void ensureDirectory(File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create download directory: " + directory.getAbsolutePath());
        }

        if (!directory.isDirectory()) {
            throw new IOException("Download path is not a directory: " + directory.getAbsolutePath());
        }
    }

    private String buildVideoFormat(String quality) throws IOException {
        if ("best".equals(quality)) {
            return "bv*+ba/b";
        }

        int height;
        try {
            height = Integer.parseInt(quality.substring(0, quality.length() - 1));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IOException("Invalid video quality: " + quality, e);
        }

        return "bv*[height<=" + height + "]+ba/b[height<=" + height + "]/bv*+ba/b";
    }

    private File executeDownload(
            CommandLine commandLine,
            File downloadDirectory
    ) throws IOException {

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        DefaultExecutor process = DefaultExecutor.builder()
                .setWorkingDirectory(downloadDirectory)
                .get();

        process.setStreamHandler(new PumpStreamHandler(stdout, stderr));

        System.out.println("========================================");
        System.out.println("YT-DLP EXECUTABLE: " + commandLine.getExecutable());
        System.out.println("FFMPEG/WORKING DIRECTORY: " + downloadDirectory.getAbsolutePath());
        System.out.println("YT-DLP COMMAND: " + commandLine);
        System.out.println("========================================");

        final int exitCode;
        try {
            exitCode = process.execute(commandLine);
        } catch (IOException e) {
            String stderrText = decode(stderr);
            String stdoutText = decode(stdout);

            throw new IOException(
                    buildFailureMessage("Could not execute yt-dlp", stderrText, stdoutText),
                    e
            );
        }

        String stdoutText = decode(stdout);
        String stderrText = decode(stderr);

        System.out.println("YT-DLP EXIT CODE: " + exitCode);
        if (!stdoutText.isBlank()) {
            System.out.println("YT-DLP STDOUT:");
            System.out.println(stdoutText);
        }
        if (!stderrText.isBlank()) {
            System.out.println("YT-DLP STDERR:");
            System.out.println(stderrText);
        }
        System.out.println("========================================");

        if (exitCode != 0) {
            throw new IOException(
                    buildFailureMessage(
                            "yt-dlp exited with code " + exitCode,
                            stderrText,
                            stdoutText
                    )
            );
        }

        File result = resolveDownloadedFile(stdoutText, downloadDirectory);

        if (result == null) {
            throw new IOException(
                    "yt-dlp finished successfully, but the downloaded file could not be located. "
                            + "Download directory: " + downloadDirectory.getAbsolutePath()
                            + (stdoutText.isBlank() ? "" : " | stdout: " + stdoutText)
                            + (stderrText.isBlank() ? "" : " | stderr: " + stderrText)
            );
        }

        System.out.println("YT-DLP FINAL FILE: " + result.getAbsolutePath());
        System.out.println("YT-DLP FILE SIZE: " + result.length() + " bytes");
        System.out.println("========================================");

        return result;
    }

    private String run(CommandLine commandLine) throws IOException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        DefaultExecutor process = DefaultExecutor.builder().get();
        process.setStreamHandler(new PumpStreamHandler(stdout, stderr));

        try {
            int exitCode = process.execute(commandLine);
            String output = decode(stdout);
            String error = decode(stderr);

            if (exitCode != 0) {
                throw new IOException(buildFailureMessage(
                        "yt-dlp metadata request failed with code " + exitCode,
                        error,
                        output
                ));
            }

            return output;

        } catch (IOException e) {
            String error = decode(stderr);
            String output = decode(stdout);

            if (e.getMessage() != null && e.getMessage().contains("yt-dlp metadata request failed")) {
                throw e;
            }

            throw new IOException(
                    buildFailureMessage("Could not execute yt-dlp metadata request", error, output),
                    e
            );
        }
    }

    private File resolveDownloadedFile(String stdout, File downloadDirectory) throws IOException {
        if (stdout != null && !stdout.isBlank()) {
            String[] lines = stdout.split("\\R");

            for (int i = lines.length - 1; i >= 0; i--) {
                String candidate = cleanPrintedPath(lines[i]);

                if (candidate.isBlank()) {
                    continue;
                }

                Path candidatePath = Path.of(candidate);
                if (!candidatePath.isAbsolute()) {
                    candidatePath = downloadDirectory.toPath().resolve(candidate);
                }

                candidatePath = candidatePath.toAbsolutePath().normalize();

                if (Files.isRegularFile(candidatePath) && Files.size(candidatePath) > 0) {
                    return candidatePath.toFile().getCanonicalFile();
                }
            }
        }

        // Fallback for yt-dlp versions/configurations that print a path in an
        // unexpected format. Return the newest non-temp regular file.
        try (Stream<Path> files = Files.list(downloadDirectory.toPath())) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> !isTemporaryFile(path))
                    .filter(path -> {
                        try {
                            return Files.size(path) > 0;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .max(Comparator.comparingLong(this::lastModifiedMillis))
                    .map(path -> {
                        try {
                            return path.toFile().getCanonicalFile();
                        } catch (IOException e) {
                            return path.toFile().getAbsoluteFile();
                        }
                    })
                    .orElse(null);
        }
    }

    private String cleanPrintedPath(String value) {
        String path = value == null ? "" : value.trim();

        if (path.length() >= 2
                && ((path.startsWith("\"") && path.endsWith("\""))
                || (path.startsWith("'") && path.endsWith("'")))) {
            path = path.substring(1, path.length() - 1).trim();
        }

        return path;
    }

    private boolean isTemporaryFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".part")
                || name.endsWith(".ytdl")
                || name.endsWith(".temp")
                || name.endsWith(".tmp");
    }

    private long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }

    private String decode(ByteArrayOutputStream stream) {
        return stream.toString(StandardCharsets.UTF_8).trim();
    }

    private String buildFailureMessage(String prefix, String stderr, String stdout) {
        StringBuilder message = new StringBuilder(prefix);

        if (stderr != null && !stderr.isBlank()) {
            message.append(" | stderr: ").append(stderr);
        }

        if (stdout != null && !stdout.isBlank()) {
            message.append(" | stdout: ").append(stdout);
        }

        return message.toString();
    }

    private String normalizeMode(String mode) throws IOException {
        if (mode == null || mode.isBlank()) {
            return "video";
        }

        String value = mode.trim().toLowerCase();

        if (!value.equals("video") && !value.equals("audio")) {
            throw new IOException("Invalid mode. Use video or audio.");
        }

        return value;
    }

    private String normalizeQuality(String quality) throws IOException {
        if (quality == null || quality.isBlank() || quality.equalsIgnoreCase("best")) {
            return "best";
        }

        String value = quality.trim().toLowerCase();

        if (!value.matches("(1080|720|480|360)p")) {
            throw new IOException("Invalid video quality. Use best, 1080p, 720p, 480p or 360p.");
        }

        return value;
    }

    private String normalizeAudioFormat(String audioFormat) throws IOException {
        if (audioFormat == null || audioFormat.isBlank()) {
            return "mp3";
        }

        String value = audioFormat.trim().toLowerCase();

        if (!value.equals("mp3") && !value.equals("m4a")) {
            throw new IOException("Invalid audio format. Use mp3 or m4a.");
        }

        return value;
    }
}
