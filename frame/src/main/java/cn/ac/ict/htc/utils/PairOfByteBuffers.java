package cn.ac.ict.htc.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.Writable;

public class PairOfByteBuffers implements Writable {
	static private final Log LOG = LogFactory.getLog(PairOfByteBuffers.class);
	public final ByteBuffer value;
	public final ByteBuffer offset;
	private int default_size = 1024 * 1024 * 2; // 2M

	public PairOfByteBuffers() {
		offset = BufferUtils.allocateNativeOrderDirectBuffer(default_size);
		value = BufferUtils.allocateNativeOrderDirectBuffer(default_size);
	}

	public PairOfByteBuffers(int offset_size, int value_size) {
		offset = BufferUtils.allocateNativeOrderDirectBuffer(offset_size);
		value = BufferUtils.allocateNativeOrderDirectBuffer(value_size);
	}

	public byte[] getbulk() {
		if (this.offset.position() < this.offset.limit()) {
			int oriPos = this.offset.getInt();
			int secPos;
			if (this.offset.position() < this.offset.limit())
				secPos = this.offset.getInt();
			else
				secPos = this.value.limit();
			this.offset.position(this.offset.position() - 4);
			byte[] str = new byte[secPos - oriPos];
			this.value.get(str, 0, secPos - oriPos);
			return str;
		} else
			return null;
	}

	public PairOfByteBuffers compact() {
		LOG.info("value position: " + this.value.position() + "value limit: "
				+ this.value.limit());
		this.value.compact();
		LOG.info("offset position: " + this.offset.position()
				+ "offset limit: " + this.offset.limit());
		int rem = this.offset.limit() - this.offset.position();
		int pos0 = this.offset.getInt(0);
		this.offset.compact();
		while (rem > 0) {

			rem -= 4;
			this.offset.putInt(rem, this.offset.getInt(rem) - pos0);
		}
		return this;
	}

	public void flip() {
		this.offset.flip();
		this.value.flip();
	}

	public void clear() {
		this.offset.clear();
		this.value.clear();
	}

	public void write(DataOutput out) throws IOException {
		// TODO Auto-generated method stub
//		this.flip();
		this.offset.mark();
		this.value.mark();
		out.writeInt(offset.limit() / 4);
		while (offset.position() != offset.limit()) {
			out.writeInt(offset.getInt());
		}

		out.writeInt(value.limit());
		byte[] temp = new byte[value.limit()];
		value.get(temp);
		out.write(temp);
		
		this.offset.reset();
		this.value.reset();
	}

	public void readFields(DataInput in) throws IOException {
		// TODO Auto-generated method stub
		this.clear();
		int offsetNum = in.readInt();
		for (int i = 0; i < offsetNum; i++) {

			offset.putInt(in.readInt());
			// int o = in.readInt();
			// System.out.println(o);
			// offset.putInt(o);
		}

		int valueLen = in.readInt();
		// System.out.println("read:" + valueLen);
		byte[] temp = new byte[valueLen];
		in.readFully(temp);
		value.put(temp);
		//offset.flip();
		//value.flip();
		this.flip();
	}

}
