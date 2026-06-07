#ifndef FILECARVER_FILE_SIGNATURES_H
#define FILECARVER_FILE_SIGNATURES_H

#include <map>
#include <vector>
#include <string>
#include <cstdint>

struct SignaturePattern {
    std::vector<uint8_t> header;
    std::vector<uint8_t> footer;
    std::string extension;
    size_t min_size;
    size_t max_size;
};

class FileSignatureDatabase {
public:
    static const std::map<std::string, SignaturePattern>& getSignatures() {
        static const std::map<std::string, SignaturePattern> sigs = {
            {"JPEG", {
                {0xFF, 0xD8, 0xFF}, // Header
                {0xFF, 0xD9},       // Footer
                "jpg",
                100,
                10 * 1024 * 1024
            }},
            {"PNG", {
                {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                {},
                "png",
                200,
                50 * 1024 * 1024
            }},
            {"GIF", {
                {0x47, 0x49, 0x46, 0x38},
                {0x00, 0x3B},
                "gif",
                100,
                5 * 1024 * 1024
            }},
            {"PDF", {
                {0x25, 0x50, 0x44, 0x46},
                {},
                "pdf",
                400,
                100 * 1024 * 1024
            }},
            {"MP4", {
                {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70},
                {},
                "mp4",
                500,
                500 * 1024 * 1024
            }},
            {"ZIP", {
                {0x50, 0x4B, 0x03, 0x04},
                {},
                "zip",
                100,
                500 * 1024 * 1024
            }},
            {"BMP", {
                {0x42, 0x4D},
                {},
                "bmp",
                100,
                100 * 1024 * 1024
            }},
            {"WEBP", {
                {0x52, 0x49, 0x46, 0x46}, // RIFF
                {},
                "webp",
                200,
                50 * 1024 * 1024
            }},
            {"DOC", {
                {0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1},
                {},
                "doc",
                4096,
                50 * 1024 * 1024
            }},
            {"DOCX", {
                {0x50, 0x4B, 0x03, 0x04}, // Same as ZIP (DOCX is ZIP)
                {},
                "docx",
                4096,
                50 * 1024 * 1024
            }},
            {"TIFF", {
                {0x49, 0x49, 0x2A, 0x00}, // Little-endian
                {},
                "tiff",
                300,
                100 * 1024 * 1024
            }},
            {"WAV", {
                {0x52, 0x49, 0x46, 0x46}, // RIFF
                {},
                "wav",
                44,
                500 * 1024 * 1024
            }},
            {"SQLITE", {
                {0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66}, // SQLite format
                {},
                "db",
                4096,
                1024 * 1024 * 1024 // 1GB
            }},
        };
        return sigs;
    }
};

#endif // FILECARVER_FILE_SIGNATURES_H
