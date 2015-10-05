

# CMakeLists.txt for frame                                                                                                                                                                    
project(FrameJni-native)

# required cmake version                                                                                                                                                                                   
cmake_minimum_required(VERSION 2.8)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS} )


file(GLOB TOOL_SRC dataTransfer.cpp)
add_library(dataTrans SHARED ${TOOL_SRC})


link_libraries(dataTrans)
file(GLOB Frame_SRC cn_ac_ict_htc_utils_NativeUtils.cpp )
add_library(FrameJni SHARED ${Frame_SRC})
