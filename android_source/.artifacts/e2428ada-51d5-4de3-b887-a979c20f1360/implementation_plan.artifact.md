# Implement Video Recording synchronized with GPS Logging

This plan outlines the steps to add video recording functionality to `MainActivity.kt`, synchronized with the existing GPS logging feature. The video will be recorded using the back camera and saved in the same location and with the same base name as the GPS log file.

## User Review Required

> [!IMPORTANT]
> The video will be saved in the `Documents` directory (same as the log file) to honor the "same location" request. While `Movies` is standard for videos, saving to `Documents` ensures they are kept together.
>
> [!NOTE]
> I will add a `PreviewView` to the UI so you can see what is being recorded. It will be placed below the start/stop buttons and above the log window.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///home/karl/dev/ekolodning/android_source/gradle/libs.versions.toml)
- Add CameraX versions and libraries:
    - `androidx.camera:camera-camera2`
    - `androidx.camera:camera-lifecycle`
    - `androidx.camera:camera-video`
    - `androidx.camera:camera-view`

#### [MODIFY] [build.gradle.kts (app)](file:///home/karl/dev/ekolodning/android_source/app/build.gradle.kts)
- Add the new CameraX dependencies.

### Android Manifest

#### [MODIFY] [AndroidManifest.xml](file:///home/karl/dev/ekolodning/android_source/app/src/main/AndroidManifest.xml)
- Add `android.permission.CAMERA` and `android.permission.RECORD_AUDIO`.
- Add `<uses-feature android:name="android.hardware.camera.any" />`.

### User Interface

#### [MODIFY] [activity_main.xml](file:///home/karl/dev/ekolodning/android_source/app/src/main/res/layout/activity_main.xml)
- Add a `androidx.camera.view.PreviewView` to provide a camera preview.

### Main Activity Logic

#### [MODIFY] [MainActivity.kt](file:///home/karl/dev/ekolodning/android_source/app/src/main/java/com/karl/ekolodning/MainActivity.kt)
- **Permissions:** Update `checkPermissions()` and `requestPermissions()` to include Camera and Audio.
- **Initialization:** Initialize CameraX `ProcessCameraProvider` and bind the preview and video capture use cases.
- **Start/Stop Synchronization:**
    - In `startLogging()`, initialize and start the video recording using the same timestamp-based filename (with `.mp4` extension).
    - In `stopLogging()`, stop the video recording.
- **Recording Logic:** Use CameraX `VideoCapture` API for recording.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.
- (Manual testing is required for hardware-dependent features like camera and GPS).

### Manual Verification
- Deploy to a physical device.
- Grant Camera, Location, and Audio permissions.
- Press "Start": Verify GPS logging begins and camera preview is active.
- Press "Stop": Verify GPS logging stops and video recording completes.
- Check the `Documents` folder: Verify both the `.log` and `.mp4` files exist with the same name (except extension).
