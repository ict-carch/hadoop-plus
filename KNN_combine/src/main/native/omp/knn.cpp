#include <assert.h>
#include <sys/time.h>
#include <sys/wait.h>
#include <errno.h>
#include <iostream>
#include <sstream>
//#include <papi.h>
#include "omp.h"
#include "mkl.h"
#include "mkl_cblas.h"
#include "knn.h"

using namespace std;

#define NUM_THREAD omp_get_num_procs()
//native order
#define NATIVE_ORDER getNativeOrder()
#define MAX_LOCAL_MEM 1024*1024*100
#define TRAIN 1
#define TEST 0

int _debug = 0;
int _is_timing = 0;
double *trains, *tests;
long *trainKeys, *testKeys;
int test_num = 0;
int train_num = 0;
int test_start = 0;
int test_end = 0;
long trainsmem_size;


int dataset = TRAIN;
int dimension = 0;
string spid;
string sds;

/******************* init_env_cpu ******************/

jint init_env_cpu(char *input ,const int order,const char *args)
{
	trainsmem_size = getLong_Simple(input,0,order);

        int bs = 0, ds = 0; 
	if(_debug){
		printf("length ..... %d\n",strlen(args));
		if(args != NULL&& strlen(args)!=0)
			printf("init command args :%s\n",args);
	}
	if(args!=NULL && strlen(args)!=0){
		map<string,string> args_map;
		parse_to_map(args,args_map);
//		dimension = getDimension(args_map["dimension"]);
	 	bs = atoi(args_map["blockSize"].data());
	 	ds = atoi(args_map["dd_num"].data());
		    _debug = getDebug(args_map["isDebug"]);
	}
//	if(bs.compare("0"))
        if(trainsmem_size < (long)(2048 * 1024 * 1024))
	{
	    printf("to malloc %d bytes for trains\n", (int)trainsmem_size);
            trains = (double *)malloc((int)trainsmem_size);
            trainKeys = (long *)malloc((int)trainsmem_size);
	}
	else
	{
	    printf("to malloc %d bytes for trains\n", (int)(trainsmem_size - SIZE_OF_DOUBLE));
            trains = (double *)malloc((int)(trainsmem_size - SIZE_OF_DOUBLE));
            trainKeys = (long *)malloc((int)(trainsmem_size - SIZE_OF_DOUBLE));
	}
	if(trains == NULL)
	    printf("malloc addr for trains failed!\n");
//	if(dimension)
//	    trainKeys = (long *)malloc((int)mem_size/dimension);
//	if(_debug){
//	}
	    stringstream ssbs;
	    string sbs;
	    ssbs << bs;
	    ssbs >> sbs;
	    
	    stringstream ssds;
	    ssds << ds;
	    ssds >> sds;
	    
            string cmd = "/bin/bash /home/hewt/scripts/startio.sh " + sds + " " + sbs;
            cout << cmd << endl;
            system(cmd.c_str());    
		printf( "init_env_cpu. \tsuccess!\n");
	cout << "train_num = " << train_num << "; test_num = " << test_num << endl;
		return ERR_CODE_OK;
}

/************************ put_data_cpu *******************/

