package cn.ac.ict.htc.knn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import cn.ac.ict.htc.knn.data.Vector2SF;

public class KNNCombiner extends Reducer<Text, Vector2SF, Text, Vector2SF> {
	
	protected void reduce(
			Text key,
			java.lang.Iterable<Vector2SF> value,
			org.apache.hadoop.mapreduce.Reducer<Text, Vector2SF, Text, Vector2SF>.Context context)
			throws java.io.IOException, InterruptedException {
		ArrayList<Vector2SF> vs = new ArrayList<Vector2SF>();
		// sort each vector2SF by similarty
//		System.out.println("combining key: " + key + " value: " );
		for (Vector2SF v : value) {
// 			System.out.println(v.getV1() + ", " + v.getV2());
			vs.add(new Vector2SF(v.getV1(), v.getV2()));
		}
		Collections.sort(vs, new Comparator<Vector2SF>() {
			@Override
			public int compare(Vector2SF o1, Vector2SF o2) {
				return Double.compare(o2.getV2(), o1.getV2());
			}
		});
//		System.out.println("vs after sorting: " + vs);
		int k = context.getConfiguration().getInt("cn.ac.ict.htc.knn.k", 4);

		for (int i = 0; i < k && i < vs.size(); i++) {
 //			System.out.println("key: " + key + " vs[" + i + "]: " + vs.get(i));
			context.write(key, vs.get(i));
		}
	};
}
