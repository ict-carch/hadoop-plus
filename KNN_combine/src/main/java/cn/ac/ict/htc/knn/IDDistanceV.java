package cn.ac.ict.htc.knn;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

import cn.ac.ict.htc.knn.data.Vector2;

public class IDDistanceV extends Vector2<String, Float> implements
		WritableComparable<IDDistanceV> {

	public IDDistanceV() {
		super();
	}

	public IDDistanceV(String v1, Float v2) {
		super(v1, v2);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		v1 = in.readUTF();
		v2 = in.readFloat();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(v1);
		out.writeFloat(v2);
	}

	@Override
	public int compareTo(IDDistanceV another) {
		if (v1.compareTo(another.v1) > 0)
			return 1;
		else if (v1 == another.v1) {
			return Float.compare(v2, another.v2);
		} else
			return -1;

	}

	public static void main(String[] args) {
		IDDistanceV v1 = new IDDistanceV("12", .2f), v2 = new IDDistanceV("1",
				0.2f);
		System.out.println(v1);
	}
}
