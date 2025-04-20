cmake_minimum_required(VERSION 3.11)

project(mc)
set(CMAKE_ANDROID_ARCH_ABI arm64-v8a)
add_library(mc SHARED mc-init.cpp)
