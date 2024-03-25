package net.lecousin.ant.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ClassProperty {

	protected Field field;
	protected Method getter;
	protected Method setter;
	protected String name;
	protected ResolvedType type;
	protected List<Annotation> annotations = new LinkedList<>();
	
	public boolean canGet() {
		return field != null || getter != null;
	}
	
	public boolean canSet() {
		return field != null || setter != null;
	}
	
	public String getName() {
		return name;
	}
	
	public ResolvedType getType() {
		return type;
	}
	
	boolean keepAccessible() {
		if (field != null && (field.getModifiers() & Modifier.PUBLIC) == 0) field = null;
		if (getter != null && (getter.getModifiers() & Modifier.PUBLIC) == 0) getter = null;
		if (setter != null && (setter.getModifiers() & Modifier.PUBLIC) == 0) setter = null;
		if (field == null && getter == null && setter == null) return false;
		return true;
	}
	
	public <A extends Annotation> boolean hasAnnotation(Class<A> annotationType) {
		return getAnnotation(annotationType).isPresent();
	}
	
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationType) {
		return annotations.stream().filter(a -> annotationType.isAssignableFrom(a.annotationType())).map(a -> (A) a).findAny();
	}
	
	public <A extends Annotation> A getRequiredAnnotation(Class<A> annotationType) {
		return getAnnotation(annotationType).orElseThrow();
	}
	
	public List<Annotation> getAnnotations() {
		return annotations;
	}
	
	public Object getValue(Object instance) {
		try {
			if (getter != null) return getter.invoke(instance);
			return field.get(instance);
		} catch (ReflectiveOperationException e) {
			throw new ReflectionException("Cannot get value for property " + name, e);
		}
	}
	
	public void setValue(Object instance, Object value) {
		try {
			if (setter != null) {
				setter.invoke(instance, value);
			} else {
				field.set(instance, value);
			}
		} catch (ReflectiveOperationException e) {
			throw new ReflectionException("Cannot set value for property " + name, e);
		}
	}
	
}
