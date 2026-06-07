#ifndef FILECARVER_RECOVERY_CORE_H
#define FILECARVER_RECOVERY_CORE_H

#include <string>
#include <vector>
#include <map>
#include <memory>
#include <atomic>
#include <mutex>

struct FileSignature {
    std::string name;
    std::string extension;
    std::vector<uint8_t> header;
    std::vector<uint8_t> footer;
    size_t min_size;
    size_t max_size;
    bool has_footer;
};

struct RecoveredFile {
    std::string path;
    std::string name;
    std::string type;
    size_t size;
    uint64_t offset;
    int confidence;
    bool is_fragmented;
    std::string status; // COMPLETE, PARTIAL, CORRUPTED
};

struct ScanProgress {
    uint64_t processed_bytes;
    uint64_t total_bytes;
    int files_found;
    int confidence_avg;
    std::string current_operation;
};

class RecoveryCore {
public:
    RecoveryCore();
    ~RecoveryCore();

    // Main recovery operations
    void initSignatures();
    void startScan(const std::string& input_path, const std::string& output_dir, size_t max_scan_size);
    void stopScan();
    
    // Root-based scanning (optional)
    void enableRootMode(bool enable);
    bool hasRootAccess();
    
    // Progress tracking
    ScanProgress getProgress() const;
    std::vector<RecoveredFile> getRecoveredFiles() const;
    
    // Validation
    int validateFile(const std::string& file_path) const;
    bool repairFragmentedFile(const std::string& input, const std::string& output);
    
private:
    std::vector<FileSignature> signatures;
    std::vector<RecoveredFile> recovered_files;
    std::atomic<bool> is_scanning{false};
    std::atomic<uint64_t> processed_bytes{0};
    uint64_t total_bytes{0};
    std::mutex results_mutex;
    bool root_mode{false};
    
    // Core scanning logic
    void scanChunk(const uint8_t* buffer, size_t buffer_size, uint64_t offset, const std::string& output_dir);
    bool matchSignature(const uint8_t* buffer, size_t buffer_size, const FileSignature& sig, size_t& match_offset);
    size_t findFooter(const uint8_t* buffer, size_t buffer_size, const FileSignature& sig, size_t start_offset);
    
    // File validation
    std::string validateFileIntegrity(const RecoveredFile& file);
    int calculateConfidence(const RecoveredFile& file);
};

#endif // FILECARVER_RECOVERY_CORE_H
