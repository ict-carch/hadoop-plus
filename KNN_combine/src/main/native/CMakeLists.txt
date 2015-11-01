# CMakeLists.txt for rida                                                                                                                                                                    
project(KNN-native)

# required cmake version                                                                                                                                                                                   
cmake_minimum_required(VERSION 2.8)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

include_directories(../../../../frame/src/main/native/)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fopenmp  -O3")
set(CMAKE_CXX_FLAGS "${CMAKE_C_FLAGS} -fopenmp -O3")
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fopenmp ")
#set(CMAKE_CXX_FLAGS "${CMAKE_C_FLAGS} -fopenmp ")
add_subdirectory(omp)

find_package(CUDA)
if (${CUDA_FOUND})
  message(STATUS "Found CUDA.")
  add_subdirectory(cuda)
elseif (${CUDA_FOUND})
  message(STATUS "Could not locate CUDA.")
endif (${CUDA_FOUND})
