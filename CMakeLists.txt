cmake_minimum_required(VERSION 3.22.1)
project("filecarver")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(filecarver SHARED
    src/main/cpp/native-lib.cpp
    src/main/cpp/carver.cpp
    src/main/cpp/signatures.cpp
)

target_link_libraries(filecarver
    log
    android
)

target_include_directories(filecarver PRIVATE
    src/main/cpp/include
)
