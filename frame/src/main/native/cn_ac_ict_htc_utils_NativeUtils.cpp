#include <iostream>
#include "cn_ac_ict_htc_utils_NativeUtils.h"
#include "jni_frame.h"

using namespace std;

#define ERR_CODE_ERR	  cn_ac_ict_htc_utils_NativeUtils_ERR_CODE_ERR
#define ERR_CODE_OK		  cn_ac_ict_htc_utils_NativeUtils_ERR_CODE_OK
#define ERR_CODE_CONTINUE cn_ac_ict_htc_utils_NativeUtils_ERR_CODE_CONTINUE

#define CMD_CODE_LOOPBACK cn_ac_ict_htc_utils_NativeUtils_CMD_CODE_LOOPBACK
#define CMD_CODE_INIT_CPU cn_ac_ict_htc_utils_NativeUtils_CMD_CODE_INIT_CPU
#define CMD_CODE_INIT_GPU cn_ac_ict_htc_utils_NativeUtils_CMD_CODE_INIT_GPU
#define CMD_CODE_PUT_OFF  cn_ac_ict_htc_utils_NativeUtils_CMD_CODE_PUT_OFF
#define CMD_CODE_PUT_DATA cn_ac_ict_htc_utils_NativeUtils_CMD_CODE_PUT_DATA
#define CMD_CODE_CALC     cn_ac_ict_htc_utils_NativeUtils_CMD_CODE_CALC
#define CMD_CODE_FREE_MEM cn_ac_ict_htc_utils_NativeUtils_CMD_CODE_FREE_MEM

/*********************/

int _debug = 0;

/*********************/
JNIEnv *jni_env;
void* handle;
unsigned int global_input_mem_size = 0;						 // global memory size
//char* cmd_args = (char*)malloc(1024*100*sizeof(char));//the args from java
char* cmd_args = (char*)calloc(1024*100, sizeof(char));//the args from java
vector <int> input_offset_vector;					// input offset vector

int res_flag = CPU_FLAG;							//default cpu


/****************** declaration jni_user functions *******************/
jint (*init_env_gpu)(char*,const int,const char*);
jint (*init_env_cpu)(char*,const int,const char*);

jint (*put_data_gpu)(const char*,vector<int> ,char*,const int,const int,unsigned int*);
jint (*put_data_cpu)(const char*,vector<int>,char*,const int,const int,unsigned int*);

jint (*calc_gpu)(const char*,const int ,const unsigned int,char*,int*,const int);
jint (*calc_cpu)(const char*,const int ,const unsigned int,char*,int*,const int);

jint (*free_mem_cpu)();
jint (*free_mem_gpu)();

/******************* buffer position/limit ************************/
jint get_buffer_position(jobject buffer)
{
	jclass in_cls = jni_env->GetObjectClass(buffer);
	jmethodID mtd_position = jni_env->GetMethodID(in_cls, "position", "()I");
	jint position = jni_env->CallIntMethod(buffer, mtd_position);
	return position;
}

void set_buffer_position(jobject buffer, int newPosition)
{
	jclass in_cls = jni_env->GetObjectClass(buffer);
	jmethodID mtd_position = jni_env->GetMethodID(in_cls, "position","(I)Ljava/nio/Buffer;");
	jni_env->CallObjectMethod(buffer, mtd_position, (jint) newPosition);
	return;
}

jint get_buffer_limit(jobject buffer)
{
	jclass in_cls = jni_env->GetObjectClass(buffer);
	jmethodID mtd_limit = jni_env->GetMethodID(in_cls, "limit", "()I");
	jint position = jni_env->CallIntMethod(buffer, mtd_limit);
	return position;
}

jint get_buffer_capacity(jobject buffer)
{
	jclass in_cls = jni_env->GetObjectClass(buffer);
	jmethodID mtd_capacity = jni_env->GetMethodID(in_cls, "capacity", "()I");
	jint capacity = jni_env->CallIntMethod(buffer, mtd_capacity);
	return capacity;
}


int get_buffer_order(jobject buffer)
{
	jclass in_clz = jni_env->GetObjectClass(buffer);
	jmethodID mtd_order = jni_env->GetMethodID(in_clz, "order","()Ljava/nio/ByteOrder;");
	jobject byte_order = jni_env->CallObjectMethod(buffer, mtd_order);
	jclass byte_order_clz = jni_env->GetObjectClass(byte_order);
	jmethodID toString = jni_env->GetMethodID(byte_order_clz, "toString","()Ljava/lang/String;");
	jstring str = (jstring) jni_env->CallObjectMethod(byte_order, toString);
	char *order = (char *) jni_env->GetStringUTFChars(str, NULL);
	if(strcmp(order, "LITTLE_ENDIAN") == 0){
		jni_env->ReleaseStringUTFChars(str,order);
		return 0;
	}else{
		jni_env->ReleaseStringUTFChars(str,order);
		//BIG
		return 1;
	}
}


