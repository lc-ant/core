package net.lecousin.ant.core.utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Combinations {

	public static <T> List<List<T>> combine(List<List<T>> possibilities) {
		if (possibilities.isEmpty()) return List.of();
		List<List<T>> combinations = new LinkedList<>();
		Iterator<List<T>> it = possibilities.iterator();
		List<T> firstToCombine = it.next();
		for (var first : firstToCombine) combinations.add(List.of(first));
		while (it.hasNext())
			combinations = combine(combinations, it.next());
		return combinations;
	}
	
	public static <T> List<List<T>> combine(List<List<T>> list, List<T> add) {
		List<List<T>> result = new LinkedList<>();
		for (var currentTestCase : list) {
			for (var newTestCase : add) {
				List<T> combine = new LinkedList<>(currentTestCase);
				combine.add(newTestCase);
				result.add(combine);
			}
		}
		return result;
	}
	
}
