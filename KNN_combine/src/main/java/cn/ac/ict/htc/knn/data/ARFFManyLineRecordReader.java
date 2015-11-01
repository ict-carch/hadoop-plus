package cn.ac.ict.htc.knn.data;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import org.apache.hadoop.mapreduce.InputSplit;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.LineRecordReader;

import cn.ac.ict.htc.utils.PairOfByteBuffers;

public class ARFFManyLineRecordReader extends
		RecordReader<Text, PairOfByteBuffers> {
	private static final Log LOG = LogFactory
			.getLog(ARFFManyLineRecordReader.class);
	private final Text key = new Text(SparseVector.ID);
	// private SparseVector value;
	private final LineRecordReader r;
	private long start;
	private final PairOfByteBuffers pair;
	private boolean readerClosed = false;
	private int maxLineSize;

	private ArrayList<SparseVector> list_value = new ArrayList<SparseVector>();

	private final static Pattern p = Pattern.compile("([^,]+,)|([^,]+})");

	public ARFFManyLineRecordReader(byte[] recordDelimiterBytes,
			int recordSizeLimit) {
		this.r = new LineRecordReader(recordDelimiterBytes);
		pair = new PairOfByteBuffers();
	}

	@Override
	public void close() throws IOException {
		r.close();
	}

	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		return key;
	}

	@Override
	public PairOfByteBuffers getCurrentValue() throws IOException,
			InterruptedException {
		return pair;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return r.getProgress();
	}

	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		int halfOfBufferSize = pair.value.capacity() / 2;
		maxLineSize = context.getConfiguration().getInt(
				LineRecordReader.MAX_LINE_LENGTH, halfOfBufferSize);
		if (maxLineSize > halfOfBufferSize) {
			context.getConfiguration().setInt(LineRecordReader.MAX_LINE_LENGTH,
					halfOfBufferSize);
			maxLineSize = halfOfBufferSize;
		}
		r.initialize(split, context);
		FileSplit fs = (FileSplit) split;
		start = fs.getStart();
	}

	private int getMaxLineSize() {
		return maxLineSize;
	}

	public boolean isBufferNotFull() {
		return pair.value.position() + getMaxLineSize() < pair.value.capacity();
	}

	private boolean moreToRead() throws IOException {
		if (!readerClosed) {
			if (r.nextKeyValue()) {
				return true;
			} else {
				readerClosed = true;
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		boolean retval = false;
		pair.offset.clear();
		pair.value.clear();
		while (isBufferNotFull() && moreToRead()) {
			Text line = r.getCurrentValue();
			// if (pair.value.position() == 0) {
			//
			// key.set(readLine(start, line.toString(), pair));
			//
			// }
			// readLine(start, line.toString(), pair);
			readLine(start, line.toString(), pair);
			start += line.getLength();
			retval = true;

		}
		if (retval) {
			pair.offset.flip();
			pair.value.flip();
			LOG.debug("value.position = " + pair.value.position()
					+ "value.limit = " + pair.value.limit());
			LOG.debug("offset.position = " + pair.offset.position()
					+ "offset.limit = " + pair.offset.limit());
		}
		return retval;

	}

	// @Override
	// public boolean nextKeyValue() throws IOException, InterruptedException {
	// if (r.nextKeyValue()) {
	// Text line = r.getCurrentValue();
	// Vector2<String, SparseVector> v = readLine(start, line.toString());
	// key = new Text(v.getV1());
	// value = v.getV2();
	// start += line.getLength();
	// return true;
	// }
	// return false;
	// }
	// return key
	// public static string readLine(long start, String line,PairOfByteBuffers
	// pair) {
	// public static Vector2<String, SparseVector> readLine(long start, String
	// line) {
	// if (line.startsWith("{")) {
	// // split the line into key and value
	// // remove the round blanket
	// Matcher m = p.matcher(line.toString());
	// // load ID string
	// m.find();
	// String s = m.group();
	// s = s.substring(0, s.length() - 1);
	// // read the key
	// String key = null;
	// if (s.split(" ")[0].equals(SparseVector.ID)) {
	// // if this file ignores the ID, we just use the file offset
	// // instead
	// key = s.split(" ")[1];
	// } else {
	// key = start + "";
	// }
	// // read value
	// SparseVector value = new SparseVector();
	// while (m.find()) {
	// s = m.group();
	// s = s.substring(0, s.length() - 1);
	// String c = s.split(" ")[0];
	// float v = Float.parseFloat(s.split(" ")[1]);
	// value.put(c, v);
	// }
	// return new Vector2<String, SparseVector>(key, value);
	// } else {
	// // offset as ID
	// String key = start + "";
	// // read value
	// SparseVector value = new SparseVector();
	// int i = 1;
	// // pair.offset.putInt(pair.value.position());
	// for (String s : line.split(",")) {
	// // System.out.println("i: " + i + " s: " + s);
	// value.put(i + "", Float.parseFloat(s));
	// // if(Float.parseFloat(s)==0f) continue;
	// // pair.value.putFloat(Float.parseFloat());
	// i++;
	// }
	// return new Vector2<String, SparseVector>(key, value);
	// // return return key;
	// }
	// }
	//
	public static int readLine(long start, String line, PairOfByteBuffers pair) {
		// public static Vector2<String, SparseVector> readLine(long start,
		// String line) {
		// if (line.startsWith("{")) {
		// // split the line into key and value
		// // remove the round blanket
		// Matcher m = p.matcher(line.toString());
		// // load ID string
		// m.find();
		// String s = m.group();
		// s = s.substring(0, s.length() - 1);
		// // read the key
		// String key = null;
		// if (s.split(" ")[0].equals(SparseVector.ID)) {
		// // if this file ignores the ID, we just use the file offset
		// // instead
		// key = s.split(" ")[1];
		// } else {
		// key = start + "";
		// }
		// // read value
		// SparseVector value = new SparseVector();
		// while (m.find()) {
		// s = m.group();
		// s = s.substring(0, s.length() - 1);
		// String c = s.split(" ")[0];
		// float v = Float.parseFloat(s.split(" ")[1]);
		// value.put(c, v);
		// }
		// return new Vector2<String, SparseVector>(key, value);
		// } else {
		// offset as ID
		// String key = start + "";
		// read value
		// SparseVector value = new SparseVector();
		int i = 0;
		pair.offset.putInt(pair.value.position());
		pair.value.putLong(start);
		//System.out.print("case" + start + ": ");
		pair.offset.putInt(pair.value.position());
		for (String s : line.split(",")) {
			// System.out.println("i: " + i + " s: " + s);
			// value.put(i + "", Float.parseFloat(s));
			// if(Float.parseFloat(s)==0f) continue;
			pair.value.putDouble(Double.parseDouble(s));
			//System.out.print(" " + Double.parseDouble(s));
			i++;
		}
		//System.out.println();
		// return new Vector2<String, SparseVector>(key, value);
		return i;
		// }
	}

}
