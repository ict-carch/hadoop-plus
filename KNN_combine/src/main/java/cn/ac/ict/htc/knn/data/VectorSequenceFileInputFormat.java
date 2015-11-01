package cn.ac.ict.htc.knn.data;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;

import cn.ac.ict.htc.utils.PairOfByteBuffers;

public class VectorSequenceFileInputFormat extends
		SequenceFileInputFormat<LongWritable, PairOfByteBuffers> {
	public RecordReader<LongWritable, PairOfByteBuffers> createRecordReader(
			InputSplit split, TaskAttemptContext context) throws IOException {
		return new VectorSequenceFileRecordReader();
	}

}
