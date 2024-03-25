package net.lecousin.ant.core.reflection;

public class ReflectionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ReflectionException(String message, ReflectiveOperationException cause) {
		super(message, cause);
	}
	
	public ReflectionException(String message) {
		super(message);
	}
	
}
