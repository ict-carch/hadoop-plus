package cn.ac.ict.htc.recordsinput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import cn.ac.ict.htc.recordsinput.LineRecordReader;
import cn.ac.ict.htc.utils.PairOfByteBuffers;

public class ManyLineRecordReader extends
		RecordReader<LongWritable, PairOfByteBuffers> {
	static private final Log LOG = LogFactory.getLog(ManyLineRecordReader.class);
	
	static private final byte[] LINE_SEPERATOR = System.getProperty("line.separator").getBytes();

	private final LineRecordReader lineRecordReader;
	private boolean readerClosed = false;
	private final LongWritable key = new LongWritable(); 
	private final PairOfByteBuffers pair;
	private int maxLineSize;

	public ManyLineRecordReader(byte[] recordDelimiterBytes, int recordSizeLimit) {
	    this.lineRecordReader = new LineRecordReader(recordDelimiterBytes);
	    pair = new PairOfByteBuffers(recordSizeLimit*4, recordSizeLimit);
	}

	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		int halfOfBufferSize = pair.value.capacity() / 2;
		maxLineSize = context.getConfiguration().getInt(LineRecordReader.MAX_LINE_LENGTH, halfOfBufferSize);

		//y62663: 
		if (maxLineSize > halfOfBufferSize)
		{
			context.getConfiguration().setInt(LineRecordReader.MAX_LINE_LENGTH, halfOfBufferSize);
			maxLineSize = halfOfBufferSize;
		}

		lineRecordReader.initialize(split, context);
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		boolean retVal = false;
		pair.offset.clear();
		pair.value.clear();
		//y62663: 必须先检查缓冲区，再读行数据。
		while (isBufferNotFull() && moreToRead()) {
				if (pair.value.position() == 0) {
					key.set(lineRecordReader.getCurrentKey().get());
					LOG.debug("y62663. key: " + key);
				}

				byte[] bytes = lineRecordReader.getCurrentValue().copyBytes();
//				LOG.info("read " + bytes.length + " bytes.");
				retVal = true;
				pair.offset.putInt(pair.value.position());
				pair.value.put(bytes);
				pair.value.put(LINE_SEPERATOR);
				Arrays.fill(bytes, (byte)0);
		}
		
		if (retVal) {
			pair.offset.flip();
			pair.value.flip();
            LOG.info("value.position = " + pair.value.position() + "value.limit = " + pair.value.limit());
            LOG.info("offset.position = " + pair.offset.position() + "offset.limit = " + pair.offset.limit());
			LOG.debug("y62663. value:" + pair.value);
		}
		
		return retVal;
	}

	private boolean isBufferNotFull() {
		return pair.value.position() + getMaxLineSize() < pair.value.capacity();
	}
	
    private boolean moreToRead() throws IOException {
        if (!readerClosed)
        {   
            if(lineRecordReader.nextKeyValue())
            {   
                return true;
            }   
            else
            {   
//                LOG.info("y62663: reader closed now.");
                readerClosed = true;
                return false;
            }   
        }   
        else
        {   
//            LOG.info("y62663: reader closed already.");
            return false;
        }   
    }

	@Override
	public LongWritable getCurrentKey() throws IOException,
			InterruptedException {
		return key;
	}

	@Override
	public PairOfByteBuffers getCurrentValue() throws IOException,
			InterruptedException {
//		PairOfByteBuffers info = new PairOfByteBuffers(offset, value);
		return pair;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return lineRecordReader.getProgress();
	}

	@Override
	public void close() throws IOException {
		lineRecordReader.close();
	}

	private int getMaxLineSize() {
		return maxLineSize;
	}
}

