#include <iostream>
#include <jni.h>
#include <string.h>
#include <vector>
#include <map>
#include <math.h>
#include "malloc.h"
using namespace std;
extern "C"
{
	int getNativeOrder();

	int inverse_int(char*,int);
	float inverse_float(char*,int);
	long inverse_long(char*,int);
	double inverse_double(char*,int);

	//default dst order is native order
	int getInt_Simple(char*, int, int);
	long getLong_Simple(char* ,int,int);
	float getFloat_Simple(char*,int ,int);
	double getDouble_Simple(char*,int,int);

	//args1:src 
	//args2:pos
	//args3:src order
	//args4:dst order
	int getInt(char*, int, int,int);
	long getLong(char* ,int,int,int);
	float getFloat(char*,int ,int,int);
	double getDouble(char*,int,int,int);

	char* bytesToChars(jbyte *b, int bytes_len);
	jbyte* getValue(jbyte *b, int start, int len);
	string getString(jbyte *b, int start, int len);
	vector<string> split(string str,string pattern);
	void parse_to_map(const char*,map<string,string> &);
}
