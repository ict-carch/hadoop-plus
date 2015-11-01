package cn.ac.ict.htc.knn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import cn.ac.ict.htc.knn.data.SparseVector;
import cn.ac.ict.htc.knn.data.Vector2SF;

public class KNNReducer extends Reducer<Text, Vector2SF, Text, SparseVector> {

    protected void reduce(
            Text key,
            java.lang.Iterable<Vector2SF> value,
            org.apache.hadoop.mapreduce.Reducer<Text, Vector2SF, Text, SparseVector>.Context context)
            throws java.io.IOException, InterruptedException {
    	System.out.println("key: " + key + ", value: " + value);
        ArrayList<Vector2SF> vs = new ArrayList<Vector2SF>();
        // sort each vector2SF by similarty
        for (Vector2SF v : value) {
            vs.add(new Vector2SF(v.getV1(), v.getV2()));
        }
        Collections.sort(vs, new Comparator<Vector2SF>() {

            @Override
            public int compare(Vector2SF o1, Vector2SF o2) {
                return Double.compare(o2.getV2(), o1.getV2());
            }
        });
        int k = context.getConfiguration().getInt("cn.ac.ict.htc.knn.k", 4);
        SparseVector sp = new SparseVector();

        for (int i = 0; i < k && i < vs.size(); i++) {
            sp.put(vs.get(i).getV1(), vs.get(i).getV2());
        }
        context.write(key, sp);
    }

    ;
}
