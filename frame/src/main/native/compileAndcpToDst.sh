g++ -O3 -fPIC -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux -shared -o libFrameJni.so cn_ac_ict_htc_utils_NativeUtils.cpp dataTransfer.cpp
cp libFrameJni.so ${HADOOP_HOME}/lib/native/
scp libFrameJni.so hadoop2:${HADOOP_HOME}/lib/native/
