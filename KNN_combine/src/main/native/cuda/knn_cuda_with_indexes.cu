#include <stdio.h>
#include <sys/time.h>
#include <iostream>
#include <fstream>  
#include <math.h>
#include <map>
#include <cuda_runtime.h>
#include <cublas_v2.h>
#include "cuda.h"
#include "knn.h"
#include <time.h>

using namespace std;

#define TRAIN 1
#define TEST 0

__device__ __constant__ int dimPerThread;
double *trains, *tests;
long *trainKeys, *testKeys;
int dimension = 0;
int dataset = TRAIN;
int test_num = 0;
int train_num = 0;
int _debug = 1;
long trainsmem_size;
CUdevice  cuDevice=0;

/***********************  init_env_gpu ********************/
//malloc global space
jint init_env_gpu(char* src,const int order,const char* args)
{
//	long gpu_size = getLong(src,0,order,0);
	trainsmem_size = getLong_Simple(src,0,order);
        cuDevice=getInt(src,8,order,0);
	cudaSetDevice(cuDevice);

        int _debug = 1;
	if(_debug){
		printf("init_env_gpu to %d device\n", cuDevice);	
		printf("length ..... %d\n",strlen(args));
		if(args != NULL&& strlen(args)!=0)
			printf("init command args :%s\n",args);
	}
	if(args!=NULL && strlen(args)!=0){
		map<string,string> args_map;
		parse_to_map(args,args_map);
//		dimension = getDimension(args_map["dimension"]);
	}
        if(trainsmem_size < (long)(2048 * 1024 * 1024)	)
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
	printf("init_env_gpu. \tsuccess!\n");
	return ERR_CODE_OK;
}

/*************** free_mem_gpu *******************/
jint free_mem_gpu()
{
	return ERR_CODE_OK;
}
/************************ put_data_gpu *******************/