jint put_data_cpu(const char *args,vector<int> input_offset,char *input_data,const int input_limit,const int order ,unsigned int *global_input_mem_size)
{
	int _debug = 1;
	if(_debug){
		printf("put_data_cpu ,\tthe buffer limit is %d.\n",input_limit);
	}
	map<string,string> cmd_args;
	if(args != NULL){
		parse_to_map(args,cmd_args);
		if(cmd_args.size()>0){
                    dimension = getDimension(cmd_args["dimension"]);
		    dataset = getDataset(cmd_args["dataset"]);
		    _debug = getDebug(cmd_args["isDebug"]);
		}
	}
	if(_debug){
		printf("input offset size is %d.\t strlen(input_data) is %d\n",input_offset.size(),strlen(input_data));
	}
	switch(dataset){
	    case TRAIN:
	    {
//		if(trainKeys==NULL)
//		{
//	            printf("to malloc %d bytes for trainKeys\n", (int)(trainsmem_size/dimension));
//         	    trainKeys = (long *)malloc((int)(trainsmem_size/dimension));
//		}
//	        if(trainKeys == NULL)
//	            printf("malloc addr for trainKeys failed!\n");
		int case_num = input_offset.size() ;
		if(trains == NULL)
		{
		trains = (double *)malloc(case_num * dimension * sizeof(double));
		trainKeys = (long *)malloc(case_num * sizeof(double));
		}
	    	if(_debug)
	    	    cout << "put " << case_num << " training vectors " << endl;
		for(int idx = 1; idx <= case_num; idx++)
		{
                    int vector_start = input_offset[idx - 1];
		    if(_debug)
		    {
			cout << "vector start at : " << vector_start << endl;
		    }
		    trainKeys[train_num] = getLong_Simple(input_data, vector_start, order);
		    memcpy(&trains[train_num * dimension], input_data + vector_start + SIZE_LONG, dimension * SIZE_OF_DOUBLE);
//		    for (int i = 0; i < dimension; i++)
//		        trains[train_num * dimension + i] = getDouble_Simple(input_data, vector_start + SIZE_LONG + i * SIZE_OF_DOUBLE, order);
//	            case_num--;
//		    if(_debug)
//		    {
//			for(int i = 0; i < dimension; i++) 
//			    cout << trains[train_num * dimension + i] << " ";
//			cout << endl;
//		    }
		    train_num++;
		}
	   	*global_input_mem_size += input_limit;
	    	return ERR_CODE_OK;
	    }
	    case TEST:
	    {
		int case_num = input_offset.size();
	    	if(_debug)
		    cout << "put " << case_num << "tests" << endl;
		tests = (double *)malloc(case_num * dimension * sizeof(double));
		testKeys = (long *)malloc(case_num * sizeof(double));
		for(int idx = 1; idx <= case_num; idx++)
		{
                    int vector_start = input_offset[idx - 1];
		    testKeys[test_num] = getLong_Simple(input_data, vector_start, order);
		    for (int i = 0; i < dimension; i++)
		        tests[(test_num ) * dimension + i] = getDouble_Simple(input_data, vector_start + SIZE_LONG + i * SIZE_OF_DOUBLE, order);
//	            case_num--;
//		    if(_debug)
//		    {
//			cout << "test_key: " << testKeys[test_num] << ", vector: ";
//			for(int i = 0; i < dimension; i++)
//			    cout << tests[test_num * dimension + i] << " ";
//			 cout << endl; 
//	 	    }
		    test_num++;
		}
	    	*global_input_mem_size += input_limit;
 	     	return ERR_CODE_OK;
	    }
	    default:
	    {
	    	//nothing
	    	//may do the default trans the data which is without process data
	    	break;
 	     }
 	} 
	return ERR_CODE_OK;
}

/************************  calc_cpu *********************/

