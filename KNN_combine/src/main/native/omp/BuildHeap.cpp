//借助堆，查找最小的k个数  
//copyright@ yansha &&July  
//July、updated，2011.04.28。  
#include <iostream>  
#include <stdio.h>
#include <assert.h>  
#include "knn.h"
using namespace std;  

/*------------------- 
BUILD-MIN-HEAP(A) 
1  heap-size[A] ← length[A] 
2  for i ← |_length[A]/2_| downto 1 
3       do MAX-HEAPIFY(A, i) 
*/  
// 建立大根堆  
void BuildHeap(KeyDistance heap[], int len)  
{  
    if (heap == NULL)  
        return;  
              
    int index = len / 2;  
    for (int i = index; i >= 0; i--)  
       MaxHeap(heap, i, len);  
}  
/*----------------------------   
PARENT(i) 
   return |_i/2_| 
   LEFT(i) 
      return 2i 
   RIGHT(i) 
      return 2i + 1 
      MIN-HEAPIFY(A, i) 
      1 l ← LEFT(i) 
      2 r ← RIGHT(i) 
      3 if l ≤ heap-size[A] and A[l] < A[i] 
      4    then smallest ← l 
      5    else smallest ← i 
      6 if r ≤ heap-size[A] and A[r] < A[smallest] 
      7    then smallest ← r 
      8 if smallest ≠ i 
      9    then exchange A[i] <-> A[smallest] 
      10         MIN-HEAPIFY(A, smallest) 
      */  
//调整大根堆  
void MaxHeap(KeyDistance heap[], int i, int len)  
{  
    int largeIndex = -1;  
    int left = i * 2;  
    int right = i * 2 + 1;  
          
//    if (left <= len && heap[left].distance > heap[i].distance)  
    if (left <= len && heap[left].distance < heap[i].distance)  
        largeIndex = left;  
    else  
        largeIndex = i;  
              
//    if (right <= len && heap[right].distance > heap[largeIndex].distance)  
    if (right <= len && heap[right].distance < heap[largeIndex].distance)  
        largeIndex = right;  
              
    if (largeIndex != i)  
    {  
        KeyDistance tmp(heap[i]);
        heap[i].key = heap[largeIndex].key;
        heap[i].distance = heap[largeIndex].distance;
        heap[largeIndex].key = tmp.key;
        heap[largeIndex].distance = tmp.distance;
	
//        swap(heap[i], heap[largeIndex]);  
        MaxHeap(heap, largeIndex, len);  
    }  
}  
//int main()  
//{  
//    // 定义数组存储堆元素  
//    int k;  
//    cout << "input the scale of heap k = ";
//    cin >> k;  
////    int *heap = new int [k+1];   //注，只需申请存储k个数的数组  
////    FILE *fp = fopen("data.txt", "r");   //从文件导入海量数据（便于测试，只截取了9M的数据大小）  
////    assert(fp);  
////          
////    for (int i = 1; i <= k; i++)  
////        fscanf(fp, "%d ", &heap[i]);  
//
//    int *heap = new int[k]();
//    for (int i = 0; i < k; i++)  
//        scanf("%d ", &heap[i]);  
//    BuildHeap(heap, k-1);      //建堆  
//          
//    int newData;  
////    while (fscanf(fp, "%d", &newData) != EOF)  
//    scanf("%d", &newData);  
////    {  
//        if (newData < heap[0])   //如果遇到比堆顶元素kmax更小的，则更新堆  
//        {  
//            heap[0] = newData;  
//            MaxHeap(heap, 0, k-1);   //调整堆  
//        }  
//                  
////    }  
//          
//    for (int j = 0; j < k; j++)  
//        cout << heap[j] << " ";  
//    cout << endl;  
//          
////    fclose(fp);  
//    return 0;  
//}  
