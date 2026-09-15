include_guard(GLOBAL)

if(DEFINED MESON_EXECUTABLE AND EXISTS "${MESON_EXECUTABLE}")
    return()
endif()

find_program(_system_meson NAMES meson meson.py meson.cmd meson.bat)
if(_system_meson)
    set(MESON_EXECUTABLE "${_system_meson}" CACHE FILEPATH "Meson launcher" FORCE)
    return()
endif()

find_package(Python3 REQUIRED COMPONENTS Interpreter)

set(_meson_site_packages "${CMAKE_BINARY_DIR}/python-tools")
execute_process(
    COMMAND "${Python3_EXECUTABLE}" -c "import mesonbuild"
    RESULT_VARIABLE _meson_import_result
    OUTPUT_QUIET
    ERROR_QUIET
)

if(NOT _meson_import_result EQUAL 0)
    file(MAKE_DIRECTORY "${_meson_site_packages}")
    execute_process(
        COMMAND
            "${Python3_EXECUTABLE}" -m pip install
            --disable-pip-version-check
            --no-warn-script-location
            --target "${_meson_site_packages}"
            meson
        RESULT_VARIABLE _meson_install_result
        OUTPUT_VARIABLE _meson_install_stdout
        ERROR_VARIABLE _meson_install_stderr
    )

    if(NOT _meson_install_result EQUAL 0)
        message(FATAL_ERROR
            "Failed to install Meson into ${_meson_site_packages}.\n"
            "stdout:\n${_meson_install_stdout}\n"
            "stderr:\n${_meson_install_stderr}\n"
            "Install Meson manually or ensure pip can access PyPI."
        )
    endif()
endif()

set(_meson_wrapper_py "${CMAKE_BINARY_DIR}/meson-wrapper.py")
file(WRITE "${_meson_wrapper_py}"
    "import os\n"
    "import sys\n\n"
    "extra = r'${_meson_site_packages}'\n"
    "if os.path.isdir(extra) and extra not in sys.path:\n"
    "    sys.path.insert(0, extra)\n\n"
    "from mesonbuild.mesonmain import main\n\n"
    "raise SystemExit(main())\n"
)

if(CMAKE_HOST_WIN32)
    file(TO_NATIVE_PATH "${Python3_EXECUTABLE}" _python_native)
    file(TO_NATIVE_PATH "${_meson_wrapper_py}" _meson_wrapper_py_native)
    set(_meson_wrapper "${CMAKE_BINARY_DIR}/meson.cmd")
    file(WRITE "${_meson_wrapper}" "@echo off\r\n\"${_python_native}\" \"${_meson_wrapper_py_native}\" %*\r\n")
else()
    set(_meson_wrapper "${CMAKE_BINARY_DIR}/meson")
    file(WRITE "${_meson_wrapper}" "#!/bin/sh\nexec \"${Python3_EXECUTABLE}\" \"${_meson_wrapper_py}\" \"$@\"\n")
    file(CHMOD "${_meson_wrapper}"
        PERMISSIONS
            OWNER_READ OWNER_WRITE OWNER_EXECUTE
            GROUP_READ GROUP_EXECUTE
            WORLD_READ WORLD_EXECUTE
    )
endif()

set(MESON_EXECUTABLE "${_meson_wrapper}" CACHE FILEPATH "Meson launcher" FORCE)
