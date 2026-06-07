#include "carver.h"
#include "signatures.h"
#include <android/log.h>
#include <fstream>
#include <cstring>
#include <algorithm>

#define LOG_TAG "FileCarver"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

FileCarver::FileCarver() {
    initSignatures();
}

FileCarver::~FileCarver() {
    stop();
}

void FileCarver::initSignatures() {
    // JPEG
    signatures.push_back({
        "JPEG",
        {0xFF, 0xD8, 0xFF},
        {0xFF, 0xD9},
        10 * 1024 * 1024,
        "jpg"
    });

    // PNG
    signatures.push_back({
        "PNG",
        {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
        {},
        50 * 1024 * 1024,
        "png"
    });

    // PDF
    signatures.push_back({
        "PDF",
        {0x25, 0x50, 0x44, 0x46},
        {},
        100 * 1024 * 1024,
        "pdf"
    });

    // MP4
    signatures.push_back({
        "MP4",
        {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70},
        {},
        500 * 1024 * 1024,
        "mp4"
    });

    // GIF
    signatures.push_back({
        "GIF",
        {0x47, 0x49, 0x46, 0x38},
        {0x00, 0x3B},
        5 * 1024 * 1024,
        "gif"
    });

    // BMP
    signatures.push_back({
        "BMP",
        {0x42, 0x4D},
        {},
        100 * 1024 * 1024,
        "bmp"
    });

    LOGI("Initialized %zu file signatures", signatures.size());
}

void FileCarver::carveFiles(const std::string& input_path, const std::string& output_dir,
                            size_t max_size, const std::vector<std::string>& file_types) {
    is_running = true;
    recovered_count = 0;

    std::ifstream storage(input_path, std::ios::binary);
    if (!storage.is_open()) {
        LOGE("Cannot open file: %s", input_path.c_str());
        is_running = false;
        return;
    }

    storage.seekg(0, std::ios::end);
    size_t file_size = storage.tellg();
    storage.seekg(0, std::ios::beg);

    if (file_size == 0) {
        LOGE("File is empty");
        storage.close();
        is_running = false;
        return;
    }

    LOGI("File size: %zu bytes, Max carve size: %zu bytes", file_size, max_size);

    // Read file in chunks to avoid memory issues
    const size_t CHUNK_SIZE = 10 * 1024 * 1024; // 10MB chunks
    std::vector<unsigned char> buffer(CHUNK_SIZE);
    size_t offset = 0;

    while (storage.read(reinterpret_cast<char*>(buffer.data()), CHUNK_SIZE) || storage.gcount() > 0) {
        if (!is_running) break;

        size_t bytes_read = storage.gcount();
        LOGI("Processing chunk at offset: %zu (bytes: %zu)", offset, bytes_read);

        // Search for signatures
        for (const auto& sig : signatures) {
            if (!file_types.empty()) {
                if (std::find(file_types.begin(), file_types.end(), sig.name) == file_types.end()) {
                    continue; // Skip if not in requested types
                }
            }

            for (size_t i = 0; i <= bytes_read - sig.header.size(); i++) {
                if (std::memcmp(&buffer[i], sig.header.data(), sig.header.size()) == 0) {
                    LOGI("Found %s header at offset: 0x%zX", sig.name.c_str(), offset + i);

                    size_t start_offset = offset + i;
                    size_t end_offset = start_offset + sig.header.size();
                    size_t max_end = std::min(start_offset + sig.max_size, max_size);

                    // If there's a footer, search for it
                    if (!sig.footer.empty()) {
                        bool found_footer = false;
                        // Search in current chunk and potentially next chunks
                        for (size_t j = i + sig.header.size();
                             j <= bytes_read - sig.footer.size() && (offset + j) < max_end;
                             j++) {
                            if (std::memcmp(&buffer[j], sig.footer.data(), sig.footer.size()) == 0) {
                                end_offset = offset + j + sig.footer.size();
                                found_footer = true;
                                LOGI("Found %s footer at offset: 0x%zX", sig.name.c_str(), offset + j);
                                break;
                            }
                        }
                        if (!found_footer) continue;
                    } else {
                        end_offset = std::min(start_offset + sig.max_size, 
                                            start_offset + (bytes_read - i));
                    }

                    // Write recovered file
                    char output_filename[256];
                    snprintf(output_filename, sizeof(output_filename),
                             "%s/recovered_%d_%zu.%s",
                             output_dir.c_str(),
                             recovered_count.load(),
                             start_offset,
                             sig.extension.c_str());

                    std::ofstream out_file(output_filename, std::ios::binary);
                    if (out_file.is_open()) {
                        size_t file_size_to_write = end_offset - start_offset;
                        if (start_offset + file_size_to_write <= file_size) {
                            // Re-read from original file to ensure we get exact data
                            storage.seekg(start_offset);
                            std::vector<unsigned char> file_data(file_size_to_write);
                            storage.read(reinterpret_cast<char*>(file_data.data()), file_size_to_write);
                            out_file.write(reinterpret_cast<char*>(file_data.data()), file_size_to_write);
                            out_file.close();

                            LOGI("Wrote recovered file: %s (%zu bytes)", output_filename, file_size_to_write);

                            {
                                std::lock_guard<std::mutex> lock(result_mutex);
                                recovered_files.push_back(output_filename);
                                recovered_count++;
                            }
                        }
                    }
                }
            }
        }

        offset += bytes_read;
    }

    storage.close();
    LOGI("Carving complete. Total recovered: %d", recovered_count.load());
    is_running = false;
}

void FileCarver::stop() {
    is_running = false;
    LOGI("Carving stopped");
}

std::vector<std::string> FileCarver::getRecoveredFiles() const {
    std::lock_guard<std::mutex> lock(result_mutex);
    return recovered_files;
}
