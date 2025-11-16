# IPCamRecorder Android Project (skeleton)

This project is a starting point for an Android app that:
- Connects to an IP camera (RTSP) via ExoPlayer
- Records stream to MP4 using FFmpegKit
- Supports PTZ (ONVIF/HTTP placeholders), digital zoom, SD-card save via SAF
- Simple motion tracking placeholders and ML Kit hooks
- Google Sign-In + YouTube OAuth flow skeleton for uploading videos to YouTube (resumable upload)

## What is included
- Minimal Android project skeleton with `MainActivity`, `CameraConfigActivity`, `OnvifClient`, `SafStorage`, `Tracker`, and `YouTubeUploaderActivity`.
- `app/build.gradle` lists dependencies (ExoPlayer, FFmpegKit, Firebase Storage optional, Google Sign-In).

## YouTube Upload (high-level steps)
1. Create a Google Cloud Project and enable the YouTube Data API v3.
2. Configure an OAuth 2.0 Client ID for an Android application. Note the `client_id` and configure package name / SHA-1 certificate fingerprint in the console.
3. In the app, request Google Sign-In with the scope `https://www.googleapis.com/auth/youtube.upload` (example in `YouTubeUploaderActivity`).
4. After sign-in, obtain an OAuth access token and perform a **resumable upload**:
   - Send POST to `https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status`
   - Include `Authorization: Bearer ACCESS_TOKEN` and `X-Upload-Content-Length`, `X-Upload-Content-Type`
   - The response will contain `Location` header — the resumable upload URL
   - PUT the binary data to that URL in chunks
5. For security, consider performing uploads through a backend that stores refresh tokens.

## SD-card saving (SAF)
Use `ACTION_OPEN_DOCUMENT_TREE` to let user select a folder on SD card. Persist URI permissions and create files via `DocumentsContract.createDocument`.

## PTZ / ONVIF
The `OnvifClient` provided is a placeholder. Implement WS-Security UsernameToken digest and proper SOAP body for `ContinuousMove` / `AbsoluteMove` per ONVIF spec.

## Building
Open as Android Studio project. Add `google-services.json` if you want Firebase. Configure OAuth in Google Cloud Console for YouTube uploads.

---
