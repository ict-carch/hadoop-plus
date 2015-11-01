#include "knn.h"

#define NUM_THREAD omp_get_num_procs()

int t_mem_default_size= 1024*1024*100;
char* t_mem = (char*)malloc(t_mem_default_size);//100M
int t_mem_split=1024*1024*5;
//
int num_splits = 0;
int num_threads = 0;


int getNumThreads()
{
	if(num_threads!=0)
		return num_threads;
	int max_thread_by_mem = t_mem_default_size/t_mem_split;
	num_threads = min(max_thread_by_mem,NUM_THREAD);
	return num_threads;
}

int getTempMemSplit()
{
	if(num_splits!=0)
		return num_splits;
	int avg_split = t_mem_default_size/getNumThreads();
	return avg_split;
}
/*
 * return the temp mem
 */
char *getTempMem(int thread_num)
{
	if(t_mem==NULL){
		t_mem = (char*)malloc(t_mem_default_size);
	}
	if(num_splits == 0)
		num_splits = getTempMemSplit();
	return t_mem+thread_num*num_splits;
}
void freeTempMem()
{
	free(t_mem);
	t_mem=NULL;
}
int getDataset(string cmd_code)
{
	return cmd_code.at(0)-'0';
}

int getDimension(string dimen)
{
	return atoi(dimen.data());
}
int getMKLthread(string k)
{
	return atoi(k.data());
}
int getOMPthread(string k)
{
	return atoi(k.data());
}
int getDebug(string k)
{
	return atoi(k.data());
}
int getK(string k)
{
	return atoi(k.data());
}
double *getDoubleVector(char *ch, int pos, int dimension, int order)
{
    double *v = (double *)malloc(dimension * SIZE_OF_DOUBLE);
    for(int i = 0; i < dimension; i++)
    {
        v[i] = getDouble_Simple(ch, pos + i * SIZE_OF_DOUBLE, order);
    }
    return v;
}
