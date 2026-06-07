#include "recovery_core.h"
#include <android/log.h>
#include <fstream>
#include <cstring>
#include <algorithm>
#include <thread>
#include <chrono>

#define LOG_TAG "RecoveryCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

RecoveryCore::RecoveryCore() {
    LOGI("RecoveryCore initialized");
}

RecoveryCore::~RecoveryCore() {
    stopScan();
}

void RecoveryCore::initSignatures() {
    // JPEG/JPG
    signatures.push_back({
        "JPEG",
        "jpg",
        {0xFF, 0xD8, 0xFF},
        {0xFF, 0xD9},
        100,
        10 * 1024 * 1024,
        true
    });

    // PNG
    signatures.push_back({
        "PNG",
        "png",
        {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
        {},
        200,
        50 * 1024 * 1024,
        false
    });

    // GIF
    signatures.push_back({
        "GIF",
        "gif",
        {0x47, 0x49, 0x46, 0x38}, // GIF8
        {0x00, 0x3B},
        100,
        5 * 1024 * 1024,
        true
    });

    // PDF
    signatures.push_back({
        "PDF",
        "pdf",
        {0x25, 0x50, 0x44, 0x46}, // %PDF
        {},
        400,
        100 * 1024 * 1024,
        false
    });

    // MP4/MOV
    signatures.push_back({
        "MP4",
        "mp4",
        {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70}, // ftyp
        {},
        500,
        500 * 1024 * 1024,
        false
    });

    // ZIP/APK
    signatures.push_back({
        "ZIP",
        "zip",
        {0x50, 0x4B, 0x03, 0x04}, // PK..
        {},
        100,
        500 * 1024 * 1024,
        false
    });

    // BMP
    signatures.push_back({
        "BMP",
        "bmp",
        {0x42, 0x4D}, // BM
        {},
        100,
        100 * 1024 * 1024,
        false
    });

    // WEBP
    signatures.push_back({
        "WEBP",
        "webp",
        {0x52, 0x49, 0x46, 0x46}, // RIFF (check for WEBP later)
        {},
        200,
        50 * 1024 * 1024,
        false
    });

    LOGI("Initialized %zu file signatures", signatures.size());
}

void RecoveryCore::startScan(const std::string& input_path, const std::string& output_dir, size_t max_scan_size) {
    if (is_scanning.load()) {
        LOGD("Scan already in progress");
        return;
    }

    is_scanning = true;
    processed_bytes = 0;

    std::ifstream input(input_path, std::ios::binary);
    if (!input.is_open()) {
        LOGE("Cannot open input file: %s", input_path.c_str());
        is_scanning = false;
        return;
    }

    input.seekg(0, std::ios::end);
    total_bytes = input.tellg();
    input.seekg(0, std::ios::beg);

    if (total_bytes == 0) {
        LOGE("Input file is empty");
        input.close();
        is_scanning = false;
        return;
    }

    if (max_scan_size > 0) {
        total_bytes = std::min((uint64_t)max_scan_size, total_bytes);
    }

    LOGI("Starting scan: %s (size: %llu bytes)", input_path.c_str(), total_bytes);

    const size_t CHUNK_SIZE = 5 * 1024 * 1024; // 5MB chunks
    std::vector<uint8_t> buffer(CHUNK_SIZE);
    uint64_t offset = 0;

    while (offset < total_bytes && is_scanning.load()) {
        size_t to_read = std::min((uint64_t)CHUNK_SIZE, total_bytes - offset);
        input.read(reinterpret_cast<char*>(buffer.data()), to_read);

        if (input.gcount() > 0) {
            scanChunk(buffer.data(), input.gcount(), offset, output_dir);
            offset += input.gcount();
            processed_bytes = offset;
            LOGD("Scan progress: %llu/%llu (%.1f%%)", offset, total_bytes, (offset * 100.0) / total_bytes);
        } else {
            break;
        }
    }

    input.close();
    is_scanning = false;
    LOGI("Scan complete. Found %zu files", recovered_files.size());
}

void RecoveryCore::stopScan() {
    is_scanning = false;
    LOGI("Scan stopped");
}

void RecoveryCore::enableRootMode(bool enable) {
    root_mode = enable;
    LOGI("Root mode: %s", enable ? "enabled" : "disabled");
}

bool RecoveryCore::hasRootAccess() {
    return root_mode;
}

ScanProgress RecoveryCore::getProgress() const {
    ScanProgress progress;
    progress.processed_bytes = processed_bytes.load();
    progress.total_bytes = total_bytes;
    progress.files_found = recovered_files.size();
    progress.current_operation = "Scanning...";
    return progress;
}

std::vector<RecoveredFile> RecoveryCore::getRecoveredFiles() const {
    std::lock_guard<std::mutex> lock(results_mutex);
    return recovered_files;
}

void RecoveryCore::scanChunk(const uint8_t* buffer, size_t buffer_size, uint64_t offset, const std::string& output_dir) {
    for (const auto& sig : signatures) {
        for (size_t i = 0; i <= buffer_size - sig.header.size(); i++) {
            size_t match_offset = 0;
            if (matchSignature(buffer, buffer_size, sig, match_offset)) {
                LOGI("Found %s at offset 0x%llX", sig.name.c_str(), offset + i);

                uint64_t file_start = offset + i;
                size_t file_size = sig.max_size;

                // Try to find footer if exists
                if (sig.has_footer) {
                    size_t footer_offset = findFooter(buffer, buffer_size, sig, i + sig.header.size());
                    if (footer_offset > 0) {
                        file_size = footer_offset - i + sig.footer.size();
                    }
                }

                RecoveredFile recovered;
                recovered.path = output_dir + "/recovered_" + std::to_string(recovered_files.size()) + "." + sig.extension;
                recovered.name = "recovered_" + std::to_string(recovered_files.size());
                recovered.type = sig.name;
                recovered.size = file_size;
                recovered.offset = file_start;
                recovered.confidence = calculateConfidence(recovered);
                recovered.is_fragmented = false;
                recovered.status = "COMPLETE";

                {
                    std::lock_guard<std::mutex> lock(results_mutex);
                    recovered_files.push_back(recovered);
                }
            }
        }
    }
}

bool RecoveryCore::matchSignature(const uint8_t* buffer, size_t buffer_size, const FileSignature& sig, size_t& match_offset) {
    if (buffer_size < sig.header.size()) return false;

    for (size_t i = 0; i <= buffer_size - sig.header.size(); i++) {
        if (std::memcmp(&buffer[i], sig.header.data(), sig.header.size()) == 0) {
            match_offset = i;
            return true;
        }
    }
    return false;
}

size_t RecoveryCore::findFooter(const uint8_t* buffer, size_t buffer_size, const FileSignature& sig, size_t start_offset) {
    if (!sig.has_footer || sig.footer.empty()) return 0;

    for (size_t i = start_offset; i <= buffer_size - sig.footer.size(); i++) {
        if (std::memcmp(&buffer[i], sig.footer.data(), sig.footer.size()) == 0) {
            return i;
        }
    }
    return 0;
}

int RecoveryCore::calculateConfidence(const RecoveredFile& file) {
    int confidence = 100;
    
    // Reduce confidence for fragmented files
    if (file.is_fragmented) confidence -= 20;
    
    // Reduce for partially corrupted
    if (file.status == "PARTIAL") confidence -= 30;
    
    // Very low for corrupted
    if (file.status == "CORRUPTED") confidence = 20;

    return std::max(0, std::min(100, confidence));
}

int RecoveryCore::validateFile(const std::string& file_path) const {
    // TODO: Implement actual validation logic
    // Check file headers, footers, checksums
    return 100; // Placeholder
}

bool RecoveryCore::repairFragmentedFile(const std::string& input, const std::string& output) {
    // TODO: Implement fragment stitching logic
    return false; // Placeholder
}
