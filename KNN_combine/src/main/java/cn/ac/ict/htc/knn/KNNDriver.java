package cn.ac.ict.htc.knn;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import cn.ac.ict.htc.knn.data.VectorSequenceFileInputFormat;
import cn.ac.ict.htc.knn.data.NewARFFInputformat;
import cn.ac.ict.htc.knn.data.ARFFOutputFormat;
import cn.ac.ict.htc.knn.data.SparseVector;
import cn.ac.ict.htc.knn.data.Vector2SF;
import cn.ac.ict.htc.mapreduce.ICTMapper;
import cn.ac.ict.htc.job.AbstractJob;

public class KNNDriver extends AbstractJob {
	private static final Log logger = LogFactory.getLog(KNNDriver.class);
	
	public static final int TRAIN = 1;
	public static final int TEST = 0;
	
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new KNNDriver(), args);
		System.exit(res);
	}
	
	public static long parseByteNum(String sizeStr){
		Double base = Double.parseDouble(sizeStr.replaceAll("[GMK]B",""));

		final long sizeBytes;
		
		if ( sizeStr.endsWith("GB") ) {
		    sizeBytes = 1024*1024*1024*base.longValue();
		}
		else if ( sizeStr.endsWith("MB") ) {
		    sizeBytes = 1024*1024*base.longValue();
		}
		else {
		    sizeBytes = 1024*base.longValue();
		}
		return sizeBytes;
	}

	public int run(String[] args) throws Exception {

		logger.info(">>>>begin run");
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args)
				.getRemainingArgs();
		
		addOption(buildOption("i", "train", "train file path", true, true));
		addOption(buildOption("c", "cons_num", "number of corun interested containers", true, false));
		addOption(buildOption("hc", "hcons_num", "number of corun Hadoop containers", true, false));
		addOption(buildOption("o", "out", "output file path", true, true));
		addOption(buildOption("t", "test", "test file path", true, true));
		addOption(buildOption("k", "k", "number of nn", true, false));
		addOption(buildOption("mt", "mkl_thread", "thread number of mkl", true, false));
		addOption(buildOption("ot", "omp_thread", "thread number of omp", true, false));
		addOption(buildOption("s", "minsplitsize", "splitsize", true, false));
		addOption(buildOption("b", "isDebug", "is Debug", true, false));
		addOption(buildOption("r", "resource", "type of prefered resource", true, false));
		addOption(buildOption("f", "factor", "degree of reducing stream copy", true, false));
		addOption(buildOption("bs", "bs", "blocksize for dd read", true, false));
		addOption(buildOption("ds", "dd_num", "number of dd read process", true, false));
		addOption(buildOption("ns", "streamNum", "number of stream process", true, false));
		
		Map<String,String> map = parseArguments(otherArgs);
		Path traindir = new Path(map.get("train"));
		String k = map.get("k");
		String mkl_thread = map.get("mkl_thread");
		String omp_thread = map.get("omp_thread");
		String minsplitsize = map.get("minsplitsize");
		String isDebug = map.get("isDebug");
		String resource = map.get("resource");
		String factor = map.get("factor");
		String blockSize = map.get("bs");
		String ddNum = map.get("dd_num");
		String conNum = map.get("cons_num");
		String hconNum = map.get("hcons_num");
		String streamNum = map.get("streamNum");
		
		long splitSize = minsplitsize==null?1L<<28:parseByteNum(minsplitsize);
		System.out.println();
		conf.setLong(FileInputFormat.SPLIT_MINSIZE, splitSize);
		// conf.setInt("output_capacity", 1024*1024*100);
		conf.set("mapred.textoutputformat.separator", ",");
		// disable timeout of a mr task (DANGEROUS!!!)
		conf.setLong("mapreduce.task.timeout", 0);
		if(k != null)
			conf.set("cn.ac.ict.htc.knn.k", k);
		if(mkl_thread != null)
			conf.set("cn.ac.ict.htc.knn.mkl_thread", mkl_thread);
		if(omp_thread != null)
		    conf.set("cn.ac.ict.htc.knn.omp_thread", omp_thread);
		if(isDebug != null)
                    conf.set("cn.ac.ict.htc.knn.isDebug", isDebug);
		if(resource != null)
                    conf.set("cn.ac.ict.htc.knn.resource", resource);
		if(factor != null)
                    conf.set("cn.ac.ict.htc.knn.factor", factor);
		if(blockSize != null)
                    conf.set("cn.ac.ict.htc.knn.blockSize", blockSize);
		if(ddNum != null)
                    conf.set("cn.ac.ict.htc.knn.ddNum", ddNum);
		if(conNum != null)
                    conf.set("cn.ac.ict.htc.knn.conNum", conNum);
		if(hconNum != null)
                    conf.set("cn.ac.ict.htc.knn.hconNum", hconNum);
		if(streamNum != null)
                    conf.set("cn.ac.ict.htc.knn.streamNum", streamNum);
		
		
		for (FileStatus fs : FileSystem.get(conf).listStatus(
				new Path(map.get("test") ))) {
			conf.set("cn.ac.ict.htc.knn.test", fs.getPath().toString());
			System.out.println("current test file"
					+ conf.get("cn.ac.ict.htc.knn.test"));
		        Path output = new Path(map.get("out"));
		        FileSystem.get(conf).delete(output, true);

			String isHasGPU	= System.getProperty("isHasGpu");
			logger.info("isHasGPU: " + isHasGPU);
//			Job job = prepareJob(traindir, output, VectorSequenceFileInputFormat.class,
//					KNNMapper.class, Text.class, Vector2SF.class,
			Job job = prepareJob(traindir, output, NewARFFInputformat.class,
					KNNMapper.class, Text.class, Vector2SF.class,
			KNNReducer.class, Text.class, SparseVector.class, ARFFOutputFormat.class, conf);
			job.setJarByClass(KNNDriver.class);
			job.setCombinerClass(KNNCombiner.class);
			Configuration conf2 = job.getConfiguration();
			conf2.set(ICTMapper.INITIAL_ARGS_CPU,"blockSize:"+blockSize+",dd_num:"+ddNum);
			conf2.set(ICTMapper.INITIAL_ARGS_GPU,"blockSize:"+blockSize+",dd_num:"+ddNum);
    		conf2.setInt("mapreduce.map.memory.mb", 4096);
			conf2.setLong(ICTMapper.EXPECT_GLOBAL_MEM_GPU, splitSize);
			conf2.setLong(ICTMapper.EXPECT_GLOBAL_MEM_CPU, splitSize);
			conf2.set(ICTMapper.PREF_RESOURCE, conf.get("cn.ac.ict.htc.knn.resource"));
			conf2.set(ICTMapper.CONTAINER_IMPL, "OMP");
			conf2.setInt(ICTMapper.EXPECT_OUTPUT_BUFFER_CAPACITY_GPU, 10*1024*1024);
			conf2.setInt(ICTMapper.EXPECT_OUTPUT_BUFFER_CAPACITY_CPU, 10*1024*1024);
			conf2.set(ICTMapper.LIBRARY_NAME_CPU, "libKNNOMP.so");
			conf2.set(ICTMapper.LIBRARY_NAME_GPU, "libKNNCuda.so");
			int res = job.waitForCompletion(true) ? 0 : 1;
			if (res != 0) {
				return res;
			}
		}
		return 0;
	}
}
