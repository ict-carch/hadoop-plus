package cn.ac.ict.htc.tools;

import org.apache.hadoop.io.Writable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lubinb on 14-3-27.
 */
public class NumberListWritable<E extends Number> implements Writable {
    private List<E> innerList;
    private int size = 0;
    private Number test = null;

    //
    private final byte TYPE_BYTE = 0;
    private final byte TYPE_SHORT = 1;
    private final byte TYPE_INTEGER = 2;
    private final byte TYPE_LONG = 3;
    private final byte TYPE_FLOAT = 4;
    private final byte TYPE_DOUBLE = 5;
    //
    private byte TYPE_LOCAL = -1;



    public NumberListWritable(){
    }
    public NumberListWritable(List<E> list){
        set(list);
    }

    public void set(List<E> list){
        innerList = new ArrayList<E>(list.size());
        innerList.addAll(list);
        size = innerList.size();
        if(list.size()>0){
            test = list.get(0);
        }
    }

    public List<E> get(){
        return innerList;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        byte _type = in.readByte();
        int _size = in.readInt();
        innerList = new ArrayList<E>();
        if(_type == -1)
            throw new RuntimeException("read the element type is wrong!");
        for(int i = 0;i < _size;i++){
            innerList.add(readNext(in, _type));
        }
        TYPE_LOCAL = _type;
		size=innerList.size();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        byte type = getType();
        if(type == -1)
            throw new RuntimeException("The element type is wrong!");
        out.writeByte(type);
        out.writeInt(size);
        if(size == 0){
            return;
        }
        for(int i = 0;i < size;i++){
            writeNumber(out,innerList.get(i),type);
        }
    } 
    private E readNext(DataInput in,byte type) throws IOException{
        E number = null;
        switch(type){
            case TYPE_BYTE:
                number = (E)new Byte(in.readByte());
                break;
            case TYPE_SHORT:
                number = (E)new Short(in.readShort());
                break;
            case TYPE_INTEGER:
                number = (E)new Integer(in.readInt());
                break;
            case TYPE_FLOAT:
                number = (E)new Float(in.readFloat());
                break;
            case TYPE_DOUBLE:
                number = (E)new Double(in.readDouble());
                break;
            default:
                break;
        }
        return number;
    }

    private void writeNumber(DataOutput out,E number,byte type) throws IOException{
        switch(type){
            case TYPE_BYTE:
                out.writeByte(number.byteValue());
                break;
            case TYPE_SHORT:
                out.writeShort(number.shortValue());
                break;
            case TYPE_INTEGER:
                out.writeInt(number.intValue());
                break;
            case TYPE_FLOAT:
                out.writeFloat(number.floatValue());
                break;
            case TYPE_DOUBLE:
                out.writeDouble(number.doubleValue());
                break;
            default:
                break;
        }
    }

    private byte getType(){
        if(TYPE_LOCAL != -1){
            return TYPE_LOCAL;
        }else if(test instanceof Byte) {
            TYPE_LOCAL = TYPE_BYTE;
        }else if(test instanceof Float){
            TYPE_LOCAL = TYPE_FLOAT;
        }else if(test instanceof Integer) {
            TYPE_LOCAL = TYPE_INTEGER;
        }else if (test instanceof Double) {
            TYPE_LOCAL = TYPE_DOUBLE;
        }else if(test instanceof Long) {
            TYPE_LOCAL = TYPE_LONG;
        }else if(test instanceof Short) {
            TYPE_LOCAL = TYPE_SHORT;
        }
        return TYPE_LOCAL;
    }

    public static void main(String[] args) throws Exception{

        NumberListWritable<Double> tt = new NumberListWritable<Double>();
        ArrayList<Double> df = new ArrayList<Double>();
        for(int i = 0;i < 10;i++){
            df.add(new Double(i));
        }
        tt.set(df);
        DataInput di = new DataInputStream(new FileInputStream("C:\\data.dat"));
        DataOutput dot = new DataOutputStream(new FileOutputStream("C:\\data.dat"));

        tt.write(dot);

        NumberListWritable<Double> tt_o = new NumberListWritable<Double>();
        tt_o.readFields(di);
        System.out.println(tt_o.get().size());
    }
}
