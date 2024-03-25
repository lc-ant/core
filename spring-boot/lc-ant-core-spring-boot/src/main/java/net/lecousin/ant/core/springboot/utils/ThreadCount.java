package net.lecousin.ant.core.springboot.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ThreadCount {

	@Getter
	private final int count;
	
	public static ThreadCount parse(String s) {
		if (s == null) return new ThreadCount(1);
		if (s.endsWith("C")) {
			try {
				double factor = Double.parseDouble(s.substring(0, s.length() - 1));
				return new ThreadCount((int) Math.min(100, Math.max(1, Math.round(factor * Runtime.getRuntime().availableProcessors()))));
			} catch (NumberFormatException e) {
				// ignore
			}
		} else {
			try {
				int nb = Integer.parseInt(s);
				return new ThreadCount(Math.max(1, nb));
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return new ThreadCount(1);
	}
	
}
