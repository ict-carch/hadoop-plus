package cn.ac.ict.htc.tools;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class TopK<T> {
	private int k;
	private Comparator<? super T> queueingComparator;
	private Comparator<? super T> sortingComparator;
	private PriorityQueue<T> queue;

	//
	public TopK(int k, Comparator<? super T> comparator) {
		Preconditions.checkArgument(k > 0);
		this.k = k;
		Preconditions.checkNotNull(comparator);
		this.queueingComparator = queueingComparator(comparator);
		this.sortingComparator = sortingComparator(comparator);
		this.queue = new PriorityQueue<T>(k + 1, queueingComparator);
	}

	private Comparator<? super T> queueingComparator(
			Comparator<? super T> comparator) {
		return comparator;
	}

	private Comparator<? super T> sortingComparator(
			Comparator<? super T> comparator) {
		return comparator;
	}

	public void offer(T item) {
		if (queue.size() < k) {
			queue.add(item);
		} else if (queueingComparator.compare(item, queue.peek()) > 0) {
			queue.add(item);
			queue.poll();
		}
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	public int size() {
		return queue.size();
	}

	public List<T> retrieve() {
		List<T> topItems = Lists.newArrayList(queue);
		Collections.sort(topItems, sortingComparator);
		return topItems;
	}

	// for test
	public static void main(String[] args) {
		TopK<Element> topk = new TopK<Element>(10, ElementList.BY_VALUE);
		ElementList elist = new ElementList();
		Random ran = new Random();
		for (int i = 0; i < 10; i++) {
			int dd = ran.nextInt(1000);
			double dt = ran.nextDouble();
			topk.offer(new Element(dd, dt));
			elist.addElement(new Element(dd, dt));
		}
		List<Element> ll = topk.retrieve();
		System.out.println(ll);
		elist.sortByValue();
		System.out.println(elist.toStringByGroup());
	}
}
