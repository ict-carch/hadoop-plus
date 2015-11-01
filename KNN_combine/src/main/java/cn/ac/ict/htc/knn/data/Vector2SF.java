package cn.ac.ict.htc.knn.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class Vector2SF extends Vector2<String,Double> implements Writable {
	public Vector2SF() {
	}

	public Vector2SF(String v1, Double v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		v1 = in.readUTF();
		v2 = in.readDouble();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(v1);
		out.writeDouble(v2);
	}
}
