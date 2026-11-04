# Iris Gallery

Iris Gallery is an Android app for browsing, organizing, and editing photos and videos stored on your device. It combines a local-first gallery with optional Telegram sharing and backup tools.

## Features

- Browse photos and videos by timeline, album, and folder; use Iris Gallery as the system image/video picker.
- View and edit media, including image adjustments and EXIF metadata.
- Organize your library with favorites, duplicate review, a trash bin, and a private locked vault.
- Customize the appearance and layout, and use the home-screen photo widget and memories notifications.
- Optionally send selected media to Telegram or configure Telegram bot sync from the TeleDrop Sync screen.

## Privacy

The gallery is designed to work with media on your device. Telegram features are optional and require you to configure a bot; media is sent to Telegram only when you use those features. The app includes network access for Telegram integration, so it is not strictly offline-only.

## Tech stack

- Kotlin and Jetpack Compose with Material 3
- Coil for image loading and Media3 for video playback
- Room and DataStore for local data and preferences
- WorkManager for background Telegram sync

## Build and run

1. Install Android Studio with JDK 21 and Android SDK 35.
2. Clone this repository and open the project in Android Studio:
   ```bash
   git clone https://github.com/Hamza-Houaoui/Iris-Gallery.git
   ```
3. Sync the Gradle project, then run the `app` configuration on an Android device or emulator.

The app's minimum supported Android version is Android 8.0 (API 26).

## Credits and license

The in-app About screen credits the original Iris Gallery project and links to its source. This repository also includes Telegram sync code. Review the in-app credits and the [`LICENSE`](LICENSE) before redistributing the project.
