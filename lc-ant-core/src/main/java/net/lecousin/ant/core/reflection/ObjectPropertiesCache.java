package net.lecousin.ant.core.reflection;

import java.util.HashMap;
import java.util.Map;

public class ObjectPropertiesCache {

	private Map<Object, Map<ClassProperty, Object>> cache = new HashMap<>();
	
	public Object get(Object container, ClassProperty property) {
		return cache.computeIfAbsent(container, k -> new HashMap<>())
		.computeIfAbsent(property, p -> p.getValue(container));
	}
	
}
