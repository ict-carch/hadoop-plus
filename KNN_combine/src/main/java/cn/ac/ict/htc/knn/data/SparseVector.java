package cn.ac.ict.htc.knn.data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import org.apache.hadoop.io.Writable;

public class SparseVector extends HashMap<String, Double> implements Writable {

    public static final String HEAD = "HEAD_RESERVED";
    public static final String ID = "~0";
    
    public double average() {
        double ave = 0;
        for (Double f : values()) {
            ave += f;
        }
        return ave / size();
    }

    public double dotProduct(SparseVector another) {
        double res = 0;
        for (String k : this.keySet()) {
            if (another.containsKey(k)) {
                res += get(k) * another.get(k);
            }
        }
        return res;
    }

    public double euclideanDistance(SparseVector another) {
        double res = 0;
        SparseVector lh = this;
        SparseVector rh = another;
        if (size() < another.size()) {
            lh = another;
            rh = this;
        }
        for (String k : lh.keySet()) {
            if (rh.containsKey(k)) {
                res = Math.sqrt(Math.pow(res, 2)
                        + Math.pow(lh.get(k) - rh.get(k), 2));
            } else {
                //   res = Math.sqrt(lh.get(k) * lh.get(k) + Math.pow(res, 2));
            }
        }
        return res;
    }

    public double diceDistance(SparseVector another) {
        int res = 0;
        for (String k : this.keySet()) {
            if (another.containsKey(k)) {
                res++;
            }
        }
        return 1 / (1 + res);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        clear();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            String id = in.readUTF();
            Double f = in.readDouble();
            put(id, f);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.size());
        for (String s : this.keySet()) {
            out.writeUTF(s);
            out.writeDouble(get(s));
        }
    }

    public static void main(String[] args) {
//        SparseVector sv1 = new SparseVector(), sv2 = new SparseVector();
//        sv1.put("1", 3.0f);
//        sv1.put("2", 4.0f);
//        //sv1.put("3", 0.0f);
//
//        sv2.put("1", 0.0f);
//        sv2.put("2", 0.0f);
//        sv2.put("3", 4.0f);
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < 1000000; i++) {
//            sv1.euclideanDistance(sv2);
//        }
//        System.out.println(System.currentTimeMillis() - start);
//        System.out.println(sv1.euclideanDistance(sv2));
    }
}
