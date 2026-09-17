FetchContent_Declare(libjxl
        GIT_REPOSITORY https://github.com/libjxl/libjxl
        # タグは付け替えられることがあるので、v0.11.1 のコミットで固定する
        GIT_TAG 794a5dcf0d54f9f0b20d288a12e87afb91d20dfc # v0.11.1
        # デコーダに必要なサブモジュールだけ取得する (testdata などは大きく、ビルドにも不要)
        GIT_SUBMODULES third_party/brotli third_party/highway third_party/skcms
        BINARY_DIR build
        SUBBUILD_DIR subbuild
)

#option(JPEGXL_STATIC "" ON)
option(JPEGXL_FORCE_SYSTEM_BROTLI "" OFF)
option(JPEGXL_FORCE_SYSTEM_HWY "" OFF)
option(BUILD_TESTING "" OFF)
option(JPEGXL_BUNDLE_LIBPNG "" OFF)
option(JPEGXL_ENABLE_VIEWERS "" OFF)
option(JPEGXL_ENABLE_COVERAGE "" OFF)
option(JPEGXL_ENABLE_FUZZERS "" OFF)
option(JPEGXL_ENABLE_DOXYGEN "" OFF)
option(JPEGXL_ENABLE_MANPAGES "" OFF)
option(JPEGXL_ENABLE_EXAMPLES "" OFF)
option(JPEGXL_ENABLE_SJPEG "" OFF)
option(JPEGXL_ENABLE_OPENEXR "" OFF)
option(JPEGXL_ENABLE_TRANSCODE_JPEG "" OFF)
option(JPEGXL_ENABLE_TOOLS "" OFF)
option(JPEGXL_ENABLE_BENCHMARK "" OFF)
option(JPEGXL_ENABLE_JPEGLI "" OFF)
option(JPEGXL_ENABLE_JNI "" OFF)
option(JPEGXL_ENABLE_DEVTOOLS "" OFF)

FetchContent_GetProperties(libjxl)

if(NOT libjxl_POPULATED)
    FetchContent_MakeAvailable(libjxl)
endif()

target_include_directories(comitton PRIVATE
        ${libjxl_BINARY_DIR}
        ${libjxl_SOURCE_DIR}
        $<TARGET_PROPERTY:hwy,INCLUDE_DIRECTORIES>
)

target_link_libraries(comitton
        jxl_dec
        jxl_threads
        brotlidec
        brotlicommon
        hwy
)