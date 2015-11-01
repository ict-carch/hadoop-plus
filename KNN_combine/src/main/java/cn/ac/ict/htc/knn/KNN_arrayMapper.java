package cn.ac.ict.htc.knn;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.Iterator;
import java.io.File;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.LongWritable;

import org.apache.hadoop.mapreduce.Mapper;
import cn.ac.ict.htc.knn.data.*;
import cn.ac.ict.htc.tools.NumberListWritable;

public class KNN_arrayMapper extends Mapper<Text, NumberListWritable<Double>, Text, Vector2SF> {

    double[][] tests;
    double[] trainVector;
    String[] testKeys;
//                Heapsort<Vector2SF> sorter = new Heapsort<Vector2SF>();
		int kn, k, test_num, coords;
		double[][] results;
		String[][] trainKeys;

    protected void map(
            Text key,
            NumberListWritable<Double> value,
            org.apache.hadoop.mapreduce.Mapper<Text, NumberListWritable<Double>, Text, Vector2SF>.Context context)
            throws java.io.IOException, InterruptedException {
        context.setStatus (key.toString());
	int test_id = 0, i = 0;
                Iterator<Double> iter = value.get().iterator();
                while (iter.hasNext()) {
                    Double e = iter.next();
		    trainVector[i] = e.doubleValue();
//		    System.err.println("trainId: " + k + ", dim: " + i + ":: " + trainVector[i]);
		    i++;
                }
	  
        for (test_id = 0; test_id < test_num; test_id++) {
            double d = 0.0;
	    for (int c = 0; c < coords; c++)
	    {
		d += trainVector[c] * tests[test_id][c];
////		System.err.println("testId: " + test_id + ", trainId: " + k + ", dim: " + c + ":: " + tests[test_id][c] + ", " + trainVector[c]);
	    }
		if(k < kn)
		{
			results[test_id][k] = d;
		    System.err.println("trainId: " + key.toString());
			trainKeys[test_id][k] = new String(key.toString());
		}
		else
		{
		    double min = results[test_id][0];
			int ind = 0;
			for (i = 1; i < kn; i++)
			{
			    if(results[test_id][i] < min)
				{
					min = results[test_id][i];
					ind = i;
				}
				if(d > min)
				{
					results[test_id][ind] = d;
					trainKeys[test_id][ind] = key.toString();
				}
			}
		}
//	    Double res = new Double(d);
//	    if(k < kn){
//		vss[test_id][k] = new Vector2SF(key.toString(), res);
//		if(k==(kn-1)){
// 		    sorter.buildMaxHeap(vss[test_id], kn-1);
//		}
//	    }
//	    else if(res.compareTo(vss[test_id][0].getV2()) > 0){
// 		vss[test_id][0] = new Vector2SF(key.toString(), res);
//		sorter.maxHeapify(vss[test_id], 0, kn-1);
// 	    }
       }
		k++;

    }
    
    ;
    protected void cleanup(
            org.apache.hadoop.mapreduce.Mapper<Text, NumberListWritable<Double>, Text, Vector2SF>.Context context)
             throws java.io.IOException, InterruptedException {
//        for (int j = 0; j < test_num; j++) {
//	    for(int i = 0; i < kn; i++){
////		System.out.println("test_id: " + j + ", testKey: " + testKeys[j] + ", distance: " + vss[j][i]);
//                context.write( new Text(testKeys[j]), vss[j][i]);
//	    }
//        }
    } 

    ;

    protected void setup(
            org.apache.hadoop.mapreduce.Mapper<Text, NumberListWritable<Double>, Text, Vector2SF>.Context context)
            throws java.io.IOException, InterruptedException {
        System.out.print("loading shared comparison vectors...");
		long pid = Long.parseLong(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	        File path = new File("/home/hewt/pids");
	        File file = new File(path + "/" + pid);
	        path.mkdirs();
	        file.createNewFile();

        // load the test
	test_num = 0;
	kn = context.getConfiguration().getInt("cn.ac.ict.htc.knn.k", 4);
        FileSystem fs = FileSystem.get(context.getConfiguration());
        SequenceFile.Reader reader = new SequenceFile.Reader(FileSystem.get(context.getConfiguration()),
                new Path(context.getConfiguration().get("cn.ac.ict.htc.knn.test", "")), context.getConfiguration());
        NumberListWritable<Double> testValue;
        LongWritable testKey;
        tests = new double[36864][128];
        testKeys = new String[36864];

        try {
            testKey = (LongWritable)reader.getKeyClass().newInstance();
            testValue = (NumberListWritable)reader.getValueClass().newInstance();
            while (reader.next(testKey, testValue)) {
                Iterator<Double> iter = testValue.get().iterator();
               coords = 0;
                while (iter.hasNext()) {
                    Double e = iter.next();
		    tests[test_num][coords++] = e.doubleValue();
                }
		testKeys[test_num] = testKey.get() + "";
                test_num++;
//		System.out.println("test " + test_num + " key: " + testKey + ", " + testValue);
            }

        } catch (Exception e){
        }
        reader.close();
	results = new double[test_num][kn]; 
	trainKeys = new String[test_num][kn]; 
	trainVector = new double[coords];
        System.out.println("done.");
    }

    ;
}
