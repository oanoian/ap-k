#include "mp3_scanner.h"
#include <jni.h>
#include <android/log.h>
#include <dirent.h>
#include <sys/stat.h>
#include <algorithm>
#include <chrono>
#include <thread>
#include <cstring>
#include <cctype>

#define LOG_TAG "MP3Scanner"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace mp3player {

Mp3Scanner::Mp3Scanner() 
    : m_searchTrie(std::make_shared<TrieNode>())
    , m_indexBuilt(false)
    , m_totalScanned(0) {
}

Mp3Scanner::~Mp3Scanner() {
    clear();
}

bool Mp3Scanner::isMp3File(const std::string& filename) {
    if (filename.length() < 4) return false;
    
    // Case-insensitive comparison with SIMD optimization hint
    const char* ext = filename.c_str() + filename.length() - 4;
    char lowerExt[5];
    
    #pragma unroll
    for (int i = 0; i < 4; ++i) {
        lowerExt[i] = std::tolower(ext[i]);
    }
    lowerExt[4] = '\0';
    
    return strcmp(lowerExt, ".mp3") == 0;
}

std::string Mp3Scanner::extractTitle(const std::string& filename) {
    // Remove extension
    size_t dotPos = filename.find_last_of('.');
    std::string name = (dotPos != std::string::npos) ? 
                       filename.substr(0, dotPos) : filename;
    
    // Remove common prefixes
    size_t dashPos = name.find(" - ");
    if (dashPos != std::string::npos) {
        name = name.substr(dashPos + 3);
    }
    
    // Trim whitespace
    size_t start = name.find_first_not_of(" \t\n\r");
    size_t end = name.find_last_not_of(" \t\n\r");
    
    if (start == std::string::npos) return "";
    return name.substr(start, end - start + 1);
}

void Mp3Scanner::insertIntoTrie(const std::string& word, int fileIndex) {
    auto node = m_searchTrie;
    
    for (char c : word) {
        char lowerC = std::tolower(c);
        if (node->children.find(lowerC) == node->children.end()) {
            node->children[lowerC] = std::make_shared<TrieNode>();
        }
        node = node->children[lowerC];
    }
    
    node->isEndOfWord = true;
    node->fileIndices.push_back(std::to_string(fileIndex));
}

// SIMD-optimized string matching using basic vectorization
bool Mp3Scanner::fastStringMatch(const char* text, const char* pattern) {
    if (!text || !pattern) return false;
    
    size_t textLen = strlen(text);
    size_t patternLen = strlen(pattern);
    
    if (patternLen > textLen) return false;
    
    // Vectorization-friendly loop
    for (size_t i = 0; i <= textLen - patternLen; ++i) {
        bool match = true;
        
        #pragma omp simd
        for (size_t j = 0; j < patternLen; ++j) {
            if (std::tolower(text[i + j]) != std::tolower(pattern[j])) {
                match = false;
                break;
            }
        }
        
        if (match) return true;
    }
    
    return false;
}

void Mp3Scanner::scanDirectoryRecursive(const std::string& path,
                                        std::vector<Mp3File>& files,
                                        std::atomic<size_t>& count) {
    DIR* dir = opendir(path.c_str());
    if (!dir) {
        LOGE("Failed to open directory: %s", path.c_str());
        return;
    }
    
    struct dirent* entry;
    std::vector<std::string> subdirs;
    
    while ((entry = readdir(dir)) != nullptr) {
        std::string name = entry->d_name;
        
        // Skip . and ..
        if (name == "." || name == "..") continue;
        
        std::string fullPath = path + "/" + name;
        struct stat statbuf;
        
        if (stat(fullPath.c_str(), &statbuf) != 0) continue;
        
        if (S_ISDIR(statbuf.st_mode)) {
            subdirs.push_back(fullPath);
        } else if (S_ISREG(statbuf.st_mode) && isMp3File(name)) {
            Mp3File file;
            file.path = fullPath;
            file.title = extractTitle(name);
            file.artist = "";
            file.album = "";
            file.duration = 0;
            file.size = statbuf.st_size;
            file.lastModified = statbuf.st_mtime;
            
            files.push_back(file);
            count.fetch_add(1, std::memory_order_relaxed);
        }
    }
    
    closedir(dir);
    
    // Process subdirectories (could be parallelized further)
    for (const auto& subdir : subdirs) {
        scanDirectoryRecursive(subdir, files, count);
    }
}

SearchResult Mp3Scanner::scanFolder(const std::string& folderPath) {
    auto startTime = std::chrono::high_resolution_clock::now();
    
    std::lock_guard<std::mutex> lock(m_mutex);
    
    m_files.clear();
    m_files.reserve(1000); // Pre-allocate for performance
    
    std::atomic<size_t> count(0);
    scanDirectoryRecursive(folderPath, m_files, count);
    
    m_totalScanned = count.load();
    
    // Sort files by title for better cache locality
    std::sort(m_files.begin(), m_files.end(),
        [](const Mp3File& a, const Mp3File& b) {
            return a.title < b.title;
        });
    
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
        endTime - startTime).count();
    
    SearchResult result;
    result.files = m_files;
    result.searchTimeMs = duration;
    result.totalScanned = m_totalScanned;
    
    LOGD("Scanned %zu files in %lld ms", m_files.size(), duration);
    
    return result;
}

void Mp3Scanner::buildSearchIndex() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_indexBuilt.load()) return;
    
    m_searchTrie = std::make_shared<TrieNode>();
    
    for (size_t i = 0; i < m_files.size(); ++i) {
        insertIntoTrie(m_files[i].title, i);
        insertIntoTrie(m_files[i].artist, i);
        insertIntoTrie(m_files[i].album, i);
    }
    
    m_indexBuilt.store(true);
    LOGD("Search index built with %zu files", m_files.size());
}

