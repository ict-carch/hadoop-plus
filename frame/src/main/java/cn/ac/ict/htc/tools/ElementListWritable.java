package cn.ac.ict.htc.tools;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.Writable;

public class ElementListWritable implements Writable{

	private ElementList elements;
	private Iterator<Element> iter_el;
	private int num = 0;
	
	public ElementListWritable(){
		
	}
	
	public ElementListWritable(ElementList elements){
		this.elements = elements;
	}
	
	@Override
	public void write(DataOutput out) throws IOException {
		iter_el = elements.getElement();
		num  =elements.num();
		out.writeInt(num);
		Element e = null;
		for(int i = 0; i< num;i++){
			e = iter_el.next();
			out.writeInt(e.getIndex());
			out.writeDouble(e.getValue());
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		ElementList el = new ElementList();
		num = in.readInt();
		Element element = null;
		for(int i = 0;i< num;i++){
			element = new Element(in.readInt(), in.readDouble());
			el.addElement(element);
		}
		elements = el;
	}
	
	public ElementList get(){
		return elements;
	}
	
	public void setElements(ElementList elements){
		this.elements = elements;
	}
}
