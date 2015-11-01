package cn.ac.ict.htc.knn.data;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileRecordReader;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;


import cn.ac.ict.htc.tools.NumberListWritable;

public class NewARFFInputformat extends  FileInputFormat<Text, NumberListWritable<Double>> {
    public RecordReader<Text, NumberListWritable<Double>> createRecordReader(
            InputSplit split, TaskAttemptContext context) throws IOException {
        return new RecordReader<Text, NumberListWritable<Double>>(){
            private Text key;
            private NumberListWritable<Double> value;
            private SequenceFileRecordReader<LongWritable, NumberListWritable<Double>> recordReader = new SequenceFileRecordReader<LongWritable, NumberListWritable<Double>>();;
            private long start;
             @Override
            public void close() throws IOException {
                recordReader.close();
            }

            @Override
            public Text getCurrentKey() throws IOException, InterruptedException {
                return key;
            }

            @Override
            public NumberListWritable<Double> getCurrentValue() throws IOException,
                    InterruptedException {
                return value;
            }

            @Override
            public float getProgress() throws IOException, InterruptedException {
                return recordReader.getProgress();
            }

            @Override
            public void initialize(InputSplit split, TaskAttemptContext context)
                    throws IOException, InterruptedException {
                recordReader.initialize(split, context);
            }

            @Override
            public boolean nextKeyValue() throws IOException, InterruptedException {
//                List<Double> v;
                if (recordReader.nextKeyValue()) {
                    key = new Text(recordReader.getCurrentKey().get()+"");//
                    value = recordReader.getCurrentValue();
//                    for(int i = 0;i<v.size();i++){
//                        val.put((i+1) + "",v.get(i));
//                    }
//                    value = val;
                    return true;
                }
                return false;
            }
        };
    }

}
