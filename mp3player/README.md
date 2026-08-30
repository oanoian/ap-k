# MP3 Player Android App

A simple Android application that plays MP3 files from your device's Music and Downloads folders.

## Features

- Browse and play MP3 files from device storage
- Play/Pause/Stop controls
- Auto-play next song when current song finishes
- Material Design UI
- Support for Android 5.0 (API 21) and above
- Runtime permissions for Android 6.0+ and Android 13+

## Project Structure

```
mp3player/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/mp3player/
│   │   │   └── MainActivity.java
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## How to Build

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 8 or later
- Android SDK with API 34

### Building with Android Studio
1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the `mp3player` folder
4. Wait for Gradle sync to complete
5. Click Build → Build Bundle(s) / APK(s) → Build APK(s)

### Building with Command Line
```bash
cd mp3player
./gradlew assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## Usage

1. Install the APK on your Android device
2. Grant storage permissions when prompted
3. Add MP3 files to your device's Music or Downloads folder
4. Launch the app
5. Select a song from the list to play
6. Use Play/Pause and Stop buttons to control playback

## Permissions

- `READ_EXTERNAL_STORAGE` - To read MP3 files from storage (Android 5.0-12)
- `READ_MEDIA_AUDIO` - To read audio files (Android 13+)

## License

This project is provided as-is for educational purposes.
