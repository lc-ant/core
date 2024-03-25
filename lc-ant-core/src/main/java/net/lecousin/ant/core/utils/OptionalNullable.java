package net.lecousin.ant.core.utils;

public final class OptionalNullable<T> {

	private final boolean isEmpty;
	private final T value;
	
	private OptionalNullable(T value) {
		isEmpty = false;
		this.value = value;
	}
	
	private OptionalNullable() {
		isEmpty = true;
		value = null;
	}
	
	public static <T> OptionalNullable<T> of(T value) {
		return new OptionalNullable<>(value);
	}
	
	public static <T> OptionalNullable<T> empty() {
		return new OptionalNullable<>();
	}
	
	public boolean isEmpty() {
		return isEmpty;
	}
	
	public boolean isPresent() {
		return !isEmpty();
	}
	
	public T get() {
		return value;
	}
	
}
