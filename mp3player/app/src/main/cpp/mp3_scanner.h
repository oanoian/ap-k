#ifndef MP3_SCANNER_H
#define MP3_SCANNER_H

#include <string>
#include <vector>
#include <unordered_map>
#include <memory>
#include <mutex>
#include <atomic>

namespace mp3player {

// Trie node for optimized search
struct TrieNode {
    std::unordered_map<char, std::shared_ptr<TrieNode>> children;
    bool isEndOfWord = false;
    std::vector<std::string> fileIndices;
};

// MP3 File metadata structure (cache-friendly layout)
struct Mp3File {
    std::string path;
    std::string title;
    std::string artist;
    std::string album;
    int64_t duration;  // in milliseconds
    int64_t size;      // in bytes
    int64_t lastModified;
    
    // Cache line padding for alignment
    char padding[64 - ((sizeof(std::string)*3 + sizeof(int64_t)*3) % 64)];
};

// Search result structure
struct SearchResult {
    std::vector<Mp3File> files;
    int64_t searchTimeMs;
    size_t totalScanned;
};

// Main scanner class with heavy optimizations
class Mp3Scanner {
public:
    Mp3Scanner();
    ~Mp3Scanner();
    
    // Folder traversal with multi-threading
    SearchResult scanFolder(const std::string& folderPath);
    
    // Optimized search using Trie
    SearchResult searchFiles(const std::string& query);
    
    // Get all scanned files
    const std::vector<Mp3File>& getAllFiles() const;
    
    // Clear cache and reset
    void clear();
    
    // Preload search index for faster queries
    void buildSearchIndex();
    
private:
    // Internal methods
    void scanDirectoryRecursive(const std::string& path, 
                                std::vector<Mp3File>& files,
                                std::atomic<size_t>& count);
    
    bool isMp3File(const std::string& filename);
    std::string extractTitle(const std::string& filename);
    void insertIntoTrie(const std::string& word, int fileIndex);
    
    // SIMD-optimized string comparison
    bool fastStringMatch(const char* text, const char* pattern);
    
    // Data members
    std::vector<Mp3File> m_files;
    std::shared_ptr<TrieNode> m_searchTrie;
    mutable std::mutex m_mutex;
    std::atomic<bool> m_indexBuilt;
    std::atomic<size_t> m_totalScanned;
};

} // namespace mp3player

// JNI Export functions
extern "C" {
    JNIEXPORT jlong JNICALL Java_com_mp3player_Mp3Scanner_nativeInit(JNIEnv* env, jobject obj);
    JNIEXPORT void JNICALL Java_com_mp3player_Mp3Scanner_nativeDestroy(JNIEnv* env, jobject obj, jlong handle);
    JNIEXPORT jobjectArray JNICALL Java_com_mp3player_Mp3Scanner_nativeScanFolder(JNIEnv* env, jobject obj, jlong handle, jstring path);
    JNIEXPORT jobjectArray JNICALL Java_com_mp3player_Mp3Scanner_nativeSearch(JNIEnv* env, jobject obj, jlong handle, jstring query);
    JNIEXPORT jint JNICALL Java_com_mp3player_Mp3Scanner_nativeGetFileCount(JNIEnv* env, jobject obj, jlong handle);
    JNIEXPORT void JNICALL Java_com_mp3player_Mp3Scanner_nativeBuildIndex(JNIEnv* env, jobject obj, jlong handle);
}

#endif // MP3_SCANNER_H
