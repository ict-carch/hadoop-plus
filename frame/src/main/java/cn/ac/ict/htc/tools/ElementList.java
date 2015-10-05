package cn.ac.ict.htc.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

public class ElementList {
	private List<Integer> index = new ArrayList<Integer>();//will delete while norm toString()
	private List<Double> value = new ArrayList<Double>();//up
	private Map<Integer,Integer> index_hash = new HashMap<Integer, Integer>();

	private List<Element> elements = new ArrayList<Element>();
	public void addElement(Element ele) {
		if(ele == null)
			throw new IllegalArgumentException("the element is null");
		if(index_hash.containsKey(ele.getIndex())){
			return;
		}else if (ele != null) {
			elements.add(ele);
			index_hash.put(ele.getIndex(),elements.size()-1);
		}
	}

	public void addElements(ElementList elist) {
		for (Iterator<Element> iter = elist.getElement(); iter.hasNext();) {
			addElement(iter.next());
		}
	}
	
	/**
	 * reset the index_hash ,after sort all elements
	 */
	private void trim() {
		index_hash.clear();
		for(int i = 0;i<elements.size();i++){
			index_hash.put(elements.get(i).getIndex(), i);
		}
	}

	/*
	 * return the iterator of element(index & value)
	 */
	public Iterator<Element> getElement() {
		return elements.iterator();
	}

	public List<Element> getElementList(){
		return elements;
	}

	public int num() {
		return elements.size();
	}

	/**
	 * clear all elements
	 */
	public void clear() {
		index_hash.clear();
		elements.clear();
	}
	/**
	 * sort the elements by index
	 */
	public void sortByIndex() {
		Collections.sort(elements, BY_INDEX);
		trim();
	}
	
	/**
	 * sort the elements by value
	 */
	public void sortByValue(){
		Collections.sort(elements,BY_VALUE);
		trim();
	}

	/**
	 * only for toStringByGroup ,index group and then value group
	 */
	private void fillIndexAndValueList(){
		Iterator<Element> elements = getElement();
		while(elements.hasNext()){
			Element e = elements.next();
			index.add(e.getIndex());
			value.add(e.getValue());
		}
		
	}
	
	@Override
	public String toString() {
		return elements.toString();
		
	}
	
	public String toStringByGroup(){
		if(index.size()==0){
			fillIndexAndValueList();
		}
		return "index:" + index.toString() + "\t values :" + value.toString();
	}

	/**
	 * compare by index(element index)
	 */
	public static final Comparator<Element> BY_INDEX = new Comparator<Element>() {
		@Override
		public int compare(Element one, Element two) {
			return Ints.compare(one.getIndex(), two.getIndex());
		}
	};

	/**
	 * compare by value(element value)
	 */
	public static final Comparator<Element> BY_VALUE= new Comparator<Element>() {
		@Override
		public int compare(Element one, Element two) {
			return Doubles.compare(one.getValue(), two.getValue());
		}
	};
	
	public void assign(ElementList other, DoubleDoubleFunction plus) {
		Iterator<Element> it_other = other.elements.iterator();
		while(it_other.hasNext()){
			Element e = it_other.next();
			int pos = getQuick(e);
			if(pos<0){
				addElement(e);
			}else{
				Element ele = elements.get(pos);
				ele.setValue((double)plus.apply(ele.getValue(), e.getValue()));
			}
		}
	}

	/**
	 * return the index where the element in this list
	 * @param ele
	 * @return
	 */
	private int getQuick(Element e) {
		Integer ix = index_hash.get(e.getIndex());
		if(ix==null)
			return -1;
		return ix;
	}

    public boolean contains(int key){
        return index_hash.containsKey(key);
    }


    public boolean quickAdd(int key ,double
            value){
        if(index_hash.containsKey(key)){
            Element e = elements.get
                    (index_hash.get(key));
            if(e != null){
                e.setValue(e.getValue() + value);
            }else{
                return false;
            }
        }else{
            addElement(new Element(key,value));
        }
        return true;
    }
	
}