/****************** do_loop_back  **********************/

jint do_loop_back(jobject input, jobject output)
{
	const int in_position = get_buffer_position(input);
	const int in_limit = get_buffer_limit(input);
	const int out_position = get_buffer_position(output);
	const int out_limit = get_buffer_limit(output);
	cout << "in_limit = " << in_limit << "in_position = " <<
	    in_position << endl;
	cout << "out_limit = " << out_limit << "out_position = " <<
	    out_position << endl;
	int i = in_limit - in_position;
	if (i > out_limit - out_position)
		i = out_limit - out_position;
	char *src;
	cout << "src addr: " << src << endl;
	if (input) {
		src = (char *) jni_env->GetDirectBufferAddress(input);
	}
	cout << i << endl;
	for (int j = in_position; j < in_position + i; j++)
		cout << src[j];
	cout << endl;
	char *dst = NULL;
	if (NULL != output) {
		dst = (char *) jni_env->GetDirectBufferAddress(output);
		cout << "dst addr: " << dst << endl;
	}
	cout << "begin copy" << endl;
	for (int j = in_position; j < in_position + i; j++)
		cout << src[j];
	memcpy(dst, src, i * sizeof(char));
	cout << "dst data: " << endl;
	for (int j = out_position; j < out_position + i; j++)
		cout << dst[j];
	cout << "copied " << i * sizeof(char) << " bits." << endl;

	set_buffer_position(input, in_position + i);
	set_buffer_position(output, out_position + i);
	cout << "out_position = " << out_position + i << endl;
	return (jint) ERR_CODE_OK;
}

/***************** do_init_env_gpu ******************/ 
jint do_init_env_gpu(jobject init_buf,const char* args)
{
	char* init_val = NULL;
	if(init_buf != NULL)
		init_val = (char*)jni_env->GetDirectBufferAddress(init_buf);
	else
		return ERR_CODE_ERR;
	int init_buf_limit = get_buffer_limit(init_buf);
	int init_buf_pos = get_buffer_position(init_buf);
	int init_buf_order = get_buffer_order(init_buf);
	char* lib_name = init_val+100;
	init_val[init_buf_limit]='\0';
	if(_debug)
		printf("lib_name is %s\n",lib_name);
	jint rs = ERR_CODE_ERR;
	rs = load_library(lib_name);
	if(rs != ERR_CODE_OK){
		printf("load GPU library failure : %s\n",lib_name);
		return rs;
	}
	rs = load_method_gpu();
	if(rs != ERR_CODE_OK){
		return rs;
	}
	printf("\n************* init gpu  ******************\n");
	rs = init_env_gpu(init_val,init_buf_order,args);
	printf("\n************* init gpu end  ******************\n");
	if(rs == ERR_CODE_OK){
		res_flag = GPU_FLAG; 
	}else{
		printf("init gpu failed.\n");
	}
	if(_debug)
		printf("res flag %s\n",res_flag==GPU_FLAG?"GPU":"CPU");
	return rs;
}

/***************** do_init_env_cpu ******************/ 
jint do_init_env_cpu(jobject init_buf,const char* args)
{
	char* init_val = NULL;
	if(init_buf !=NULL)
		init_val = (char*)jni_env->GetDirectBufferAddress(init_buf);
	else
		return ERR_CODE_ERR;
	int init_buf_limit = get_buffer_limit(init_buf);
	int init_buf_pos = get_buffer_position(init_buf);
	int init_buf_order = get_buffer_order(init_buf);
	char* lib_name = init_val+100;
	init_val[init_buf_limit]='\0';
	if(_debug)
		printf("lib_name is %s\n",lib_name);
	jint rs = ERR_CODE_ERR;
	rs = load_library(lib_name);
	if(rs != ERR_CODE_OK){
		printf("load CPU library failure : %s\n",lib_name);
		return rs;
	}
	rs = load_method_cpu();
	if(rs != ERR_CODE_OK){
		return rs;
	}
	printf("\n************* init cpu  ******************\n");
	rs = init_env_cpu(init_val,init_buf_order,args);	
	printf("\n************* init cpu end ******************\n");
	if(rs == ERR_CODE_OK){
		res_flag = CPU_FLAG; 
	}else{
		printf("init cpu failed.\n");
	}
	if(_debug)
		printf("res flag %s\n",res_flag==GPU_FLAG?"GPU":"CPU");
	return rs;
}
/*************** do_put_off *****************/
jint do_put_off(jobject off_buf)
{
	input_offset_vector.clear();
	char* val_off;
	if(off_buf != NULL)
		val_off = (char*) jni_env->GetDirectBufferAddress(off_buf);
	else
		return ERR_CODE_ERR;
	int off_buf_pos = get_buffer_position(off_buf);
	int off_buf_limit = get_buffer_limit(off_buf);
	int off_buf_order = get_buffer_order(off_buf);
	printf("\n************* put offset ******************\n");
	for (int i = off_buf_pos; i < off_buf_limit; i += INT_SIZE){
		input_offset_vector.push_back(getInt_Simple(val_off, i,off_buf_order));
	}
	if(_debug)
		printf("offset size:%d.\n ",off_buf_limit);
	printf("\n************* put offset end ******************\n");
	set_buffer_position(off_buf, off_buf_limit);
	return ERR_CODE_OK;
}

