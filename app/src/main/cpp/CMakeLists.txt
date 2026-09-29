cmake_minimum_required(VERSION 3.18)
project(xhooklab C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Dobby 作为可选子模块引入；若目录/CMake 不存在则降级为 stub 编译
set(DOBBY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/dobby")
set(XHL_HAVE_DOBBY 0)

if(EXISTS "${DOBBY_DIR}/CMakeLists.txt")
    set(DOBBY_GENERATE_SHARED OFF CACHE BOOL "" FORCE)
    set(DOBBY_DEBUG OFF CACHE BOOL "" FORCE)
    set(DOBBY_ARCH arm64 CACHE STRING "" FORCE)
    add_subdirectory(${DOBBY_DIR} dobby_build)
    set(XHL_HAVE_DOBBY 1)
    message(STATUS "XHookLab: Dobby found -> native hooks ENABLED")
else()
    message(WARNING "XHookLab: Dobby NOT found -> compile stub (native hooks disabled)")
endif()

add_library(xhooklab SHARED native_hook.c)

target_compile_options(xhooklab PRIVATE -Wall -Wextra -fvisibility=hidden -O2)

if(XHL_HAVE_DOBBY)
    target_compile_definitions(xhooklab PRIVATE XHL_HAVE_DOBBY=1)
    target_include_directories(xhooklab PRIVATE ${DOBBY_DIR}/include)
    target_link_libraries(xhooklab dobby log)
else()
    target_link_libraries(xhooklab log)
    if(ANDROID)
        target_link_libraries(xhooklab dl)
    endif()
endif()