jint calc_cpu(const char *args,const int order,const unsigned int g_size,char *dst,int *dst_size,const int dst_capacity)
{
    printf("beginning calc cpu.\n");
	map<string,string> cmd_args;
	int kn = 1, mkl_thread = 0, omp_thread = 0, _debug = 0;
	string sf, sn;
	long long start_usec, end_usec;
	if(args != NULL){
		parse_to_map(args,cmd_args);
		if(cmd_args.size()>0){
		    dimension = getDimension(cmd_args["dimension"]);
		    kn = getK(cmd_args["k"]);
		    mkl_thread = getMKLthread(cmd_args["mkl_thread"]);
		    omp_thread = getOMPthread(cmd_args["omp_thread"]);
		    sf = cmd_args["factor"];
		    sn = cmd_args["streamNum"];
		    _debug = getDebug(cmd_args["isDebug"]);
		}
		if(_debug){
			printf("calc args : %s\n",args);
		}
	}
	cout << "train_num = " << train_num << "; test_num = " << test_num << "; kn = " << kn <<  endl;
	jint rs = ERR_CODE_OK;
//	FILE *fp;
//	int numObj = train_num + test_num;
//	fp=fopen("/home/hewt/scripts/knn-test/128Binary", "wb");
//	fwrite(&numObj, sizeof(int), 1, fp);
//	fwrite(&dimension, sizeof(int), 1, fp);
//	fwrite(tests, sizeof(double), test_num * dimension, fp);
//	fwrite(trains, sizeof(double), train_num * dimension, fp);
//	fclose(fp);
//	for (int i = 0; i < test_num; i++)
//	{
////	    cerr << "testKeys[" << i << "] " << testKeys[i] << " ";
//	    for (int j=0; j < dimension; j++)
//		cerr << tests[i * dimension + j] << " ";
//	    cerr << endl;
//	}
//	for (int i = 0; i < train_num; i++)
//	{
////	    cerr << "trainKeys[" << i << "] " << trainKeys[i] << " ";
//	    for (int j=0; j < dimension; j++)
//		cerr << trains[i * dimension + j] << " ";
//	    cerr << endl;
//	}
    int size_line = SIZE_LONG + dimension * SIZE_OF_DOUBLE;
    int size_result = SIZE_OF_LONG + SIZE_OF_LONG + SIZE_OF_DOUBLE;
	int test_blk = dst_capacity / (size_result * kn);
	test_end = test_start + test_blk;
	if(test_end > test_num)
	    test_end = test_num;
	int i, j, k, d;
	double res;
    int size_vector = dimension * SIZE_OF_DOUBLE;
	long *testIds = (long *)dst;
	long *ids = (long *)dst;
	double *distance = (double *)dst;
    int num_threads;
//    if(mkl_thread)
//        mkl_set_num_threads(mkl_thread);
    if(omp_thread)
	    num_threads = omp_thread;
    else
        num_threads = getNumThreads();
    printf("mkl_thread = %d, omp_thread = %d\n", mkl_thread, num_threads);
	system("sh /home/hewt/scripts/cleario.sh");
//        int pid = getpid();
//	stringstream sspid;
//	sspid << pid;
//	sspid >> spid;
//        cout << "the pid of native knn is: " <<  spid << endl;
//	        string vtune_dir = "mkdir /newdisk/third/hewt/" + spid; // + " &";
//                cout << vtune_dir << endl;
//                system(vtune_dir.c_str());
//                string vtune_cmd = "/bin/bash /home/hewt/scripts/varvtune.sh " + spid + " > /newdisk/third/hewt/" + spid + "/vtunesummary 2>&1 &";
//        cout << vtune_cmd << endl;
//                system(vtune_cmd.c_str());
//            string stream_cmd = "/home/hewt/scripts/launchstream.sh " + sn + " " + sf + " &";
//            system(stream_cmd.c_str());    
////		sleep(70);
////	    for (i = 0; i < 288; i++)
////		for(k = 0; k < 5878146; k++)
////		    for (d = 0; d < 20; d++)
////                        res += trains[k * dimension + d] * tests[i * dimension + d];
////                   double res = ddot(dimension, &trains[k * dimension], &tests[i * dimension]);
//	    if(test_start < test_num)
//	    {
//            struct timeval ustart, uend;
//                 gettimeofday(&ustart, NULL);
//////	        if(_debug)
//////	            printf("test_start = %d, test_end = %d \n", test_start, test_end);
//	        rs = train_dot_product(dimension, kn, order, dst, dst_size, dst_capacity, test_start, test_end, mkl_thread, omp_thread, _debug);
////#pragma omp parallel for num_threads(num_threads) private(i, j, k, res) shared(dst, trains, tests, ids, distance)
//    for (i = test_start; i < test_end; i++)
//    {
//            int base = (i - test_start) * kn * size_result;
//        
////        	    if(_debug)
////    	                 printf("test_cases[%d] ", i);
//	if(kn <= train_num)
//	{
////            KeyDistance *heap = new KeyDistance[kn]();
////	    if(_debug)
////                for (k = 0; k < kn; k++)
////                    printf("heap[%d].key = %ld, heap[%d].distance = %lf\n", k, heap[k].key, k, heap[k].distance);
//            for (k = 0; k < kn ; k++)
//            {
//// 	            int _debug = 0;
////        	    if(_debug)
////    	                 printf("begin dot product train_cases[%ld] and test_cases[%ld]\n", trainKeys[k], testKeys[i]);
////                   double res = ddot(dimension, &trains[k * dimension], &tests[i * dimension]);
//		    for (int d = 0; d < dimension; d++)
//                        res += trains[k * dimension + d] * tests[i * dimension + d];
////                  res = cblas_ddot(dimension, &trains[k * dimension], 1, &tests[i * dimension], 1);
////
//                testIds[((i - test_start) * kn + k) * 3] = testKeys[i];		    
//		ids[((i - test_start) * kn + k) * 3 + 1] = trainKeys[k];
//		distance[((i - test_start) * kn + k) * 3 + 2] = res;
//            }
////	    BuildHeap(heap, (kn-1));
////	    for (; k < 5878146; k++)
//	    for (; k < train_num; k++)
//	    {
////        	    if(_debug)
////    	                 printf("begin dot product train_cases[%ld] and test_cases[%ld]\n", trainKeys[k], testKeys[i]);
//
//		    for (int d = 0; d < dimension; d++)
//                        res += trains[k * dimension + d] * tests[i * dimension + d];
////                  res = cblas_ddot(dimension, &trains[k * dimension], 1, &tests[i * dimension], 1);
//		    double min = distance[(i - test_start) * kn * 3 + 2];
//		    long minid = 0;
//		    for(int ii = 1; ii < kn; ii++)
//		    {
//			if(min > distance[((i - test_start) * kn + ii) * 3 + 2])
//			{
//			    minid = ii;
//			    min = distance[((i - test_start) * kn + ii) * 3 + 2];
//			}
//		    }
//		    if(res > min)
//		    {
//			ids[((i - test_start) * kn + minid) * 3 + 1] = trainKeys[k];
//			distance[((i - test_start) * kn + minid) * 3 + 2] = res;
//		    }
//	    }
//////	    if(_debug)
//////		cout << "i: " << i << "; base: " << base << endl;
//////	    for(int kk = 0; kk < kn; kk++)
//////	    {
//////	        memcpy(dst + base + kk * size_result, &testKeys[i], SIZE_OF_LONG);
////////                if(_debug)
////////                   printf("putting testKey: %ld, ", testKeys[i]);
//////	        memcpy(dst + base + kk * size_result + SIZE_OF_LONG, &ids[kk], SIZE_OF_LONG);
////////                if(_debug)
////////                   printf("trainKey: %ld, ", heap[kk].key);
//////	        memcpy(dst + base + kk * size_result + SIZE_OF_LONG * 2, &distance[kk], SIZE_OF_DOUBLE);
////////                if(_debug)
////////                   printf("d: %lf to dstbuffer\n",  heap[kk].distance);
//////	    }
//////	    delete[] heap;
//	}
//    }
////                rs = seq_dot_product(dimension, k, order, dst, dst_size, dst_capacity, test_start, test_end);
//               gettimeofday(&uend, NULL);
//                int utime = 1000000 * (uend.tv_sec - ustart.tv_sec) + (uend.tv_usec - ustart.tv_usec);
//            string clear_cmd = "/bin/bash /home/hewt/scripts/cleario.sh " + spid;
//            cout << clear_cmd << endl;
//            system(clear_cmd.c_str());    
////	    printf("%a\n", res);
//                printf("time used for train omp dot product is: %d us\n", utime);
	        rs = ERR_CODE_OK;
//	        if(rs == ERR_CODE_OK)
//	        {
//// 	            int _debug = 0;
////	            if(_debug)
////	            {
////	                printf("dst_size: %d\nthe results are:\n", *dst_size);
////	                int pointer = 0;
////                        int block = test_end - test_start;
////	                for(int i = 0; i < block ; i++)
////	            	    for(int j = 0; j < k; j++)
////	            	    {
////	            	        printf("testCase: %ld, key: ", getLong_Simple(dst, pointer, order));
////                                    pointer += SIZE_OF_LONG;
////	            	        printf("%ld, d: ", getLong_Simple(dst, pointer, order));
////                                    pointer += SIZE_OF_LONG;
////	                            printf("%lf\n", getDouble_Simple(dst, pointer, order));
//// 	            	        pointer += SIZE_OF_DOUBLE;
//// 	            	    }
////	            }
// 	    	    if(test_end < test_num)
//	    	    {
//			cout << "enter the next round" << endl;
// 	    	        rs = ERR_CODE_CONTINUE;
//
//	                test_start += test_blk;
//	                test_end += test_blk;
//	                if(test_end > test_num)
//	                    test_end = test_num;
//	    	    }
// 	        }
//	    }
//	}
//    *dst_size += (test_end - test_start) * kn * size_result; 
 	    return rs;
}

