cmake_minimum_required(VERSION 3.10.2)

project("nativeimageprocessing")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)