jint put_data_gpu(const char *args,const vector<int> input_offset,char *input_data,const int input_limit,const int order , unsigned int *global_input_mem_size)
{
	int _debug = 0;
	if(_debug){
		printf("put_data_gpu ,\tthe buffer limit is %d.\n",input_limit);
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
		printf("input offset size is %d.\t global mem size is %d\n",input_offset.size(),*global_input_mem_size);
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
//	if(_debug)
//		printf("after trans data ,the local_size is %d.\n " , local_size );
	return ERR_CODE_OK;
}

/************************  calc_gpu *********************/

jint calc_gpu(const char *args,const int order,const unsigned int g_size,char *dst,int *dst_size,const int dst_capacity)
{
    cerr << "beginning calc gpu." << endl;
    map<string,string> cmd_args;
    int k = 1;
    if(args != NULL){
    	if(_debug){
    		printf("calc args : %s\n",args);
    	}
    	parse_to_map(args,cmd_args);
    	if(cmd_args.size()>0){
    	    dimension = getDimension(cmd_args["dimension"]);
    	    k = getK(cmd_args["k"]);
    	    _debug = getDebug(cmd_args["isDebug"]);
    	}
    }
    jint rs = ERR_CODE_OK;
    if(_debug)
        cerr << "initial dst_size: " << *dst_size << ", there are total " << test_num << " test cases" << endl;;
    double* dist_host;
    int*   ind_host;                 // Pointer to index array
    dist_host   = (double *) malloc(test_num * k * sizeof(double));
    ind_host    = (int *)   malloc(test_num * k * sizeof(double));
//    cout << "tests:" << endl;
//    for(int i = 0; i < test_num; i++)
//    {
//	for (int j = 0; j < dimension; j++)
//	    cout << tests[i * dimension + j] << " ";
//	cout << endl;
//    }
//    cout << "trains:" << endl;
//    for(int i = 0; i < k; i++)
//    {
//	for (int j = 0; j < dimension; j++)
//	    cout << trains[i * dimension + j] << " ";
//	cout << endl;
//    }
//    for(int i = 0; i < test_num; i++)
//    {
//	for(int j = 0; j < k; j++)
//	{
//	    double sum = 0.0;
//	    double psum = 0.0;
//	    for(int nn = 0; nn < 8; nn++)
//	    {
//		psum = 0.0;
//		for(int c = 0; c < dimPerThread; c++)
//		    psum += tests[i * dimension + nn * dimPerThread + c] * trains[j * dimension + nn * dimPerThread + c];
//		printf("testId: %d, trainId: %d, part: %d, psum: %lf\n", i, j, nn, psum);
//		sum += psum;
//	    }
//	    printf("testId: %d, trainId: %d, sum: %lf\n", i, j, sum);
//	}
//    }
    findingknn(trains, tests, ind_host, dist_host, dimension, k, dst, dst_size);
    free(ind_host);
    free(dist_host);
    return rs;
}
//-----------------------------------------------------------------------------------------------//
//                                            KERNELS                                            //
//-----------------------------------------------------------------------------------------------//
__host__ __device__ inline static
double shddot(int numCoords,
		double *tests, 
		double *trains,
		int trainId,
		int blockDimx,
		int threadId)
{
	int i;
	double ans = 0.0;
//	for(i = 0; i < dimPerThread; i++) {
	for(i = 0; i < 16; i++) {
//		ans += (tests[i * 32] * trains[trainId * numCoords + (threadId / 32) * dimPerThread + i]);
		ans += (tests[i * 32] * trains[trainId * numCoords + (threadId / 32) * 16 + i]);
//		printf("%d, %lf , %lf\n", i, tests[i * 32], trains[trainId * numCoords + (threadId / 32) * dimPerThread + i]);
	}
	return ans;
}
/*----< find_k_nearest_nei() >---------------------------------------------*/
__global__ static
void findKNN(int numCoords,
                           int test_nb, 
			  int train_nb,
                          int k,
                          double *tests,     
                          double *trains,    
			  double *dist_dev,
                           int *ind_dev)
{

	/*in fact, yIndex is always 0,beacuse our block and grid is one dimemsion*/
    int xIndex = threadIdx.x + blockIdx.x * blockDim.x;
    int yIndex = threadIdx.y + blockIdx.y * blockDim.y;

    int tId = xIndex + yIndex * gridDim.x * blockDim.x;
    int testId = tId / blockDim.x * 32 +tId % 32;
    extern __shared__ int s[];
    if (testId < test_nb) {
//    printf("threadIdx.x: %d, threadIdx.y: %d, blockIdx.x: %d, blockIdx.y: %d, xIndex: %d, yIndex: %d, testId: %d\n", threadIdx.x, threadIdx.y, blockIdx.x, blockIdx.y, xIndex, yIndex, testId);
        int   index, i, min_ind, j; 
        double dist, min_dist;
	int *inds;
	double *dists;
	double *tempDists;
        double *stest;
//	stest = (double *)(s) + (threadIdx.x/32) * dimPerThread * 32 + threadIdx.x % 32;
	stest = (double *)(s) + (threadIdx.x/32) * 16 * 32 + threadIdx.x % 32;
//        for(int i = 0; i < dimPerThread; i++)
        for(int i = 0; i < 16; i++)
        {
//            stest[i  * 32] = tests[testId * numCoords + i + (threadIdx.x / 32) * dimPerThread];
            stest[i  * 32] = tests[testId * numCoords + i + (threadIdx.x / 32) * 16];
        }
	__syncthreads();
	tempDists = (double *)s + numCoords * 32  + threadIdx.x;
//	    printf("test_nb: %d, train_nb: %d, numCoords: %d, k: %d\n", test_nb, train_nb, numCoords, k);
        /* find the cluster id that has min distance to test */
        for(i = 0; i < k; i++)
	{

	    dist =  shddot(numCoords, stest, trains, i, blockDim.x, threadIdx.x);
//            printf("test_id: %d, train_id: %d, threadId: %d, d: %lf\n", testId, i, threadIdx.x, dist);
	    tempDists[0] = dist;
	    __syncthreads();
	    if(threadIdx.x < 128)
                tempDists[0] += tempDists[128];
	    __syncthreads();
	    if(threadIdx.x < 64)
                tempDists[0] += tempDists[64];
	    __syncthreads();
	    if(threadIdx.x < 32)
	    {
                tempDists[0] += tempDists[32];
//		printf("test_id: %d, train_id: %d, distance: %lf\n", testId, i, tempDists[0]);
	        dists = (double *)s + numCoords * 32 + blockDim.x + threadIdx.x;
	        inds = s + (numCoords * 32 + k * 32 + blockDim.x) * 2 + threadIdx.x;
                dists[i * 32] = tempDists[0];
	        inds[i * 32] = i;
	    }
	}
        for (i=k; i<train_nb; i++) {
            dist = shddot(numCoords, stest, trains, i, blockDim.x, threadIdx.x);
	    tempDists[0] = dist;
	    __syncthreads();
	    if(threadIdx.x < 128)
                tempDists[0] += tempDists[128];
	    __syncthreads();
	    if(threadIdx.x < 64)
                tempDists[0] += tempDists[64];
	    __syncthreads();
	    if(threadIdx.x < 32)
	    {
                tempDists[0] += tempDists[32];
//		printf("test_id: %d, train_id: %d, distance: %lf\n", testId, i, tempDists[0]);
	        dists = (double *)s + numCoords * 32 + blockDim.x + threadIdx.x;
	        inds = s + (numCoords * 32 + k * 32 + blockDim.x) * 2 + threadIdx.x;
	        min_dist = dists[0];
	        min_ind = 0;
	        for(index = 1; index < k; index++) 
	        {
	            if(dists[index * 32] < min_dist)
	            {
	                min_dist = dists[index * 32];
	                 min_ind = index;
	            } 
	        }
                if (tempDists[0] > min_dist) { /* find the min and its array index */
                    dists[min_ind * 32] = tempDists[0];
                    inds[min_ind  * 32]  = i; 
                }
            }
        }
	if(threadIdx.x < 32)
	{
//		printf("dimPerThread: %d\n", dimPerThread);
	        dists = (double *)s + numCoords * 32 + blockDim.x + threadIdx.x;
	        inds = s + (numCoords * 32 + k * 32 + blockDim.x) * 2 + threadIdx.x;
            for(i = 0; i < k; i++)
	    {
                dist_dev[testId + i* test_nb] = dists[i* 32 ];
                ind_dev[testId + i * test_nb] = inds[i * 32 ];
//	    if(testId < 32)
//		printf("testId: %d, trainId: %d, dist: %lf\n", testId, ind_dev[testId + i * test_nb], dist_dev[testId + i * test_nb]);
	    }
	}
    }
}
int findingknn(double *ref, double *query, int *ind_host, double *dist_host, int dimension, int k, char *dst, int *dst_size)
{
	double* ref_dev = 0;
	double* query_dev = 0;
        double* dist_dev = 0;                // Pointer to distance array
	int* ind_dev = 0;
	jint rs = ERR_CODE_OK;
        int       max_nb_query_traited;
        size_t       actual_nb_query_width;
        size_t memory_total;

    struct timeval ustart, uend;
    gettimeofday(&ustart, NULL);

	if(_debug)
	    cerr << "dimension at the begining of findingknn: " << dimension << endl;
        // CUDA Initialisation
//	cudaEvent_t debug_start, debug_stop;
//	cudaEventCreate(&debug_start);
//	cudaEventCreate(&debug_stop);
//	cudaEventRecord(debug_start, 0);
//	cudaFree(0);
//	cudaEventRecord(debug_stop, 0);
//	cudaEventSynchronize(debug_stop);
//
//    	float debug_costtime;
//        cudaEventElapsedTime(&debug_costtime, debug_start, debug_stop);
//	printf("initial time: %f\n", debug_costtime);

//	cudaDeviceReset();
//        cuInit(0);
//	cudaDeviceSynchronize();
//    gettimeofday(&uend, NULL);
//    int utime = 1000000 * (uend.tv_sec - ustart.tv_sec) + (uend.tv_usec - ustart.tv_usec);
//    printf("time used for cuInit is: %d us\n", utime);
//    gettimeofday(&ustart, NULL);

        max_nb_query_traited = min( test_num, 8 * 1024 * 64);
	if(_debug)
	    cerr << "totally " << train_num << " train cases, max_nb_query_traited = " << max_nb_query_traited << endl;
        /* Allocate device memory for the matrices */
 
        cudaMalloc((void **)&ref_dev,  dimension * train_num * sizeof(double));
        cudaMalloc((void **)&query_dev, max_nb_query_traited * dimension * sizeof(double)); 
        cudaMalloc((void **)&dist_dev, max_nb_query_traited * k * sizeof(double)); 
        cudaMalloc((void **)&ind_dev, max_nb_query_traited * k * sizeof(int)); 
	cudaDeviceSynchronize();
    gettimeofday(&uend, NULL);
    int utime = 1000000 * (uend.tv_sec - ustart.tv_sec) + (uend.tv_usec - ustart.tv_usec);
    printf("time used for cuda malloc knn is: %d us\n", utime);
    gettimeofday(&ustart, NULL);
 
	cudaMemcpy(ref_dev, ref, train_num * dimension * sizeof(double), cudaMemcpyHostToDevice);
        cudaMemset(dist_dev, 0, max_nb_query_traited * k * sizeof(double));
//	    cerr << cudaGetErrorString(cudaGetLastError()) << endl;
        for (int i=0; i<test_num; i+=max_nb_query_traited){
            
            	// Number of query points considered
        actual_nb_query_width = min( max_nb_query_traited, (int)test_num - i );
        
        // Copy of part of query actually being treated
	cudaMemcpy( (void *)query_dev, ((char *)query) + i * dimension * SIZE_OF_DOUBLE, actual_nb_query_width * dimension * sizeof(double), cudaMemcpyHostToDevice);
//	cerr << cudaGetErrorString(cudaGetLastError()) << endl;
//	int tpb = (49152 / (12 * k + dimension * 8) ) / 32 * 32;
//	if(tpb > 192)
//	    tpb = 192;
	int tpb = 256;
        dim3 g_512x1((actual_nb_query_width + 32 - 1)/32, 1, 1);
        dim3 t_512x1(tpb, 1, 1);
        cerr << "launching kernel with " << (actual_nb_query_width + 32 - 1)/32 << ", " << tpb << endl; 
	cudaDeviceSynchronize();
    gettimeofday(&uend, NULL);
    utime = 1000000 * (uend.tv_sec - ustart.tv_sec) + (uend.tv_usec - ustart.tv_usec);
    printf("time used for preparing calc knn is: %d us\n", utime);
    gettimeofday(&ustart, NULL);
	int dpt = dimension / 8;
	cudaMemcpyToSymbol(dimPerThread, &dpt, sizeof(int), 0, cudaMemcpyHostToDevice);
	findKNN<<<g_512x1, t_512x1, (12 * k + dimension * 8)* 32 + tpb * 8>>>(dimension, actual_nb_query_width, train_num, k, query_dev, ref_dev, dist_dev, ind_dev);
	cudaDeviceSynchronize();
    gettimeofday(&uend, NULL);
    utime = 1000000 * (uend.tv_sec - ustart.tv_sec) + (uend.tv_usec - ustart.tv_usec);
    printf("time used for knn kernel is: %d us\n", utime);
    gettimeofday(&ustart, NULL);
	cerr << cudaGetErrorString(cudaGetLastError()) << endl;
	cudaMemcpy((void *)dist_host, (void *)dist_dev, max_nb_query_traited * k * sizeof(double), cudaMemcpyDeviceToHost);
	cerr << cudaGetErrorString(cudaGetLastError()) << endl;
	cudaMemcpy((void *)ind_host, (void *)ind_dev, max_nb_query_traited * k * sizeof(int), cudaMemcpyDeviceToHost);
	cerr << cudaGetErrorString(cudaGetLastError()) << endl;
	cudaDeviceSynchronize();
//	if(_debug)
//	{
//            for(int  ii = 0; ii < actual_nb_query_width; ii++)
//	    {
//                for(int kk = 0; kk < k; kk++)
//                {
//	             printf("testIdx: %d, trainInx: %d, dist_host[%d]: %lf ", i + ii, ind_host[kk * actual_nb_query_width + ii], kk, dist_host[actual_nb_query_width * kk + ii]);
//	        }
//	          printf("\n");
//	    }  
//	}
        for(int ii = 0; ii < actual_nb_query_width; ii++)
            for(int kk = 0; kk < k; kk++) 
            {
//	        _debug = 1;
//                if(_debug)
//	        {
//                    cerr << "putting testKey: " << testKeys[i + ii];
//                    cerr << ", trainIndex: " << ind_host[kk * actual_nb_query_width + ii];
//                    cerr << ", trainKey: " << trainKeys[ind_host[actual_nb_query_width * kk + ii]];
//                    cerr << ", d: " << dist_host[actual_nb_query_width * kk + ii] << "to outputbuffer" << endl;
//	        }
                memcpy(dst + *dst_size, &testKeys[i + ii], SIZE_OF_LONG);
                *dst_size += SIZE_OF_LONG;
                memcpy(dst + *dst_size, &trainKeys[ind_host[actual_nb_query_width * kk + ii]], SIZE_OF_LONG);
                *dst_size += SIZE_OF_LONG;
                memcpy(dst + *dst_size, &dist_host[actual_nb_query_width * kk + ii], SIZE_OF_DOUBLE);
                *dst_size += SIZE_OF_DOUBLE;
	    } 
	}
    gettimeofday(&uend, NULL);
    utime = 1000000 * (uend.tv_sec - ustart.tv_sec) + (uend.tv_usec - ustart.tv_usec);
    printf("time used for cp result is: %d us\n", utime);
	cerr << "dst_size: " << *dst_size << endl; 
        return rs;  
}
