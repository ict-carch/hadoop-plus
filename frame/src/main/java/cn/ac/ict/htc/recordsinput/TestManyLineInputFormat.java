package cn.ac.ict.htc.recordsinput;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.SplittableCompressionCodec;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import cn.ac.ict.htc.recordsinput.TestManyLineInputFormat;
import cn.ac.ict.htc.recordsinput.TestManyLineRecordReader;

import com.google.common.base.Charsets;

public class TestManyLineInputFormat extends
    FileInputFormat<LongWritable, ByteBuffer> {
    static public final int DEFAULT_RECORD_SIZE = 1024 * 1024;
    static public final long DEFAULT_GPU_CACHE_SIZE = (1l << 31) - 1;
    static public final int DEFAULT_K_SIZE = 10;
    static public final int DEFAULT_MAXINTER_SIZE = 10;
    static public final int DEFAULT_COORDS_SIZE = 20;
    
    static public final String CONF_KEY_RECORD_SIZE = "manylineinputformat.record.size";
    static public final String CONF_GPU_CACHE_SIZE = "mapreduce.input.fileinputformat.confgpucachesize";
    static public final String CONF_K_SIZE = "mapreduce.input.fileinputformat.confksize";
    static public final String CONF_MAXINTER_SIZE =	"mapreduce.input.fileinputformat.confmaxintersize";
    static public final String CONF_COORDS_SIZE = "mapreduce.input.fileinputformat.confcoordssize";
    
    private static final Log LOG = LogFactory.getLog(ManyLineInputFormat.class);
    @Override
    public RecordReader<LongWritable, ByteBuffer> createRecordReader(
    	InputSplit split, TaskAttemptContext context) throws IOException,
    	InterruptedException {
        String delimiter = context.getConfiguration().get(
        		"textinputformat.record.delimiter");
        LOG.info("delimiter = " + delimiter);
        byte[] recordDelimiterBytes = null;
        if (null != delimiter) {
        	recordDelimiterBytes = delimiter.getBytes(Charsets.UTF_8);
        }
                LOG.info("recordDelimiterBytes = " + recordDelimiterBytes);
        
        int recordSize = context.getConfiguration().getInt(
        		CONF_KEY_RECORD_SIZE, DEFAULT_RECORD_SIZE);
                LOG.info("recordSize = " + recordSize);
        
        return new TestManyLineRecordReader(recordDelimiterBytes, recordSize);
    }
    
    @Override
    protected boolean isSplitable(JobContext context, Path file) {
        final CompressionCodec codec = new CompressionCodecFactory(
        		context.getConfiguration()).getCodec(file);
        if (null == codec) {
        	return true;
        }
        return codec instanceof SplittableCompressionCodec;
    }
}
