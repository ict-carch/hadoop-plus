package cn.ac.ict.htc.knn.data;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.LineRecordReader;

import cn.ac.ict.htc.utils.PairOfByteBuffers;

import com.google.common.base.Charsets;

/**
 * ARFF file hadoop input file format
 * 
 * @author Song Liu (sl9885)
 */
public class ARFFManyLineInputformat extends FileInputFormat<Text, PairOfByteBuffers> {
	
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
        private static final Log LOG = LogFactory.getLog(ARFFManyLineInputformat.class);
   
    @Override
    protected boolean isSplitable(JobContext context, Path file) {
        CompressionCodec codec = new CompressionCodecFactory(context.getConfiguration()).getCodec(file);
        return codec == null;
    }

    @Override
    public RecordReader<Text, PairOfByteBuffers> createRecordReader(
            final InputSplit split, final TaskAttemptContext context) {
    	String delimiter = context.getConfiguration().get(
				"textinputformat.record.delimiter");
		byte[] recordDelimiterBytes = null;
		if (null != delimiter) {
			recordDelimiterBytes = delimiter.getBytes(Charsets.UTF_8);
		}
                LOG.info("recordDelimiterBytes = " + recordDelimiterBytes);

		int recordSize = context.getConfiguration().getInt(
				CONF_KEY_RECORD_SIZE, DEFAULT_RECORD_SIZE);
                LOG.info("recordSize = " + recordSize);
        return new ARFFManyLineRecordReader(recordDelimiterBytes, recordSize);
    }
}
