# CMake configuration for MP3 Player Native Library

cmake_minimum_required(VERSION 3.22.1)

project("mp3player" VERSION 1.0 LANGUAGES CXX)

# C++17 standard with optimizations
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -march=armv8-a+fp+simd")

# Enable NEON optimizations for ARM
if(${ANDROID_ABI} STREQUAL "armeabi-v7a" OR ${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon -mfloat-abi=softfp")
endif()

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

# Create native library
add_library(mp3_scanner SHARED
    mp3_scanner.cpp
)

# Include directories
target_include_directories(mp3_scanner PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Link libraries
target_link_libraries(mp3_scanner
    ${log-lib}
    ${android-lib}
    android
)

# Compiler optimizations
target_compile_options(mp3_scanner PRIVATE
    -Wall
    -Wextra
    -ffast-math
    -funroll-loops
    -fvectorize
    -fslp-vectorize
)

# Define for production builds
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_definitions(mp3_scanner PRIVATE
        NDEBUG
        OPTIMIZED_MODE=1
    )
endif()
