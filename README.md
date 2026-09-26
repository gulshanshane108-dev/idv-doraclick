# DoraClip Backend — Local Development

Spring Boot backend for DoraClip, a multi-platform social-media media downloader.

## Supported platform families

- Instagram
- YouTube
- Facebook
- TikTok
- X / Twitter

Platform recognition is performed by the backend, then yt-dlp performs extraction. Actual extraction availability can change when a source platform changes its delivery or anti-bot behavior.

## API

### Detect platform
`POST /api/download/validate`

```json
{"url":"https://www.youtube.com/watch?v=..."}
```

### Get metadata
`POST /api/download/info`

```json
{"url":"https://www.youtube.com/watch?v=..."}
```

### Download
`POST /api/download`

```json
{
  "url":"https://www.youtube.com/watch?v=...",
  "mode":"video",
  "quality":"720p",
  "audioFormat":"mp3"
}
```

### File
`GET /api/download/file?fileName=...`

### Health/test
`GET /api/test`

## Local setup

1. Install Java 21.
2. Put `yt-dlp.exe` and `ffmpeg.exe` in `tools/`, or set `YTDLP_PATH` and `FFMPEG_PATH`.
3. Run:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080` by default.

The frontend can run on `http://localhost:5173` and is allowed by default CORS.

## Security notes

- No database credentials are stored in source code.
- File download paths are normalized and checked against the configured download directory.
- Download URLs are restricted to the supported platform families before yt-dlp execution.
- Do not download content unless you have the necessary rights/permission and comply with applicable platform terms and laws.

## Temporary download cleanup
Generated media is temporary. The `/api/download/file` endpoint streams the file and deletes it in a `finally` block after the response stream closes. A scheduled cleanup task also removes abandoned files older than `DOWNLOAD_CLEANUP_MAX_AGE_MINUTES` (default 30 minutes), running every `DOWNLOAD_CLEANUP_INTERVAL_MS` (default 5 minutes).
