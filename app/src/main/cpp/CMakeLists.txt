cmake_minimum_required(VERSION 3.11)

project(selaura)
set(CMAKE_ANDROID_ARCH_ABI arm64-v8a)
add_library(selaura SHARED selaura.cpp)
