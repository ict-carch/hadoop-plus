file(GLOB MY_SRC *.c *.cpp)

# lib dirs
find_library(MKL_LIB
             libmkl_rt.so
             /home/hewt/Softwares/intel/composer_xe_2013.4.183/composer_xe_2013.4.183/mkl/lib/intel64
)

find_library(MKL_CPL_LIB
             libiomp5.so
            /home/hewt/Softwares/intel/composer_xe_2013.4.183/composer_xe_2013.4.183/compiler/lib/intel64 
)
find_library(DARATRANS_PATH
			libdataTrans.so
			../../../../../frame/target/native)
#find_library(PAPI_LIB
#             libpapi.so.5.3.0.0
#             /home/hewt/Softwares/papi-5.3.0-install/lib 
#)

include_directories(/home/hewt/Softwares/intel/composer_xe_2013.4.183/mkl/include)
include_directories(/home/hewt/Softwares/papi-5.3.0-install/include)
add_library(KNNOMP SHARED ${MY_SRC})
target_link_libraries(KNNOMP ${MKL_CPL_LIB} ${MKL_LIB} ${DARATRANS_PATH})
#	${PAPI_LIB})
