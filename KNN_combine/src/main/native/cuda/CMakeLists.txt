# nvcc flags
set(CUDA_NVCC_FLAGS ${CUDA_NVCC_FLAGS};-gencode arch=compute_20,code=sm_20 ) #if not, atomicAdd is undefined.

file(GLOB MY_SRC *.cu *.cpp) 

find_library(DARATRANS_PATH
			libdataTrans.so
			../../../../../frame/target/native) 
find_library(CUDA_RT
			libcuda.so.1
                        /usr/lib64/nvidia)
find_library(CUDA_PATH_CUBLAS
			libcublas.so
			/usr/local/cuda/lib64)
#
#find_library(CUDA_PATH_CUSPARSE
#			libcusparse.so)
			
cuda_add_library(KNNCuda ${MY_SRC} SHARED)

target_link_libraries(KNNCuda ${DARATRANS_PATH}
	${CUDA_PATH_CUBLAS}
	${CUDA_RT})
