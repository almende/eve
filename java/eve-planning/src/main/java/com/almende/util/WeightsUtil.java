package com.almende.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joda.time.DateTime;

import com.almende.eve.entity.Weight;

/**
 * @class WeightsUtil
 * Can order and merge a list with weights: intervals in time with a weight
 * 
 * @author Jos de Jong, Almende B.V. 2012
 */
public class WeightsUtil {
	/**
	 * Order the intervals by start date
	 * @param intervals   A list with time intervals
	 */
	public static void order(List<Weight> intervals) {
		class IntervalComparator implements Comparator<Weight> {
			@Override
			public int compare(Weight a, Weight b) {
				if (a.getStart().isAfter(b.getStart())) return 1;
				if (a.getStart().isBefore(b.getStart())) return -1;
				return 0;
			}
		}
		
		IntervalComparator comparator = new IntervalComparator();
		Collections.sort(intervals, comparator);
	}
	
	/**
	 * Merge overlapping weighted intervals in given list
	 * @param intervals
	 * @return
	 */
	public static List<Weight> merge(List<Weight> intervals) {		
		// copy the intervals
		ArrayList<Weight> merged = new ArrayList<Weight>();
		
		for (Weight newInterval : intervals) {
			// create a copy of the interval to be merged
			Weight interval = new Weight(newInterval);

			// check if the interval overlaps with any of the others
			int i = 0;
			while (i < merged.size() && interval != null) {
				Weight other = merged.get(i);
				if (interval.getInterval().overlaps(other.getInterval())) {
					// we have an overlap
					// split these two overlapping intervals in three intervals:
					// leftInterval, centerInterval, rightInterval

					// find the start of the left interval
					DateTime start = null;
					Double leftWeight = null;
					if (interval.getStart().isBefore(other.getStart())) {
						start = interval.getStart();
						leftWeight = interval.getWeight();
					} 
					else {
						start = other.getStart();
						leftWeight = other.getWeight();
					}
					
					// find the start of the center interval (= end of left interval)
					DateTime centerStart = null;
					if (interval.getStart().isAfter(other.getStart())) {
						centerStart = interval.getStart();
					} 
					else {
						centerStart = other.getStart();
					}

					Double centerWeight = interval.getWeight() + other.getWeight();
					
					// find the end of the center interval (= start of right interval)
					DateTime centerEnd = null;
					if (interval.getEnd().isBefore(other.getEnd())) {
						centerEnd = interval.getEnd();
					} 
					else {
						centerEnd = other.getEnd();
					}

					// find the end of the right interval
					DateTime end = null;
					Double rightWeight = null;
					if (interval.getEnd().isAfter(other.getEnd())) {
						end = interval.getEnd();
						rightWeight = interval.getWeight();
					} 
					else {
						end = other.getEnd();
						rightWeight = other.getWeight();
					}

					// replace old, merged interval with the overlapping part
					if (centerEnd.isAfter(centerStart)) {
						Weight centerInterval = new Weight(
								centerStart, centerEnd, centerWeight);
						merged.set(i, centerInterval);
					}

					// insert a new interval left from the overlapping part
					if (centerStart.isAfter(start)) {
						Weight leftInterval = new Weight(
								start, centerStart, leftWeight);
						merged.add(i, leftInterval);
						i++;
					}
					
					// replace interval with the residual, the non-overlapping
					// right part of the two intervals
					if (end.isAfter(centerEnd)) {
						interval = new Weight(centerEnd, end, 
								rightWeight);
					}
					else {
						interval = null;
					}
				}
				else if (interval.getEnd().isBefore(other.getStart()) ||
						interval.getEnd().equals(other.getStart())) {
					// interval is before merged interval. Insert the interval
					// at this position in the list.
					merged.add(i, interval);
					interval = null;
					i++;
				}
				else {
					// interval is after merged interval. check the next merged
					// interval in the list
				}

				i++;
			}
			
			// if interval is not yet merged, add it to the end of the list
			if (interval != null) {
				merged.add(interval);
				interval = null;
			}			
		}
		
		// merge intervals which exactly align 
		// (end equals start and weight is the same)
		int i = 1;
		while (i < merged.size()) {
			Weight prev = merged.get(i - 1);
			Weight cur = merged.get(i);
			if (prev.getWeight().equals(cur.getWeight()) && 
					prev.getEnd().equals(cur.getStart())) {
				Weight combi = new Weight(prev.getStart(),
						cur.getEnd(), prev.getWeight());
				merged.set(i - 1, combi);
				merged.remove(i);
				i--;
			}
			i++;
		}
		
		// return merged intervals
		return merged;
	}
	
	// TODO: move tests to a unittest
	public static void main (String[] args) {
		List<Weight> intervals = new ArrayList<Weight>();

		intervals.add(new Weight(
				new DateTime(2012, 8, 4, 8, 0, 0), 
				new DateTime(2012, 8, 4, 10, 0, 0), 
				new Double(1)));
		
		intervals.add(new Weight(
				new DateTime(2012, 8, 4, 12, 0, 0), 
				new DateTime(2012, 8, 4, 14, 0, 0), 
				new Double(1)));
		
		intervals.add(new Weight(
				new DateTime(2012, 8, 4, 13, 0, 0), 
				new DateTime(2012, 8, 4, 16, 0, 0), 
				new Double(1)));

		intervals.add(new Weight(
				new DateTime(2012, 8, 4, 3, 0, 0), 
				new DateTime(2012, 8, 4, 4, 0, 0), 
				new Double(1)));
		
		intervals.add(new Weight(
				new DateTime(2012, 8, 4, 3, 0, 0), 
				new DateTime(2012, 8, 4, 4, 0, 0), 
				new Double(1)));
		
		intervals.add(new Weight(
				new DateTime(2012, 8, 4, 3, 0, 0), 
				new DateTime(2012, 8, 4, 5, 0, 0), 
				new Double(1)));
		
		intervals.add(new Weight(
				new DateTime(2012, 8, 4, 17, 30, 0), 
				new DateTime(2012, 8, 4, 18, 0, 0), 
				new Double(1.5)));
		intervals.add(new Weight(
				new DateTime(2012, 8, 4, 17, 0, 0), 
				new DateTime(2012, 8, 4, 17, 30, 0), 
				new Double(1.5)));
		
		List<Weight> merged = WeightsUtil.merge(intervals); 
		for (Weight i : merged) {
			System.out.println(i);
		}
	}
}
