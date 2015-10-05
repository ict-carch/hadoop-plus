package cn.ac.ict.htc.job;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public abstract class AbstractJob extends Configured implements Tool {
	private static final Log logger = LogFactory.getLog(AbstractJob.class);
	private Options options;
	private CommandLine commandLine;

	@SuppressWarnings({ "deprecation", "rawtypes" })
	protected Job prepareJob(Path inputPath, Path outputPath,
			Class<? extends InputFormat> inputFormat,
			Class<? extends Mapper> mapper,
			Class<? extends Writable> mapperKey, Class<?> mapperValue,
			Class<? extends OutputFormat> outputFormat, Configuration conf)
			throws IOException {
		Job job = new Job(new Configuration(conf));
		Configuration jobConf = job.getConfiguration();

		job.setJarByClass(mapper);

		job.setInputFormatClass(inputFormat);
		jobConf.set("mapred.input.dir", inputPath.toString());

		job.setMapperClass(mapper);
		job.setMapOutputKeyClass(mapperKey);
		job.setMapOutputValueClass(mapperValue);
		job.setOutputKeyClass(mapperKey);
		job.setOutputValueClass(mapperValue);
		jobConf.setBoolean("mapred.compress.map.output", true);
		job.setNumReduceTasks(0);
		job.setOutputFormatClass(outputFormat);
		jobConf.set("mapred.output.dir", outputPath.toString());

		return job;
	}

	@SuppressWarnings({ "deprecation", "rawtypes" })
	public static Job prepareJob(Path inputPath, Path outputPath,
			Class<? extends InputFormat> inputFormat,
			Class<? extends Mapper> mapper,
			Class<? extends Writable> mapperKey, Class<?> mapperValue,
			Class<? extends Reducer> reducer,
			Class<? extends Writable> reducerKey,
			Class<? extends Writable> reducerValue,
			Class<? extends OutputFormat> outputFormat, Configuration conf)
			throws IOException {

		Job job = new Job(new Configuration(conf));
		Configuration jobConf = job.getConfiguration();

		if (reducer.equals(Reducer.class)) {
			if (mapper.equals(Mapper.class)) {
				throw new IllegalStateException(
						"Can't figure out the user class jar file from mapper/reducer");
			}
			job.setJarByClass(mapper);
		} else {
			job.setJarByClass(reducer);
		}

		job.setInputFormatClass(inputFormat);
		job.setMapperClass(mapper);
		if (mapperKey != null) {
			job.setMapOutputKeyClass(mapperKey);
		}
		if (mapperValue != null) {
			job.setMapOutputValueClass(mapperValue);
		}

		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		job.setReducerClass(reducer);
		job.setOutputKeyClass(reducerKey);
		job.setOutputValueClass(reducerValue);

		job.setOutputFormatClass(outputFormat);
		job.setJobName("firstJob");

		return job;
	}

	protected boolean shouldRunNextPhase(AtomicInteger currentPhase) {
		int phase = currentPhase.getAndIncrement();
		logger.info(">>>>>>>>>>>>>job phase : " + phase);
		int startPhase = 0;
		int endPhase = Integer.MAX_VALUE;
		boolean phaseSkipped = phase < startPhase || phase > endPhase;
		return !phaseSkipped;
	}

	public Map<String, String> parseArguments(String[] args) throws ParseException {
		Map<String,String> argsMap = new HashMap<String, String>();
		parseArgs(args);
		for(Option opt :commandLine.getOptions()){
			argsMap.put(opt.getArgName(), commandLine.getOptionValue(opt.getArgName()));
		}
		return argsMap;
	}
	
	public void parseArgs(String[] args) throws ParseException{
		commandLine =  new BasicParser().parse(options, args);
	}
	
	public void addOption(Option option){
		if(options == null){
			options = new Options();
		}
		options.addOption(option);
	}
	
	/**
	 * 
	 * create option 
	 * 
	 * @param shortName
	 * @param name
	 * @param description
	 * @param hasArgs  
	 * @param required
	 * @return
	 */
	@SuppressWarnings("static-access")
	public Option buildOption(String shortName,String name, String description, boolean hasArgs,boolean required){
		return OptionBuilder.withLongOpt(name).withArgName(name).isRequired(required).withDescription(description).hasArg(hasArgs).create(shortName);

	}
	
	public boolean hasOption(String optionName){
		return commandLine.hasOption(optionName);
	}
	
	
	public String getOption(String optionName){
		return commandLine.getOptionValue(optionName);
	}
	
	public String getOption(String optionName, String defaultValue){
		String value = commandLine.getOptionValue(optionName);
		if(value == null){
			value = defaultValue;
		}
		return value;
	}
	
	/**
	 * delete temp paths
	 * @param conf
	 * @param paths
	 * @throws IOException
	 */
	public void setTempPathToClean(Configuration conf,Path ...paths) throws IOException{
		if(paths.length==0)
			return;
		FileSystem fs = paths[0].getFileSystem(conf);
		for(Path path:paths){
			fs.delete(path, true);
		}
	}
}
