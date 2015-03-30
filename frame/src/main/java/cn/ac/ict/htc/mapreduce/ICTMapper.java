package cn.ac.ict.htc.mapreduce;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Mapper;

import cn.ac.ict.htc.utils.NativeException;
import cn.ac.ict.htc.utils.NativeUtils;
import cn.ac.ict.htc.utils.PairOfByteBuffers;

public abstract class ICTMapper<KEYIN,VALUEIN,KEYOUT,VALUEOUT> extends
		Mapper<KEYIN,VALUEIN,KEYOUT,VALUEOUT> {
	private static final Log logger = LogFactory.getLog(ICTMapper.class);
	public static final String CLASS_NAME = ICTMapper.class.getName();
	//CPU
	public static final String EXPECT_GLOBAL_MEM_CPU = CLASS_NAME + ".expect.global.mem.cpu";
	public static final String EXPECT_OUTPUT_BUFFER_CAPACITY_CPU = CLASS_NAME + ".expect.output.buffer.capacity.cpu";
	public static final String INITIAL_ARGS_CPU = CLASS_NAME+".initial.args.cpu";
	public static final String LIBRARY_NAME_CPU = CLASS_NAME + ".library.name.cpu";
	//GPU
	public static final String EXPECT_GLOBAL_MEM_GPU = CLASS_NAME + ".expect.global.mem.gpu";
	public static final String EXPECT_OUTPUT_BUFFER_CAPACITY_GPU = CLASS_NAME + ".expect.output.buffer.capacity.gpu";
	public static final String INITIAL_ARGS_GPU = CLASS_NAME+".initial.args.gpu";
	public static final String LIBRARY_NAME_GPU = CLASS_NAME + ".library.name.gpu";
	//
	public static final String PREF_RESOURCE = CLASS_NAME+".pref.resource";
	//default value
	public static final long DEFAULT_GLOBAL_MEM = 1024L * 1024L * 500L;  								//default 500m
	public static final int DEFAULT_OUTPUT_CAPACITY = 1024 * 1024 * 100;							//default 100m
	public static final String DEFAULT_ARGS = null;																//default null
	public static final Pref_Resource PREF_RESOURCE_DEFAULT = Pref_Resource.BOTH;			//default both
	public static final String DEFAULT_LIBRARY_NAME = "libUserFunctions.so";
	//
	public static final int LONG_SIZE = 8;
	public static final int DOUBLE_SIZE = 8;
	public static final int FLOAT_SIZE = 4;
	public static final int INT_SIZE = 4;
	public static final int ONE_K = 1024;
	//default : navtiveOrder
	public static final ByteOrder DEFAULT_ORDER = ByteOrder.nativeOrder(); 
	//flag
	protected enum FlagRes{CPU,GPU}
	//variable
	private ByteBuffer output_buffer = null;																		//null
	private FlagRes res_last = null; 																					//null
	private String cmd_args = null;
	private ByteBuffer init_buffer = null;																			//content:global_size(8)+order(1)+lib_name(*)
	private String lib_name = ICTMapper.DEFAULT_LIBRARY_NAME;
	
	private int calc_rs = NativeUtils.ERR_CODE_OK;

	/**
	 * initial jni_env , CPU or GPU
	 * some case ,you will need override this method. 
	 * if you wish to do more things ,should call super.setup(context);(important)
	 */
	protected void setup(Context context) throws IOException,
			InterruptedException {
		super.setup(context);
		Configuration conf = context.getConfiguration();
		Pref_Resource pref_Resource = Pref_Resource.getPref_Resource(conf.get(ICTMapper.PREF_RESOURCE , ICTMapper.PREF_RESOURCE_DEFAULT.toString()));
		logger.info("pref_Resource conf >>>> " + pref_Resource.toString());
		//
		int ret_code = NativeUtils.ERR_CODE_ERR;
		init_buffer = ByteBuffer.allocateDirect(ONE_K);
		init_buffer.order(ICTMapper.DEFAULT_ORDER);
		init_buffer.clear();
		if(pref_Resource == Pref_Resource.BOTH){
			//initial GPU
			if(getGpus()>0&&getGpuID()!=-1){
				ret_code = initialResource(context, init_buffer, FlagRes.GPU);
			}else{
				logger.error("initial gpu failed,there are no gpus");
			}
			if(ret_code==NativeUtils.ERR_CODE_ERR){
				logger.error("trying  initial CPU!");
				ret_code = initialResource(context, init_buffer, FlagRes.CPU);
			}
		}else if(pref_Resource == Pref_Resource.ONLY_GPU){
			if(getGpus() > 0&&getGpuID()!=-1){
				ret_code = initialResource(context, init_buffer, FlagRes.GPU);
			}else{
				logger.error("initial gpu failed,there are no gpus");
			}
		}else if(pref_Resource == Pref_Resource.ONLY_CPU){
			ret_code = initialResource(context, init_buffer, FlagRes.CPU);
		}
		if(ret_code == NativeUtils.ERR_CODE_ERR){
			logger.error("initial failed,will shutdown ..");
			throw new RuntimeException("initial failed,the resource lack ...");
		}
		cmd_args = ICTMapper.DEFAULT_ARGS;
	}

	/**
	 * most case ,you should not override this method. 
	 * but you must implement calc();
	 */
	public void run(Context context) throws IOException, InterruptedException {
		setup(context);
	    try {
            Date t_start = new Date();
	      while (context.nextKeyValue()) {
	        map(context.getCurrentKey(), context.getCurrentValue(), context);
	      }
            Date t_end = new Date();
            logger.info("put data time:" + (t_end.getTime()-t_start.getTime()));
	      int state = calc(context);
	      logger.info("the calc state is :"+ state);
	    } finally {
	      cleanup(context);
	    }
	}

	/**
	 * most case, you do not need override map() .
	 * but if you want to transmit arguments to jni_env ,
	 * you should call the method "setCommandLineForJNI()" .
	 */
	protected void map(KEYIN key, VALUEIN info, Context context)
			throws IOException, InterruptedException {
		int rs = NativeUtils.ERR_CODE_ERR;
		PairOfByteBuffers pair = null;
		if(info instanceof PairOfByteBuffers){
			pair = (PairOfByteBuffers)info;
		}else{
			throw new IOException("The VALUEIN is not a instance of PairOfByteBuffers!");
		}
		try {
			rs = NativeUtils.callNative(NativeUtils.CMD_CODE_PUT_OFF,
					pair.offset, null, cmd_args);
			Assert.assertTrue(rs == NativeUtils.ERR_CODE_OK);
			rs = NativeUtils.callNative(NativeUtils.CMD_CODE_PUT_DATA,
					pair.value, null, cmd_args);
			cmd_args = ICTMapper.DEFAULT_ARGS;// frame will hold the cmd_args until cmd_args changed
			Assert.assertTrue(rs == NativeUtils.ERR_CODE_OK);
		} catch (NativeException e) {
			logger.error(e);
			throw new RuntimeException(e);
		}
		//clean
		pair.clear();
	}
	
	/**
	 * do clean and more free the JNI -- CPU or GPU memory .
	 * some case ,you may wish to do other thing here,
	 * you have to do these cleaning job yourself.
	 */
	@Override
	protected void cleanup(Context context)
			throws IOException, InterruptedException {
		super.cleanup(context);
		int rs = -1;
		try {
			rs = NativeUtils.callNative(NativeUtils.CMD_CODE_FREE_MEM, null, null, cmd_args);
			Assert.assertTrue(rs == NativeUtils.ERR_CODE_OK);
			cmd_args = ICTMapper.DEFAULT_ARGS;
		}catch(NativeException e){
			logger.error(e);
			throw new RuntimeException(e);
		}
		logger.info("clean JNI Memory  ... OK");
	}

	/**
	 * override this method,do the calc job here
	 * @param context
	 * @return return the status of calc
	 * @throws IOException
	 * @throws InterruptedException
	 */
	 protected abstract int calc(Context context) throws IOException,	InterruptedException ;

	/**
	 * 
	 * @return
	 * return the status of JNI call -- do_calc() ,the buffer is ready to read.
	 */
	public ByteBuffer getOutputBuffer() {
		int ret_code = NativeUtils.ERR_CODE_ERR;
		output_buffer.clear();
		logger.info("the output position and limit is " + output_buffer.position() + "," + output_buffer.limit());
		try {
			ret_code = NativeUtils.callNative(NativeUtils.CMD_CODE_CALC, null, output_buffer,
					cmd_args);
			cmd_args = ICTMapper.DEFAULT_ARGS;
			calc_rs = ret_code;
			Assert.assertTrue(ret_code != NativeUtils.ERR_CODE_ERR );
		} catch (NativeException e) {
			logger.error(e);
			throw new RuntimeException(e);
		}
		logger.info("the output position and limit is " + output_buffer.position() + "," + output_buffer.limit());
		output_buffer.flip();
		return output_buffer;
	}
	
	public int checkCalcResult(){
		return calc_rs;
	}
	
	public final FlagRes getFinalFlagRes(){
		return res_last;
	}
	
	/**
	 * set String args command for jni;
	 * @param cmd_args  the args of cmd should like  "key:value,key1:value1"
	 */
	protected void setCommandLineForJNI(String cmd_args){
		this.cmd_args = cmd_args;
	}
	/**
	 * set <String,String> args command for jni
	 */
	protected void setCommandLineForJNI(Map<String, String> cmd_map){
		StringBuffer sb = new StringBuffer();
		Set<String> keySet = cmd_map.keySet();
		for(String key :keySet){
			sb.append(key).append(":").append(cmd_map.get(key)).append(",");
		}
		sb.deleteCharAt(sb.length()-1);
		this.cmd_args =  sb.toString();
	}
	/**
	 * parse the cmd args to map
	 * @param cmd_args
	 * @return
	 */
	protected Map<String, String> getCommandMap(String cmd_args){
		Map<String,String> cmd_map = new HashMap<String, String>();
		if(cmd_args == null){
			return cmd_map;
		}
		String[] cmd_array = cmd_args.split("[:,]");
		for(int i = 0;i<cmd_array.length;){
			cmd_map.put(cmd_array[i], cmd_array[i+1]);
			i+=2;
		}
		return cmd_map;
	}
	
	/**
	 * 
	 * @param context
	 * @param inbuf
	 * @param flag    true represent GPU
	 * @return
	 */
	private int initialResource(Context context ,ByteBuffer inbuf ,final FlagRes flag){
		logger.info("initial Resource :" + flag.toString());
		int ret_code = NativeUtils.ERR_CODE_ERR;
		Configuration conf = context.getConfiguration();
		if(flag == FlagRes.GPU){
			try {
				inbuf.clear();
				lib_name = conf.get(ICTMapper.LIBRARY_NAME_GPU , ICTMapper.DEFAULT_LIBRARY_NAME);
				inbuf.putLong(conf.getLong(ICTMapper.EXPECT_GLOBAL_MEM_GPU, ICTMapper.DEFAULT_GLOBAL_MEM));
                inbuf.putInt(getGpuID());
                inbuf.position(100);//keep extension
				init_buffer.put(lib_name.getBytes());
				inbuf.flip();
				cmd_args = conf.get(ICTMapper.INITIAL_ARGS_GPU, ICTMapper.DEFAULT_ARGS);
				ret_code = NativeUtils.callNative(NativeUtils.CMD_CODE_INIT_GPU, inbuf, null, cmd_args);
			} catch (NativeException e) {
				ret_code = NativeUtils.ERR_CODE_ERR;
			}
		}else if(flag == FlagRes.CPU){
			try {
				inbuf.clear();
				lib_name = conf.get(ICTMapper.LIBRARY_NAME_CPU , ICTMapper.DEFAULT_LIBRARY_NAME);
				inbuf.putLong(conf.getLong(ICTMapper.EXPECT_GLOBAL_MEM_CPU, ICTMapper.DEFAULT_GLOBAL_MEM));
                inbuf.position(100);
				init_buffer.put(lib_name.getBytes());
				inbuf.flip();
				cmd_args = conf.get(ICTMapper.INITIAL_ARGS_CPU, ICTMapper.DEFAULT_ARGS);
				ret_code = NativeUtils.callNative(NativeUtils.CMD_CODE_INIT_CPU, inbuf, null, cmd_args);
			} catch (NativeException e) {
				ret_code = NativeUtils.ERR_CODE_ERR;
			}
		}
		if(ret_code == NativeUtils.ERR_CODE_OK) {
			res_last = flag;// record last res flag
			if(res_last==FlagRes.GPU){
				logger.info("initial GPU OK!");
				output_buffer = ByteBuffer.allocateDirect(conf.getInt(ICTMapper.EXPECT_OUTPUT_BUFFER_CAPACITY_GPU, ICTMapper.DEFAULT_OUTPUT_CAPACITY)); 
			}else{
				logger.info("initial CPU OK!");
				output_buffer = ByteBuffer.allocateDirect(conf.getInt(ICTMapper.EXPECT_OUTPUT_BUFFER_CAPACITY_CPU, ICTMapper.DEFAULT_OUTPUT_CAPACITY));
			}
			output_buffer.order(DEFAULT_ORDER);//out buffer's order is DEFAULT_ORDER also . 
		}
		return ret_code;
	}
	/**
	 * @return
	 * return GPU resource
	 */
	private int getGpus(){
		int gpus = 0;
		String env_gpu = System.getenv("GPU");
		logger.info("gpus is "+env_gpu);
		if(env_gpu !=null){
			gpus = Integer.parseInt(env_gpu); 
		}
		return gpus;
	}

    /**
     * @return
     * return GPU ID
     * [0,1,2,....]/-1
     */
    private int getGpuID(){
        if(getGpus()>0){
            String env_gpu_id = System.getenv("GPU_ID");
            logger.info("gpu_id is :" + env_gpu_id);
            if(env_gpu_id!=null){
                return Integer.parseInt(env_gpu_id);
            }
        }
        return -1;
    }

	/**
	 * CPU or GPU preference: ONLY_CPU , ONLY_GPU or BOTH
	 * @author lubinbin
	 */
	public enum Pref_Resource {
		ONLY_GPU, ONLY_CPU, BOTH;
		public static Pref_Resource getPref_Resource(String prefStr) {
		   logger.info("++++++++++++++" + prefStr); 
			if (prefStr == null)
				return Pref_Resource.BOTH;
			if (prefStr.trim().equalsIgnoreCase("both"))
				return Pref_Resource.BOTH;
			else if (prefStr.trim().equalsIgnoreCase("only_cpu"))
				return Pref_Resource.ONLY_CPU;
			else
				return Pref_Resource.ONLY_GPU;
		}
		public String toString() {
			if (this == Pref_Resource.BOTH)
				return "both";
			else if (this == Pref_Resource.ONLY_CPU)
				return "only_cpu";
			else
				return "only_gpu";
		}
	}
}
