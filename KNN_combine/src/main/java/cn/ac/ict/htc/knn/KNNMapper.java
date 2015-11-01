package cn.ac.ict.htc.knn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Vector;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapreduce.Mapper;

import cn.ac.ict.htc.mapreduce.ICTMapper;
import cn.ac.ict.htc.utils.NativeException;
import cn.ac.ict.htc.utils.PairOfByteBuffers;
import cn.ac.ict.htc.utils.NativeUtils;
import cn.ac.ict.htc.tools.ArrayListWritable;
import cn.ac.ict.htc.tools.NumberListWritable;
import cn.ac.ict.htc.knn.data.*;

public class KNNMapper extends
		ICTMapper<Text, NumberListWritable<Double>, Text, Vector2SF> {
		
//// vars in Java impl.
    double[][] tests;
    double[] trainVector;
    String[] testKeys;
	int kn, k, test_num;
	double[][] results;
	String[][] trainKeys;
//// vars in GPU impl.	
	int coords = 0;
	String cmd = "dimension:";
	private static final Log logger = LogFactory.getLog(KNNMapper.class);
    protected void JAVAmap(
        Text key,
        NumberListWritable<Double> value,
        org.apache.hadoop.mapreduce.Mapper<Text, NumberListWritable<Double>, Text, Vector2SF>.Context context)
        throws java.io.IOException, InterruptedException {
        context.setStatus (key.toString());
		int test_id = 0, i = 0;
        Iterator<Double> iter = value.get().iterator();
//		System.err.println("coords in Java map: " + coords + "init vector: ");
//		for(i = 0; i < coords; i++) {
//			System.err.println(trainVector[i]);
//		}
//		i = 0;
//		logger.info("ele in iter: " + iter.next().doubleValue());
        while (iter.hasNext()) {
            Double e = iter.next();
		    trainVector[i] = e.doubleValue();
//		    System.err.println("trainId: " + k + ", dim: " + i + ":: " + trainVector[i]);
		    i++;
        }
	  
        for (test_id = 0; test_id < test_num; test_id++) {
            double d = 0.0;
	    	for (int c = 0; c < coords; c++)
	    	{
				d += trainVector[c] * tests[test_id][c];
////			System.err.println("testId: " + test_id + ", trainId: " + k + ", dim: " + c + ":: " + tests[test_id][c] + ", " + trainVector[c]);
	    	}
			if(k < kn)
			{
//				System.err.println("test_id: " + test_id);
//				for(i = 0; i < kn; i++) {
//					System.err.println(results[test_id][i]);
//				}
				results[test_id][k] = d;
//			    System.err.println("trainId: " + key.toString());
				trainKeys[test_id][k] = new String(key.toString());
			}
			else
			{
//				System.err.println("test_id: " + test_id);
//				for(i = 0; i < kn; i++) {
//					System.err.println(results[test_id][i]);
//				}
			    double min = results[test_id][0];
				int ind = 0;
				for (i = 1; i < kn; i++)
				{
				    if(results[test_id][i] < min)
					{
						min = results[test_id][i];
						ind = i;
					}
					if(d > min)
					{
						results[test_id][ind] = d;
						trainKeys[test_id][ind] = key.toString();
					}
				}
			}
        }
		k++;

    }
    
    ;
	protected int calc(Context context) throws IOException,
			InterruptedException {
		String cmd_calc = cmd  + coords + ",k:" + context.getConfiguration().get("cn.ac.ict.htc.knn.k", "4") 
				+ ",mkl_thread:" + context.getConfiguration().get("cn.ac.ict.htc.knn.mkl_thread", "0")
				+ ",omp_thread:" + context.getConfiguration().get("cn.ac.ict.htc.knn.omp_thread", "0")
				 + ",isDebug:" + context.getConfiguration().get("cn.ac.ict.htc.knn.isDebug", "0")
				 + ",factor:" + context.getConfiguration().get("cn.ac.ict.htc.knn.factor", "0")
				 + ",streamNum:" + context.getConfiguration().get("cn.ac.ict.htc.knn.streamNum", "0");
		logger.info("calc_cmd: " + cmd_calc);
		setCommandLineForJNI(cmd_calc);
		int retval = NativeUtils.ERR_CODE_CONTINUE;
		while(retval == NativeUtils.ERR_CODE_CONTINUE){
			long start = System.nanoTime();
			ByteBuffer output = getOutputBuffer();
			long elapsed = System.nanoTime() - start;
			logger.info("time for native calc knn is: " + elapsed + " ns");
			retval = checkCalcResult();
			int output_limit = output.limit();
			logger.info("out buffer position: " + output.position());
			logger.info("out buffer limit: " + output_limit);
			
			while (output.position() < output_limit) {
				context.write(new Text(output.getLong() + ""),
						new Vector2SF(output.getLong() + "", output.getDouble()));
			}
		}
		return 0;
	}
	@Override
	protected void parseRecordintoBuffer(
        Text key,
        NumberListWritable<Double> value){
		pairOfBuffer.offset.putInt( pairOfBuffer.value.position());
		pairOfBuffer.value.putLong(new Long(key.toString()));//key
		for(Iterator<Double> iter = value.get().iterator();iter.hasNext();){
			Double e = iter.next();
			pairOfBuffer.value.putDouble(e);
//			System.err.println("putting " + e.floatValue() + " to pairOfBuffer.value.");
//			count++;
		}
	}
	@Override
	protected void setup(Context context)
			throws java.io.IOException, InterruptedException {

		super.setup(context);
		logger.info("The impl.this container would run is: " + container_type);
		System.out.println("loading shared comparison vectors...");

		// load the test vectors
		FileSystem fs = FileSystem.get(context.getConfiguration());
		SequenceFile.Reader reader = new SequenceFile.Reader(FileSystem.get(context.getConfiguration()),
			new Path(context.getConfiguration().get("cn.ac.ict.htc.knn.test", "")), context.getConfiguration());
        NumberListWritable<Double> testValue;
        LongWritable testKey;
		try {
            testKey = (LongWritable)reader.getKeyClass().newInstance();
            testValue = (NumberListWritable)reader.getValueClass().newInstance();
			if(container_type == Container_Type.JAVA) {
				kn = context.getConfiguration().getInt("cn.ac.ict.htc.knn.k", 4);
		        tests = new double[36864][128];
        		testKeys = new String[36864];
            	while (reader.next(testKey, testValue)) {
                	Iterator<Double> iter = testValue.get().iterator();
	                coords = 0;
     	            while (iter.hasNext()) {
         	            Double e = iter.next();
		    			tests[test_num][coords++] = e.doubleValue();
//						System.err.println("coords: " + coords);
                	}
					testKeys[test_num] = testKey.get() + "";
            	    test_num++;
				}
				System.err.println("total test number: " + test_num);
				results = new double[test_num][kn]; 
				trainKeys = new String[test_num][kn]; 
				trainVector = new double[coords];
//				System.err.println("coords after finishing setup: " + coords + "init vector: ");
//				for(int i = 0; i < coords; i++) {
//					System.err.println(trainVector[i]);
//				}
			}
			else {
			    PairOfByteBuffers pairOfBuffer = new PairOfByteBuffers(5*1024*1024,100*1024*1024);
			    int halfCapacity = pairOfBuffer.value.capacity() / 2;
			    pairOfBuffer.offset.clear();
			    pairOfBuffer.value.clear();
			    while (reader.next(testKey, testValue)) {
			    	Iterator<Double> iter = testValue.get().iterator();
			    	pairOfBuffer.offset.putInt(pairOfBuffer.value.position());
			    	pairOfBuffer.value.putLong(testKey.get());
			    	coords = 0;
			    	while (pairOfBuffer.value.position() < halfCapacity && iter.hasNext()) {
			    	    Double e = iter.next();
	
			    	    pairOfBuffer.value.putDouble(e);
//			    	    if(context.getConfiguration().get("cn.ac.ict.htc.knn.isDebug", "0").equals("1"))
//			    	        System.out.print(e + " ");
			    	    coords++;
			    	}
			    }
		    	String cmd_test = cmd  + coords + ",dataset:" + KNNDriver.TEST
				 + ",isDebug:" + context.getConfiguration().get("cn.ac.ict.htc.knn.isDebug", "0");
		    	setCommandLineForJNI(cmd_test);

		    	pairOfBuffer.offset.flip();
		    	pairOfBuffer.value.flip();
		    	try {
		    		NativeUtils.callNative(NativeUtils.CMD_CODE_PUT_OFF,
		    				pairOfBuffer.offset, null, cmd_test);
		    		NativeUtils.callNative(NativeUtils.CMD_CODE_PUT_DATA,
		    				pairOfBuffer.value, null, cmd_test);
		    	} catch (NativeException e) {

		    	}
				String cmd_train = cmd + coords + ",dataset:" + KNNDriver.TRAIN
						 + ",isDebug:" + context.getConfiguration().get("cn.ac.ict.htc.knn.isDebug", "0")
						 + ",blockSize:" + context.getConfiguration().get("cn.ac.ict.htc.knn.blockSize", "0");
				setCommandLineForJNI(cmd_train);
			}
		} catch (Exception e){
		}
		reader.close();
		System.out.println("done.");
	}
}
