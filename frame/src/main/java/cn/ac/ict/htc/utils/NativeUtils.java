package cn.ac.ict.htc.utils;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NativeUtils {
	private static final Log LOG = LogFactory.getLog(NativeUtils.class);
	//LOOPBACK
	public static final int CMD_CODE_LOOPBACK = 0;
	//CPU
	public static final int CMD_CODE_INIT_CPU  = 1;
	//GPU
	public static final int CMD_CODE_INIT_GPU = 101;
	
	public static final int CMD_CODE_PUT_OFF = 2;
	public static final int CMD_CODE_PUT_DATA = 3;
	public static final int CMD_CODE_CALC = 4;
	public static final int CMD_CODE_FREE_MEM = 13;
	
	public static final int ERR_CODE_OK = 0;
	public static final int ERR_CODE_CONTINUE = 1;
	public static final int ERR_CODE_ERR = -1;

	static public native int callNative(int cmdCode, ByteBuffer input, ByteBuffer output, String args) throws NativeException;
	
	static
	{
		String path = System.getProperty("java.library.path");
		LOG.debug("java.library.path: " + path);
		//be with VM argument: -Djava.library.path=**
		System.loadLibrary("FrameJni");
	}
}
