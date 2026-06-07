cmake_minimum_required(VERSION 3.22.1)
project("recovery-core")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -Wall -Wextra")

add_library(recovery-core SHARED
    src/main/cpp/recovery_core.cpp
    src/main/cpp/recovery-jni.cpp
)

target_include_directories(recovery-core PRIVATE
    src/main/cpp/include
)

target_link_libraries(recovery-core
    log
    android
)
