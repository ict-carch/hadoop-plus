#g++ -fPIC -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux -I.  -L .  -shared -ldataTrans -O3 -o libFrameJni.so cn_ac_ict_htc_utils_NativeUtils.cpp 
g++ -fPIC -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux -I.  -L .  -shared -ldataTrans -o libFrameJni.so cn_ac_ict_htc_utils_NativeUtils.cpp 
cp libFrameJni.so ${HADOOP_HOME}/lib/native/
scp libFrameJni.so hadoop2:${HADOOP_HOME}/lib/native/
scp libFrameJni.so hadoop4:${HADOOP_HOME}/lib/native/
