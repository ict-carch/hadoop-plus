package cn.ac.ict.htc.tools;

public final class Functions {
	public static final DoubleDoubleFunction PLUS = new DoubleDoubleFunction() {

		@Override
		public double apply(double a, double b) {
			return a + b;
		}
	};
}