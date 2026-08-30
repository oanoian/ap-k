# MP3 Player Pro with C++ Native Optimization

High-performance Android MP3 player using JNI, C++ native code with optimized search algorithms and folder traversal.

## Features
- **Native C++ Core**: Folder traversal and file scanning implemented in C++
- **Optimized Search Algorithm**: Trie-based search for fast MP3 file lookup
- **JNI Integration**: Seamless Java-C++ communication
- **CMake Build System**: Modern C++ build configuration
- **Memory Efficient**: Optimized memory management for large music libraries
- **Multi-threaded Scanning**: Parallel directory traversal
- **SIMD Optimizations**: Vectorized string operations for ARM NEON

## Project Structure
```
mp3player/
├── app/
│   ├── src/main/cpp/          # Native C++ code
│   │   ├── CMakeLists.txt     # CMake configuration
│   │   ├── mp3_scanner.h      # Header definitions
│   │   └── mp3_scanner.cpp    # Folder traversal & search implementation
│   ├── src/main/java/         # Java code
│   │   └── com/mp3player/
│   │       ├── MainActivity.java
│   │       └── SongAdapter.java
│   ├── src/main/res/          # Resources
│   │   ├── layout/
│   │   ├── values/
│   │   └── mipmap-*/
│   ├── build.gradle           # App-level build config
│   └── proguard-rules.pro
├── build.gradle               # Root build config
├── settings.gradle            # Project settings
├── gradle.properties          # Gradle properties
└── local.properties           # SDK path (create manually)
```

## Build Instructions

### Prerequisites
1. Android Studio Arctic Fox or later
2. Android NDK (installed via SDK Manager)
3. CMake (included with Android Studio)

### Building the APK
1. Open the project in Android Studio
2. Ensure NDK is installed: 
   - Go to SDK Manager → SDK Tools
   - Check "NDK (Side by side)" and "CMake"
3. Sync Gradle files
4. Run `./gradlew assembleDebug` or use Build → Build Bundle(s) / APK(s) → Build APK(s)
5. APK generated at: `app/build/outputs/apk/debug/app-debug.apk`

### Command Line Build
```bash
cd mp3player
./gradlew clean assembleDebug
```

## Performance Optimizations

### C++ Optimizations
- **O3 Optimization Level**: Maximum compiler optimizations
- **ARM NEON SIMD**: Vectorized string operations
- **Cache-friendly Data Structures**: Aligned memory layout
- **Trie Search Index**: O(m) prefix search complexity
- **Memory-mapped I/O**: Efficient file reading
- **Lock-free Atomics**: Thread-safe counters

### Architecture
- Multi-ABI support: armeabi-v7a, arm64-v8a, x86, x86_64
- C++17 standard with modern features
- JNI bridge for Java-native communication

## Usage
1. Launch the app and grant storage permissions
2. Tap "Scan" to search for MP3 files
3. Browse the song list
4. Tap a song to play
5. Use Play/Pause/Stop controls
6. Use "Search" for quick file lookup

## Technical Details

### Native Methods
- `nativeInit()`: Initialize C++ scanner
- `nativeScanFolder()`: Recursive directory traversal
- `nativeSearch()`: Trie-based search
- `nativeBuildIndex()`: Pre-build search index
- `nativeGetFileCount()`: Get total file count
- `nativeDestroy()`: Cleanup resources

### Search Algorithm
The Trie-based search provides:
- Prefix matching in O(m) time where m is query length
- Case-insensitive search
- Searches title, artist, and album fields