/****************** do_put_data ******************/

jint do_put_data(jobject data_buf,const char* args)
{
	char* val_data = NULL;
	if(data_buf != NULL)
		val_data = (char*)jni_env->GetDirectBufferAddress(data_buf);
	else
		return ERR_CODE_ERR;
	//
	jint rs =ERR_CODE_ERR;
	int data_buf_limit = get_buffer_limit(data_buf);
	int data_buf_pos = get_buffer_position(data_buf);
	int data_buf_order = get_buffer_order(data_buf);
	if(_debug){
		printf("put data--\tdata_buf limit:%d,pos:%d,order:%d\n",data_buf_limit,data_buf_pos,data_buf_order);
		printf("put data--\tres_flag :%s\n",res_flag==GPU_FLAG?"GPU":"CPU");
	}
	unsigned int global_size_temp = global_input_mem_size;
	printf("\n************* put data ******************\n");
	if(res_flag == GPU_FLAG){
		rs = put_data_gpu(args,input_offset_vector,val_data,data_buf_limit,data_buf_order,&global_input_mem_size);
	}else{
		rs = put_data_cpu(args,input_offset_vector,val_data,data_buf_limit,data_buf_order,&global_input_mem_size);
	}
	printf("\n************* put data end ******************\n");
	unsigned int put_data_size = global_input_mem_size - global_size_temp;
	if(_debug){
		printf("global input mem size :%d\n",global_input_mem_size);
	}
	set_buffer_position(data_buf, data_buf_limit);
	return rs;
}

/********************** do_calc *****************/
jint do_calc(jobject out_buf,const char* args)
{
	char* dst = NULL;
	if(out_buf != NULL)
		dst = (char*)jni_env->GetDirectBufferAddress(out_buf);
	else
		return ERR_CODE_ERR;
	jint rs = ERR_CODE_ERR;
	int out_buf_limit = get_buffer_limit(out_buf);
	int out_buf_pos = get_buffer_position(out_buf);
	int out_buf_order = get_buffer_order(out_buf);
	int out_buf_capacity = get_buffer_capacity(out_buf);
	int output_size = 0;
	if(_debug){
		printf("calc--\tout_buf limit:%d,pos:%d,order:%d,capacity:%d.\n",out_buf_limit,out_buf_pos,out_buf_order,out_buf_capacity);
		printf("calc--\tres_flag :%s\n",res_flag==GPU_FLAG?"GPU":"CPU");
	}
	printf("\n************* calc ******************\n");
	if(res_flag == GPU_FLAG){
		rs = calc_gpu(args,out_buf_order,global_input_mem_size,dst,&output_size,out_buf_capacity);
	}else{
		rs = calc_cpu(args,out_buf_order,global_input_mem_size,dst,&output_size,out_buf_capacity);
	}
	printf("\n************* calc end ******************\n");
	if(_debug)
		printf("output size :%d \n",output_size);
	set_buffer_position(out_buf,out_buf_pos + output_size);
	return rs;
}
/******************* do_free ****************/
jint do_free()
{
	jint rs = ERR_CODE_ERR;
	printf("\n*************** free memory *************** \n");
	if(res_flag == GPU_FLAG){
		rs = free_mem_gpu();
	}else{
		rs = free_mem_cpu();
	}
	global_input_mem_size = 0;
	printf("\n************** free memory end **************** \n");
	return rs;
}

/***************** do_default *****************/
jint do_default(jobject input,jobject output,char* args)
{
	printf("default case ...\n");
	return ERR_CODE_OK;
}

