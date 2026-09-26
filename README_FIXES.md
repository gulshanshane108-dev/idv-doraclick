# DoraClip backend - build fix

The previous package had a compile error in `DownloadService#getVideoInfo`:
`YtDlpService#getVideoInfo` returns a `String`, while the controller/service contract requires `VideoInfoResponse`.

This version injects `VideoInfoService` and delegates metadata parsing to it:

`DownloadService -> VideoInfoService -> YtDlpService -> YtDlpExecutor`

No frontend is included in this package.

## Download execution fix

The download executor now resolves relative executable/download paths against the JVM application directory rather than the yt-dlp working directory. This prevents `yt-dlp.exe` from incorrectly being searched under `downloads/tools/` when the process working directory is the downloads directory.

The Windows defaults point to the user's local tools directory and the project's downloads directory, while environment variables remain available for deployment overrides. The executor also validates both executables before starting and reports yt-dlp stdout/stderr and exit code on failure.
