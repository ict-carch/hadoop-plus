#include "jni.h"
#include <vector>

using namespace std;

#define ERR_CODE_ERR      -1 
#define ERR_CODE_OK       0 
#define ERR_CODE_CONTINUE 1 

#define INT_SIZE sizeof(int)
#define FLOAT_SIZE sizeof(float)
#define LONG_SIZE sizeof(long)
#define DOUBLE_SIZE sizeof(double)


extern "C"
{
/***********************************************/
/************                   ****************/
/************ user function gpu ****************/
/************                   ****************/
/***********************************************/

// args1:pointer of init_buf
// args2:buffer order
// args3:init args
jint init_env_gpu(char*,const int ,const char*);

//args1:args from java
//args2:offset
//args3:pointer of data_buf
//args4:size of data
//args5:order of data_buf
//args6:size of global mem
jint put_data_gpu(const char* ,vector<int> ,char*,const int,const int,unsigned int*);

//args1:args from java
//args2:order of out_buf
//args3:size of global mem
//args4:out buffer pointer
//args5:out buffer size
//args6:the buffer capacity
jint calc_gpu(const char* ,const int ,const unsigned int,char*,int*,const int);

jint free_mem_gpu();


/***********************************************/
/************                   ****************/
/************ user function cpu ****************/
/************                   ****************/
/***********************************************/

// args1:pointer of init_buf
// args2:buffer order
// args3:init args
jint init_env_cpu(char*,const int,const char*);

//args1:args from java
//args2:offset
//args3:pointer of data_buf
//args4:size of data
//args5:order of data_buf
//args6:size of global mem
jint put_data_cpu(const char*,vector<int> ,char*,const int,const int,unsigned int*);

//args1:args from java
//args2:order of out_buf
//args3:size of global mem
//args4:out buffer pointer
//args5:out buffer size
//args6:the buffer capacity
jint calc_cpu(const char*,const int ,const unsigned int,char*,int*,const int);

jint free_mem_cpu();

}//#extern c
