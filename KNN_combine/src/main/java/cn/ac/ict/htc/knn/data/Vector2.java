package cn.ac.ict.htc.knn.data;

public class Vector2<T1, T2> {
	protected T1 v1;
	protected T2 v2;

	public Vector2() {

	}

	public Vector2(T1 v1, T2 v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	public T1 getV1() {
		return v1;
	}

	public T2 getV2() {
		return v2;
	}

	@Override
	public String toString() {
		return v1 + "-" + v2;
	}
}
