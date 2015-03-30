#include<dlfcn.h>
#include<stdio.h>
#include<stdlib.h>
#include<assert.h>
#include<string.h>
#include "jni.h"
#include "dataTransfer.h"

#define CPU_FLAG 0
#define GPU_FLAG 1
#define CHAR_SIZE sizeof(char)
#define INT_SIZE sizeof(int)
#define LONG_SIZE sizeof(long)

void parseArgs(jstring);
jint load_library(const char* );

/************ dlopen ************/

jint load_method_gpu();
jint load_method_cpu();

/*******************  buffer *******************/

jint get_buffer_limit(jobject);
jint get_buffer_position(jobject);
void set_buffer_position(jobject, int);
int get_buffer_order(jobject);

/*************** do method ********************/
jint do_init_env_gpu(jobject,const char*);
jint do_init_env_cpu(jobject,const char*);

jint do_put_off(jobject);

jint do_put_data(jobject,const char*);

jint do_calc(jobject,const char*);

jint do_free();

jint do_default(jobject,jobject,char*);

/****************************************/

extern int _debug;
extern int _is_timing;
