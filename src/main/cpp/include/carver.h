#ifndef FILECARVER_CARVER_H
#define FILECARVER_CARVER_H

#include <string>
#include <vector>
#include <mutex>
#include <atomic>

struct FileSignature {
    std::string name;
    std::vector<unsigned char> header;
    std::vector<unsigned char> footer;
    size_t max_size;
    std::string extension;
};

class FileCarver {
private:
    std::vector<FileSignature> signatures;
    std::vector<std::string> recovered_files;
    std::mutex result_mutex;
    std::atomic<bool> is_running{false};
    std::atomic<int> recovered_count{0};

public:
    FileCarver();
    ~FileCarver();

    void initSignatures();
    void carveFiles(const std::string& input_path, const std::string& output_dir, 
                    size_t max_size, const std::vector<std::string>& file_types);
    void stop();
    int getRecoveredCount() const { return recovered_count.load(); }
    std::vector<std::string> getRecoveredFiles() const;
};

#endif // FILECARVER_CARVER_H
