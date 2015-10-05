package cn.ac.ict.htc.tools;

public class Element {
	private int index;
	private double value;

	public Element(int index, double value) {
		this.index = index;
		this.value = value;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Element)) {
			return false;
		}
		Element e = (Element) obj;
		return this.index == e.getIndex() && this.value == e.getValue();
	}
	
	@Override
	public String toString() {
		return this.index+"="+this.value;
	}
}
