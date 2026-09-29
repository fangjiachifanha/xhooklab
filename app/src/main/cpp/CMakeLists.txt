cmake_minimum_required(VERSION 3.18)
project(xhooklab C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Dobby 作为可选的子模块引入；若目录不存在则优雅降级（不引入 Dobby，仅编译 stub）
set(XHL_HAVE_DOBBY 0)
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/dobby/CMakeLists.txt")
    set(DOBBY_GENERATE_SHARED OFF CACHE BOOL "" FORCE)
    set(DOBBY_DEBUG OFF CACHE BOOL "" FORCE)
    add_subdirectory(dobby)
    set(XHL_HAVE_DOBBY 1)
    message(STATUS "XHookLab: Dobby submodule found, native hooks ENABLED")
else()
    message(WARNING "XHookLab: Dobby submodule NOT found, native hooks will be stubbed")
endif()

add_library(xhooklab SHARED native_hook.c)

target_compile_options(xhooklab PRIVATE -Wall -Wextra -fvisibility=hidden -O2)

if(XHL_HAVE_DOBBY)
    target_compile_definitions(xhooklab PRIVATE XHL_HAVE_DOBBY=1)
    target_include_directories(xhooklab PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/dobby/include)
    target_link_libraries(xhooklab dobby log)
else()
    target_link_libraries(xhooklab log)
    if(ANDROID)
        target_link_libraries(xhooklab dl)
    endif()
endif()