jint load_library(const char* libName)
{
	handle = dlopen(libName,RTLD_LAZY);
	if(!handle){
		printf("handle is NULL,load lib failure!");
		fputs(dlerror(),stderr);
		return ERR_CODE_ERR;	
	}
	return ERR_CODE_OK;
}

/***********************************************/

jint load_method_gpu()
{
	if(_debug)
		printf("loading method gpu..\n");
	init_env_gpu = (jint(*)(char*,const int,const char*))dlsym(handle,"init_env_gpu");	
	put_data_gpu = (jint(*)(const char*,vector<int> ,char*,const int,const int,unsigned int*))dlsym(handle,"put_data_gpu");
	calc_gpu = (jint(*)(const char*,const int,const unsigned int,char*,int*,const int))dlsym(handle,"calc_gpu");
	free_mem_gpu = (jint(*)())dlsym(handle,"free_mem_gpu");
	char* error = dlerror();
	if(error != NULL){
		printf("load methods of GPU failure!\nERROR:%s\n",error);
		fputs(error,stderr);
		return ERR_CODE_ERR;
	}
	if(_debug)
		printf("loaded method gpu..\n");
	return ERR_CODE_OK;
}

jint load_method_cpu()
{
	if(_debug)
		printf("loading method cpu..\n");
	init_env_cpu = (jint(*)(char*,const int,const char*))dlsym(handle,"init_env_cpu");	
	put_data_cpu = (jint(*)(const char*,const vector<int>,char*,const int,const int,unsigned int*))dlsym(handle,"put_data_cpu");
	calc_cpu = (jint(*)(const char*,const int,const unsigned int,char*,int*,const int))dlsym(handle,"calc_cpu");
	free_mem_cpu = (jint(*)())dlsym(handle,"free_mem_cpu");

	char* error = dlerror();
	if(error != NULL){
		printf("load methods of CPU failure!\nERROR: %s\n",error);
		fputs(error,stderr);
		return ERR_CODE_ERR;
	}
	if(_debug)
		printf("loaded method cpu..\n");
	return ERR_CODE_OK;
}

void parseArgs(jstring args)
{
	const char* _cmd_args;
	if(args != NULL){
		_cmd_args =jni_env->GetStringUTFChars(args,NULL);
	}
	if(args == NULL||strlen(_cmd_args)==0){ 
		if(_debug)
			printf("%s; cmd_args = %s, strlen of cmd_args is: %d\n","args is empty", cmd_args, strlen(cmd_args));
		return;
	}else if(cmd_args == NULL||strlen(cmd_args) == 0){   
		if(cmd_args == NULL)
			cmd_args = (char*) calloc(1024*100, sizeof(char));
		strcpy(cmd_args,_cmd_args);
	}else if(strcmp(_cmd_args,cmd_args)!=0){
		strcpy(cmd_args,_cmd_args);
	}
	if(args != NULL){
		jni_env->ReleaseStringUTFChars(args,_cmd_args);
		printf("cmd_args is %s\n",cmd_args);
	}
}

/***********************  callNative  *******************************/
JNIEXPORT jint JNICALL
    Java_cn_ac_ict_htc_utils_NativeUtils_callNative(JNIEnv * env,
							      jclass clazz,
							      jint
							      cmd_code,
							      jobject
							      input,
							      jobject
							      output,
							      jstring
							      args) {

	jni_env = env;
	jint ret_code = ERR_CODE_ERR;
	parseArgs(args);
	printf("%s\t%s\t%d\n","coming to the native,","and cmd_code is",cmd_code);
	switch (cmd_code) {
	case CMD_CODE_LOOPBACK:
	{
		ret_code = do_loop_back(input, output);
		break;
	}
	case CMD_CODE_INIT_CPU:
	{
		ret_code = do_init_env_cpu(input,cmd_args);
		break;
	}
	case CMD_CODE_INIT_GPU:
	{
		ret_code = do_init_env_gpu(input,cmd_args);
		break;
	}
	case CMD_CODE_PUT_OFF:
	{
		ret_code = do_put_off(input);
		break;
	}
	case CMD_CODE_PUT_DATA:
	{
		ret_code = do_put_data(input,cmd_args);	
		break;
	}
	case CMD_CODE_CALC:
	{
		ret_code = do_calc(output,cmd_args);
		break;
	}
	case CMD_CODE_FREE_MEM:
	{
		ret_code = do_free();
		break;
	}
	default:
	{
		ret_code = do_default(input, output, cmd_args);
		break;
	}
	}
	return ret_code;
}
