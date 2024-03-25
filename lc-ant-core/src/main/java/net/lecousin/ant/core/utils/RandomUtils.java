package net.lecousin.ant.core.utils;

import java.util.Random;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RandomUtils {
	
	private static final char[] SYMBOLS = { ',', '?', ';', '.', ':', '/', '!', '&', '#', '{', '}', '[', ']', '(', ')', '_', '-', '@', '+', '=', '*' };
	
	public static String generateAlphaNumericWithSymbols(Random random, int size) {
		char[] chars = new char[size];
		for (int i = 0; i < size; ++i) {
			int r = random.nextInt(10 + 26 + 26 + SYMBOLS.length);
			if (r < 10) chars[i] = (char) ('0' + r);
			else if (r < 10 + 26) chars[i] = (char) ('a' + r - 10);
			else if (r < 10 + 26 + 26) chars[i] = (char) ('A' + r - 10 - 26);
			else chars[i] = SYMBOLS[r - 10 - 26 - 26];
		}
		return new String(chars);
	}
	
}
