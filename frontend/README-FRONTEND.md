# DoraClip Full React Frontend

Fresh React/Vite frontend built for the existing DoraClip Spring Boot backend.

## Backend endpoints used

GET `/api/test`

POST `/api/download/validate`
```json
{"url":"https://www.youtube.com/watch?v=..."}
```

POST `/api/download/info`
```json
{"url":"https://www.youtube.com/watch?v=..."}
```

POST `/api/download`
```json
{
  "url":"https://www.youtube.com/watch?v=...",
  "mode":"video",
  "quality":"720p",
  "audioFormat":"mp3"
}
```

GET `/api/download/file?fileName=...`

## Local run

Start Spring Boot on port 8080 first.

Then:

```powershell
npm install
npm run dev
```

Open:

`http://localhost:5173`

## Production

Create `.env`:

```text
VITE_API_BASE_URL=https://YOUR-BACKEND-DOMAIN
```

Then:

```powershell
npm run build
```

## Download behavior

The frontend does not fetch the media file into JavaScript memory. It:
1. POSTs to `/api/download`.
2. Reads the returned `data.fileName`.
3. Navigates to `/api/download/file?fileName=...`.
4. Lets the browser download the server response.

This is important for large video files.

## CORS

The backend must allow the frontend origin. For local development:

```text
http://localhost:5173
```

For production, allow the exact production frontend domain.