SearchResult Mp3Scanner::searchFiles(const std::string& query) {
    auto startTime = std::chrono::high_resolution_clock::now();
    
    if (query.empty()) {
        SearchResult result;
        result.files = m_files;
        result.searchTimeMs = 0;
        result.totalScanned = m_files.size();
        return result;
    }
    
    std::vector<Mp3File> results;
    results.reserve(100);
    
    // Use Trie for prefix search if index is built
    if (m_indexBuilt.load()) {
        auto node = m_searchTrie;
        bool validPath = true;
        
        for (char c : query) {
            char lowerC = std::tolower(c);
            if (node->children.find(lowerC) == node->children.end()) {
                validPath = false;
                break;
            }
            node = node->children[lowerC];
        }
        
        if (validPath && node->isEndOfWord) {
            for (const auto& idxStr : node->fileIndices) {
                int idx = std::stoi(idxStr);
                if (idx >= 0 && idx < static_cast<int>(m_files.size())) {
                    results.push_back(m_files[idx]);
                }
            }
        }
    } else {
        // Fallback to linear search with SIMD optimization
        for (const auto& file : m_files) {
            if (fastStringMatch(file.title.c_str(), query.c_str()) ||
                fastStringMatch(file.artist.c_str(), query.c_str()) ||
                fastStringMatch(file.album.c_str(), query.c_str())) {
                results.push_back(file);
            }
        }
    }
    
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
        endTime - startTime).count();
    
    SearchResult result;
    result.files = results;
    result.searchTimeMs = duration;
    result.totalScanned = m_files.size();
    
    LOGD("Search found %zu results in %lld ms", results.size(), duration);
    
    return result;
}

const std::vector<Mp3File>& Mp3Scanner::getAllFiles() const {
    return m_files;
}

void Mp3Scanner::clear() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_files.clear();
    m_searchTrie = std::make_shared<TrieNode>();
    m_indexBuilt.store(false);
    m_totalScanned.store(0);
}

} // namespace mp3player

// JNI Implementation
extern "C" {

JNIEXPORT jlong JNICALL Java_com_mp3player_Mp3Scanner_nativeInit(
    JNIEnv* env, jobject obj) {
    mp3player::Mp3Scanner* scanner = new mp3player::Mp3Scanner();
    return reinterpret_cast<jlong>(scanner);
}

JNIEXPORT void JNICALL Java_com_mp3player_Mp3Scanner_nativeDestroy(
    JNIEnv* env, jobject obj, jlong handle) {
    if (handle != 0) {
        delete reinterpret_cast<mp3player::Mp3Scanner*>(handle);
    }
}

JNIEXPORT jobjectArray JNICALL Java_com_mp3player_Mp3Scanner_nativeScanFolder(
    JNIEnv* env, jobject obj, jlong handle, jstring path) {
    
    if (handle == 0) return nullptr;
    
    mp3player::Mp3Scanner* scanner = reinterpret_cast<mp3player::Mp3Scanner*>(handle);
    const char* pathChars = env->GetStringUTFChars(path, nullptr);
    
    auto result = scanner->scanFolder(std::string(pathChars));
    
    env->ReleaseStringUTFChars(path, pathChars);
    
    // Create Java String array
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray resultArray = env->NewObjectArray(
        static_cast<jsize>(result.files.size()), stringClass, nullptr);
    
    for (size_t i = 0; i < result.files.size(); ++i) {
        jstring pathStr = env->NewStringUTF(result.files[i].path.c_str());
        env->SetObjectArrayElement(resultArray, static_cast<jsize>(i), pathStr);
        env->DeleteLocalRef(pathStr);
    }
    
    return resultArray;
}

JNIEXPORT jobjectArray JNICALL Java_com_mp3player_Mp3Scanner_nativeSearch(
    JNIEnv* env, jobject obj, jlong handle, jstring query) {
    
    if (handle == 0) return nullptr;
    
    mp3player::Mp3Scanner* scanner = reinterpret_cast<mp3player::Mp3Scanner*>(handle);
    const char* queryChars = env->GetStringUTFChars(query, nullptr);
    
    auto result = scanner->searchFiles(std::string(queryChars));
    
    env->ReleaseStringUTFChars(query, queryChars);
    
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray resultArray = env->NewObjectArray(
        static_cast<jsize>(result.files.size()), stringClass, nullptr);
    
    for (size_t i = 0; i < result.files.size(); ++i) {
        jstring pathStr = env->NewStringUTF(result.files[i].path.c_str());
        env->SetObjectArrayElement(resultArray, static_cast<jsize>(i), pathStr);
        env->DeleteLocalRef(pathStr);
    }
    
    return resultArray;
}

JNIEXPORT jint JNICALL Java_com_mp3player_Mp3Scanner_nativeGetFileCount(
    JNIEnv* env, jobject obj, jlong handle) {
    
    if (handle == 0) return 0;
    
    mp3player::Mp3Scanner* scanner = reinterpret_cast<mp3player::Mp3Scanner*>(handle);
    return static_cast<jint>(scanner->getAllFiles().size());
}

JNIEXPORT void JNICALL Java_com_mp3player_Mp3Scanner_nativeBuildIndex(
    JNIEnv* env, jobject obj, jlong handle) {
    
    if (handle == 0) return;
    
    mp3player::Mp3Scanner* scanner = reinterpret_cast<mp3player::Mp3Scanner*>(handle);
    scanner->buildSearchIndex();
}

}
