package net.lecousin.ant.core.mapping;

public interface ClassMapper<S, T> {

	Class<S> sourceType();
	
	Class<T> targetType();
	
	T map(S source);
	
}
