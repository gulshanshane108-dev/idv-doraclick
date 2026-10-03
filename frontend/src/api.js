// Same-origin by default: when VITE_API_BASE_URL is empty/unset,
// requests go to the same host/port that serves the React build
// (single-jar deployment on :8080). Set VITE_API_BASE_URL only when
// the API lives on a different origin (e.g. separate dev servers).
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/+$/, "");

async function readResponse(response) {
  const text = await response.text();
  let body = null;
  try { body = text ? JSON.parse(text) : null; }
  catch { body = text; }

  if (!response.ok) {
    const message =
      body?.message || body?.error || body?.details ||
      (typeof body === "string" ? body : `Request failed (${response.status})`);
    throw new Error(message);
  }
  return body;
}

export async function testBackend() {
  const response = await fetch(`${API_BASE_URL}/api/test`);
  return readResponse(response);
}

export async function validateUrl(url) {
  const response = await fetch(`${API_BASE_URL}/api/download/validate`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify({ url })
  });
  return readResponse(response);
}

export async function getVideoInfo(url) {
  const response = await fetch(`${API_BASE_URL}/api/download/info`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify({ url })
  });
  return readResponse(response);
}

export async function downloadMedia({ url, mode, quality, audioFormat }) {
  const response = await fetch(`${API_BASE_URL}/api/download`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify({
      url,
      mode,
      quality,
      audioFormat
    })
  });
  return readResponse(response);
}

export function downloadFileUrl(fileName) {
  return `${API_BASE_URL}/api/download/file?fileName=${encodeURIComponent(fileName)}`;
}

export { API_BASE_URL };