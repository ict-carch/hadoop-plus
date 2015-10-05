#g++ -fPIC -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux -I . -shared -O3 -o libdataTrans.so dataTransfer.cpp
g++ -fPIC -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux -I . -shared  -o libdataTrans.so dataTransfer.cpp
cp libdataTrans.so ${HADOOP_HOME}/lib/native/
scp libdataTrans.so hadoop2:${HADOOP_HOME}/lib/native/
scp libdataTrans.so hadoop4:${HADOOP_HOME}/lib/native/
