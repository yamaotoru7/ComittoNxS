# Windowsのとき
# patch, grep, xargs, sed がインストールされているかを確認する.
if("${CMAKE_HOST_SYSTEM_NAME}" STREQUAL "Windows")
  # Git for Windows のパスを優先的に検索する
  find_program(GIT_EXECUTABLE git.exe)
  if(GIT_EXECUTABLE)
    get_filename_component(GIT_BIN_DIR "${GIT_EXECUTABLE}" DIRECTORY)
    get_filename_component(GIT_ROOT_DIR "${GIT_BIN_DIR}" DIRECTORY)
    set(GIT_USR_BIN "${GIT_ROOT_DIR}/usr/bin")
  endif()

  find_program(PATCH patch.exe HINTS "${GIT_USR_BIN}" "C:/Program Files/Git/usr/bin")
  find_program(GREP grep.exe HINTS "${GIT_USR_BIN}" "C:/Program Files/Git/usr/bin")
  find_program(XARGS xargs.exe HINTS "${GIT_USR_BIN}" "C:/Program Files/Git/usr/bin")
  find_program(SED sed.exe HINTS "${GIT_USR_BIN}" "C:/Program Files/Git/usr/bin")

  if((NOT PATCH) OR (NOT GREP) OR (NOT XARGS) OR (NOT SED))
    message(FATAL_ERROR "patch.exe, grep.exe, xargs.exe, sed.exe のいずれかが見つかりません。Git for Windows をインストールし、C:\\Program Files\\Git\\usr\\bin にパスを通すか、git.exe にパスが通っていることを確認してください。")
  endif()
  get_filename_component(PATCH_DIR "${PATCH}" DIRECTORY)
endif()

# オプションの値が前回のcmake実行から引き継がれないように削除しておく
unset(DOWNLOAD_UNRAR_SOURCES CACHE)
option(
  DOWNLOAD_UNRAR_SOURCES
  "Download UnRAR library, and patch."
  ON
)

if(DOWNLOAD_UNRAR_SOURCES)
  include(FetchContent)
  set(UNRAR_VERSION "7.1.4")
  set(UNRAR_HASH_SHA256 "7f3decbcbf71704ffb3726b9c4e2222f055953310042a9ba0f96b3fb2209971f")

  set(FILE_NAME "unrarsrc-${UNRAR_VERSION}.tar.gz")
  FetchContent_Declare(
    unrar
    URL      https://www.rarlab.com/rar/${FILE_NAME}
    URL_HASH SHA256=${UNRAR_HASH_SHA256}
    DOWNLOAD_NO_EXTRACT true
  )
  FetchContent_MakeAvailable(unrar)

  execute_process(
    COMMAND ${CMAKE_COMMAND} -E tar xfz ${FILE_NAME}
    WORKING_DIRECTORY "${unrar_SOURCE_DIR}"
  )

  set(PATCH_PATH "${CMAKE_CURRENT_SOURCE_DIR}/unrar.patch")

  if("${CMAKE_HOST_SYSTEM_NAME}" STREQUAL "Windows")
    execute_process(
      COMMAND ${CMAKE_COMMAND} -E env "PATH=${PATCH_DIR};$ENV{PATH}" cmd /C "${CMAKE_CURRENT_SOURCE_DIR}/patch-unrar.bat" "${PATCH_PATH}"
      WORKING_DIRECTORY "${unrar_SOURCE_DIR}/unrar"
      RESULT_VARIABLE BUILD_COMMAND_RESULT
      OUTPUT_VARIABLE BUILD_COMMAND_STDOUT
      ERROR_VARIABLE BUILD_COMMAND_STDERR
    )
  else()
    execute_process(
      COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/patch-unrar.sh "${PATCH_PATH}"
      WORKING_DIRECTORY "${unrar_SOURCE_DIR}/unrar"
      RESULT_VARIABLE BUILD_COMMAND_RESULT
      OUTPUT_VARIABLE BUILD_COMMAND_STDOUT
      ERROR_VARIABLE BUILD_COMMAND_STDERR
    )
  endif ()
  # ビルドの実行結果をログ出力する
  message(WARNING "Cmake: unrar: BUILD_COMMAND_RESULT = ${BUILD_COMMAND_RESULT}")
  message(WARNING "Cmake: unrar: BUILD_COMMAND_STDOUT = \n${BUILD_COMMAND_STDOUT}")
  message(WARNING "Cmake: unrar: BUILD_COMMAND_STDERR = \n${BUILD_COMMAND_STDERR}")
  if(NOT BUILD_COMMAND_RESULT EQUAL 0)
    message(FATAL_ERROR "unrar: patch failed (${BUILD_COMMAND_RESULT}).\n${BUILD_COMMAND_STDERR}")
  endif()
else()
  # Use local patched unrar sources
  set(unrar_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR})
endif ()

add_definitions(
  -D_FILE_OFFSET_BITS=64
  -D_LARGEFILE_SOURCE
  #  -DRAR_SMP
  #  -DUNRAR
  #  -DSILENT
)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-dangling-else -Wno-logical-op-parentheses -Wno-switch")

set(
  UNRAR_SOURCES
  ${unrar_SOURCE_DIR}/unrar/blake2s.cpp
  ${unrar_SOURCE_DIR}/unrar/crc.cpp
  ${unrar_SOURCE_DIR}/unrar/crypt.cpp
  ${unrar_SOURCE_DIR}/unrar/getbits.cpp
  ${unrar_SOURCE_DIR}/unrar/hash.cpp
  ${unrar_SOURCE_DIR}/unrar/largepage.cpp
  ${unrar_SOURCE_DIR}/unrar/rarvm.cpp
  ${unrar_SOURCE_DIR}/unrar/rdwrfn.cpp
  ${unrar_SOURCE_DIR}/unrar/rijndael.cpp
  ${unrar_SOURCE_DIR}/unrar/secpassword.cpp
  ${unrar_SOURCE_DIR}/unrar/sha1.cpp
  ${unrar_SOURCE_DIR}/unrar/sha256.cpp
  ${unrar_SOURCE_DIR}/unrar/strfn.cpp
  ${unrar_SOURCE_DIR}/unrar/system.cpp
  ${unrar_SOURCE_DIR}/unrar/timefn.cpp
  ${unrar_SOURCE_DIR}/unrar/unicode.cpp
  ${unrar_SOURCE_DIR}/unrar/unpack.cpp
)

add_library(unrar STATIC ${UNRAR_SOURCES})
include_directories(${unrar_SOURCE_DIR})
target_include_directories(comitton PRIVATE ${unrar_SOURCE_DIR})
target_link_libraries(comitton unrar)
