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
import cn.ac.ict.htc.recordsinput.TestManyLineRecordReader;

public class TestManyLineRecordReader extends
    RecordReader<LongWritable, ByteBuffer> {
    static private final Log LOG = LogFactory.getLog(TestManyLineRecordReader.class);
    
    static private final byte[] LINE_SEPERATOR = System.getProperty("line.separator").getBytes();
    
    private final LineRecordReader lineRecordReader;
    private boolean readerClosed = false;
    private final LongWritable key = new LongWritable(); 
    private final ByteBuffer buffer;
    private int maxLineSize;
    
    public TestManyLineRecordReader(byte[] recordDelimiterBytes, int recordSizeLimit) {
        this.lineRecordReader = new LineRecordReader(recordDelimiterBytes);
        buffer = ByteBuffer.allocateDirect(recordSizeLimit);
    }
    
    @Override
    public void initialize(InputSplit split, TaskAttemptContext context)
    	throws IOException, InterruptedException {
        int halfOfBufferSize = buffer.capacity() / 2;
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
        buffer.clear();
        //y62663: 必须先检查缓冲区，再读行数据。
        while (isBufferNotFull() && moreToRead()) {
        		if (buffer.position() == 0) {
        			key.set(lineRecordReader.getCurrentKey().get());
        			LOG.debug("y62663. key: " + key);
        		}
        
        		byte[] bytes = lineRecordReader.getCurrentValue().copyBytes();
        		LOG.info("read " + bytes.length + "bytes");
        		retVal = true;
        		buffer.put(bytes);
        		buffer.put(LINE_SEPERATOR);
        		Arrays.fill(bytes, (byte)0);
        }
        
        if (retVal) {
        	buffer.flip();
        	LOG.debug("y62663. value:" + buffer);
        }
        
        return retVal;
    }
    
    private boolean isBufferNotFull() {
        return buffer.position() + getMaxLineSize() < buffer.capacity();
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
                LOG.info("y62663: reader closed now.");
                readerClosed = true;
                return false;
            }   
        }   
        else
        {   
            LOG.info("y62663: reader closed already.");
            return false;
        }   
    }
    
    @Override
    public LongWritable getCurrentKey() throws IOException,
    	InterruptedException {
        return key;
    }
    
    @Override
    public ByteBuffer getCurrentValue() throws IOException,
    	InterruptedException {
        return buffer;
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
