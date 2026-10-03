# DoraClip — Merged Full-Stack (single port)

Spring Boot backend + React frontend in **one** deployable. The Vite build
output lives in `src/main/resources/static`, so the single JAR serves both
the UI and the API on port `8080`. No CORS needed in production.

```
idv-doraclick/
├── frontend/                  React source (Vite). Builds into the folder below.
├── src/main/resources/static/ React production build (generated, committed).
├── src/main/java/.../controller/SpaController.java  SPA fallback (/downloader, /faq, ... -> index.html)
├── Dockerfile                 Node build + Maven build + runtime (one image, port 8080)
└── build-fullstack.ps1        Local one-command merged build
```

## Run merged (one port)

```powershell
# from idv-doraclick/
.\build-fullstack.ps1
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

Open `http://localhost:8080` — UI and API are on the same origin.

## Rebuild only the UI

```powershell
cd frontend
npm install
npm run build   # writes into ../src/main/resources/static
```

Then rebuild the JAR (`mvn package -DskipTests`) or just restart with
`.\mvnw.cmd spring-boot:run` (it serves the fresh static files directly).

## Dev mode (two servers, optional)

```powershell
# terminal 1 — backend
.\mvnw.cmd spring-boot:run
# terminal 2 — frontend with hot reload
cd frontend
npm install
npm run dev     # http://localhost:5173, /api/* proxied to :8080
```

## Deploy (Railway / Docker)

Deploy this folder as one service. Docker builds UI + JAR automatically:

```powershell
docker build -t doraclip .
docker run -p 8080:8080 `
  -e YTDLP_PATH=/usr/local/bin/yt-dlp `
  -e FFMPEG_PATH=/usr/bin/ffmpeg `
  doraclip
```

Set `PORT` if the platform assigns one (already supported via `server.port=${PORT:8080}`).

## Add a new frontend page

1. Add the `Route` in `frontend/src/App.jsx`.
2. Add its path to `SpaController.java` so direct visits / refreshes work.

---

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