//double ddot(const long test_key, const long train_key, const int dimension, const double *x, const double *y)
double ddot(const int dimension, const double *x, const double *y)
{
    double res = 0;
    for(int i = 0; i < dimension; i++)
    {
	res += x[i] * y[i];
//	if(_debug)
//	    printf("after x[%ld][%d](%lf) * y[%ld][%d](%lf), res = %lf\n", test_key, i, x[i], train_key, i, y[i], res);
    }
    return res;
}
jint train_dot_product(const int dimension, const int kn, const int order, char *output, int *outputsize, const int output_capacity, const int test_start, const int test_end, const int mkl_thread, const int omp_thread, const int _debug)
{
    int rs = ERR_CODE_OK;
    int size_vector = dimension * SIZE_OF_DOUBLE;
    int size_line = SIZE_LONG + dimension * SIZE_OF_DOUBLE;
    int size_result = SIZE_OF_LONG + SIZE_OF_LONG + SIZE_OF_DOUBLE;
    int num_threads;
//    double res = 0.0;
    if(mkl_thread)
        mkl_set_num_threads(mkl_thread);
    if(omp_thread)
	num_threads = omp_thread;
    else
        num_threads = getNumThreads();
    printf("mkl_thread = %d, omp_thread = %d\n", mkl_thread, num_threads);
    if(_debug)
	cout << "train_num = " << train_num << "; test_start = " << test_start << "; test_end = " << test_end << "; output_capacity = " << output_capacity  << " testKey[" << test_end -1 << "]: " << testKeys[test_end - 1] << " testKey[0]: " << testKeys[0] << "; kn: " << kn << endl;
    int i, j, k;
//    printf("/*****************launching %d threads.**********************/\n", num_threads);
#pragma omp parallel for num_threads(num_threads) private(i, j, k) shared(output, trains, tests)
    for (i = test_start; i < test_end; i++)
    {
        
//        	    if(_debug)
//    	                 printf("test_cases[%d] ", i);
	if(kn <= train_num)
	{
            KeyDistance *heap = new KeyDistance[kn]();
//	    if(_debug)
//                for (k = 0; k < kn; k++)
//                    printf("heap[%d].key = %ld, heap[%d].distance = %lf\n", k, heap[k].key, k, heap[k].distance);
            for (k = 0; k < kn ; k++)
            {
// 	            int _debug = 0;
//                   double res = ddot(dimension, &trains[k * dimension], &tests[i * dimension]);
//		    for (int d = 0; d < dimension; d++)
//                        res += trains[k * dimension + d] * tests[i * dimension + d];
                 double res = cblas_ddot(dimension, &trains[k * dimension], 1, &tests[i * dimension], 1);
//        	 if(_debug)
//    	              printf("begin dot product train_cases[%ld] and test_cases[%ld], d: %lf\n", trainKeys[k], testKeys[i], res);
		heap[k].key = trainKeys[k];
		heap[k].distance = res;
            }
	    BuildHeap(heap, (kn-1));
	    for (; k < train_num; k++)
	    {
//		    for (int d = 0; d < dimension; d++)
//                        res += trains[k * dimension + d] * tests[i * dimension + d];
//               double res = ddot(dimension, &trains[k * dimension], &tests[i * dimension]);
                double res = cblas_ddot(dimension, &trains[k * dimension], 1, &tests[i * dimension], 1);
//        	 if(_debug)
//    	              printf("begin dot product train_cases[%ld] and test_cases[%ld], d: %lf\n", trainKeys[k], testKeys[i], res);
//		if(res < heap[0].distance)
		if(res > heap[0].distance)
		{
		    heap[0].key = trainKeys[k];
		    heap[0].distance = res;
		    MaxHeap(heap, 0, kn-1);
		}
	    }
            int base = (i - test_start) * kn * size_result;
//	    if(_debug)
//		cout << "i: " << i << "; base: " << base << endl;
	    for(int kk = 0; kk < kn; kk++)
	    {
	        memcpy(output + base + kk * size_result, &testKeys[i], SIZE_OF_LONG);
                if(_debug)
                   printf("putting testKey: %ld, ", testKeys[i]);
	        memcpy(output + base + kk * size_result + SIZE_OF_LONG, &heap[kk].key, SIZE_OF_LONG);
                if(_debug)
                   printf("trainKey: %ld, ", heap[kk].key);
	        memcpy(output + base + kk * size_result + SIZE_OF_LONG * 2, &heap[kk].distance, SIZE_OF_DOUBLE);
                if(_debug)
                   printf("d: %lf to outputbuffer\n",  heap[kk].distance);
	    }
	    delete[] heap;
	}
//	else //TO CHECK
//	{
//            for (k = 0; k < train_num; k++)
//            {
//    	    printf("begin dot product x[%d] and y[%ld]\nvector x:", k, key_tmp);
//    //	    for(int d = 0; d < dimension; d++)
//    //		printf(" %lf", x[k][d]);
//    //	    printf("\n");
//    //            double res = ddot(test_keys[k], key_tmp, dimension, x[k], y);
//                double res = cblas_ddot(dimension, x[k], 1, y, 1);
//                int base = (i * train_num + k) * size_result;
//                memcpy(output + base, &train_keys[k], SIZE_OF_LONG);
//                memcpy(output + base + SIZE_OF_LONG, &key_tmp, SIZE_OF_LONG);
//                memcpy(output + base + SIZE_OF_LONG * 2, &res, SIZE_OF_DOUBLE );
//                if(_debug)
//                    printf("\nbase: %d, testCase: %d, key: %d, d: %lf" ,base, train_keys[k], key_tmp, res);
//            }
//	}
//	}
    }
    *outputsize += (test_end - test_start) * kn * size_result; 
    if(_debug)
        printf("outputsize: %d\n", *outputsize);
//    cout << endl;
//	    printf("%a\n", res);
    return rs;
}

