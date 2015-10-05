#include "dataTransfer.h"

using namespace std;

int getNativeOrder()
{
	short int x;
	char x0;
	x = 0x1122;
	x0 = ((char *)&x)[0];
	if(x0 == 0x11)
		//big-endian
		return 1;
	else
		//little-endian
		return 0;

}
int inverse_int(char* ch,int pos)
{
	return ch[pos + 3] & 0xff
             |(ch[pos + 2] & 0xff) << 8
             |(ch[pos + 1] & 0xff) << 16
             |(ch[pos]     & 0xff) << 24;
}
float inverse_float(char* ch,int pos)
{
	return ch[pos + 3] & 0xff
             |(ch[pos + 2] & 0xff) << 8
             |(ch[pos + 1] & 0xff) << 16
             |(ch[pos]     & 0xff) << 24;
}
long inverse_long(char* ch,int pos)
{
	return (ch[pos+7]&0xffl)
		|(ch[pos+6]&0xffl)<<8
		|(ch[pos+5]&0xffl)<<16
		|(ch[pos+4]&0xffl)<<24
		|(ch[pos+3]&0xffl)<<32
		|(ch[pos+2]&0xffl)<<40
		|(ch[pos+1]&0xffl)<<48
		|(ch[pos]&0xffl)<<56;
}
double inverse_double(char* ch,int pos)
{
	return (ch[pos+7]&0xffl)
		|(ch[pos+6]&0xffl)<<8
		|(ch[pos+5]&0xffl)<<16
		|(ch[pos+4]&0xffl)<<24
		|(ch[pos+3]&0xffl)<<32
		|(ch[pos+2]&0xffl)<<40
		|(ch[pos+1]&0xffl)<<48
		|(ch[pos]&0xffl)<<56;
}
int getInt(char* ch,int pos,int src_order,int dst_order)
{
	if(src_order == dst_order){
		int *ii = (int*)(ch+pos);
		return ii[0];
	}else{
		return inverse_int(ch,pos);
	}
}
float getFloat(char* ch,int pos,int src_order,int dst_order)
{
	if(src_order == dst_order){
		float* ff = (float*)(ch+pos);
		return ff[0];
	}else{
		return inverse_float(ch,pos);
	}
}
long getLong(char* ch,int pos,int src_order,int dst_order)
{
	if(src_order == dst_order){
		long* t = (long*)(ch+pos);
		return t[0];
	}else{
		return inverse_long(ch,pos);
	}
}
double getDouble(char* ch,int pos,int src_order,int dst_order)
{
	if(src_order == dst_order){
		double* t = (double*)(ch+pos);
		return t[0];
	}else{
		return inverse_double(ch,pos);
	}
}

int getInt_Simple(char* ch,int pos,int order)
{
	return getInt(ch,pos,order,getNativeOrder());
}
float getFloat_Simple(char* ch,int pos,int order)
{
	return getFloat(ch,pos,order,getNativeOrder());
}
long getLong_Simple(char* ch,int pos,int order)
{
	return getLong(ch,pos,order,getNativeOrder());
}
double getDouble_Simple(char* ch,int pos,int order)
{
	return getDouble(ch,pos,order,getNativeOrder());
}


char* bytesToChars(jbyte *b, int bytes_len)
{
	char *str = (char*) malloc((bytes_len + 1) * sizeof(char));
	memcpy(str,(char*)b,bytes_len);
   	str[bytes_len] = '\0';
	return str;
}
jbyte* getValue(jbyte *b, int start, int len)
{
    jbyte *bb;
    bb = (jbyte*)malloc(len);
    for(int i = 0; i < len; i++)
    {
        bb[i] = b[start + i];
    }
    return bb;
}

string getString(jbyte *b, int start, int len)
{
	jbyte * bbb = getValue(b,start,len);
	char * chs = bytesToChars(bbb,len);
	string ss = string(chs);
	free(bbb);
	free(chs);
	return ss;
}
vector<string> split(string str,string pattern)
{
	string::size_type pos;
	vector<string> result;
	str+=pattern;
	int size=str.size();
	for(int i=0; i<size; i++)
	{   
		pos=str.find(pattern,i);
		if(pos<size)
		{   
			string s=str.substr(i,pos-i);
			result.push_back(s);
			i=pos+pattern.size()-1;
		}   
	}   
	return result;
}
void parse_to_map(const char* _cmd_args,map<string,string> &map_args)
{
	vector<string> cmd_line = split(_cmd_args,",");
	for(int i = 0;i<cmd_line.size();i++)
	{
		vector<string> pair_cmd = split(cmd_line[i],":") ;
		map_args.insert(std::pair<string,string>(pair_cmd[0],pair_cmd[1]));
		pair_cmd.clear();
	}
}
/*
char* stringToChars(string str)
{
	return str.data();
}
int stringToInt(string str)
{
	return atoi(str.data());
}
*/
