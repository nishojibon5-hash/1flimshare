# Agent Instructions for Flixbuzz OTT

## Automatic APK Build & Upload Rule
Every time you perform any code modification or visual updates to the Flixbuzz application, you **MUST** run the Gradle copy and upload task to generate a fresh APK, publish it, and present the download link to the user.

To build and upload the APK, execute:
```bash
gradle :app:copyApkTask
```
This task automatically duplicates the generated APK, uploads it to `file.io`, and logs the download URL to `build-output/upload_log.txt`.

---

## Last Built APK Release
- **Download Link**: https://tmpfiles.org
- **File Name**: `Flixbuzz_v1.0.apk`
- **Upload Date**: June 30, 2026
