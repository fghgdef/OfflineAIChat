cmake_minimum_required(VERSION 3.22.1)
project("offlineaichat")

# إعدادات بناء خفيفة مناسبة للموبايل (بدون OpenMP/أدوات مو موجودة على أندرويد)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)

# مصدر llama.cpp لازم يكون موجود هون كـ git submodule (راجع README)
add_subdirectory(llama.cpp)

# مكتبتنا الخاصة (الجسر بين Kotlin و llama.cpp)
add_library(offlineaichat SHARED
    llama-bridge.cpp
)

find_library(log-lib log)

target_include_directories(offlineaichat PRIVATE
    llama.cpp/include
    llama.cpp/ggml/include
)

target_link_libraries(offlineaichat
    PRIVATE
    llama
    ggml
    ${log-lib}
    android
)