/*************** free_mem_cpu *******************/
jint free_mem_cpu()
{
	printf("free mem of cpu ....\n");
//        int cpid, status;
//        if((cpid = fork()) == -1)	
//	{
//	    cout << "fork kill children err!" << strerror(errno) << endl;
//	}
//	else if(cpid == 0)
//	{
//            cout << "kill children process's ID is: " << getpid() << endl;
//	    system("killall varstream");
//	    string vtune_stop = "/opt/intel/vtune_amplifier_xe_2013/bin64/amplxe-cl -r /newdisk/second/hewt/" + spid  + "  -command stop";
//	    system(vtune_stop.c_str());
//	}
//	while((cpid = waitpid(getpid(), &status, 0) == -1) & (errno == EINTR));
//	if(cpid == -1)
//	    cout << "wait err" << strerror(errno) << endl;
//        cpid = wait(&status);	    
//	if(!status)
//	    cout << "child " << cpid << " terminated normally return status is 0!" << endl;
//        else if(WIFEXITED(status))	
//	    cout << "child " << cpid << " terminated normally return status is " << WEXITSTATUS(status) << "!" << endl;
//	else if(WIFSIGNALED(status))
//	    cout << "child " << cpid << " terminated due to signal " << WTERMSIG(status) << "!" << endl;
	    
	return ERR_CODE_OK;
}
