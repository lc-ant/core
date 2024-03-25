package net.lecousin.ant.core.springboot.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpringEnvironmentUtils {

	public static Map<String, Serializable> getPropertyAsMap(Environment env, String property) {
		if (env instanceof ConfigurableEnvironment c)
			return getPropertyAsMap(c, property);
		return null;
	}
	
	public static Map<String, Serializable> getPropertyAsMap(ConfigurableEnvironment env, String property) {
		Map<String, Serializable> map = new HashMap<>();
		env.getPropertySources().forEach(source -> populatePropertyMapFromSource(map, source, property + "."));
		if (map.isEmpty())
			return null;
		return map;
	}
	
	private static void populatePropertyMapFromSource(Map<String, Serializable> map, PropertySource<?> source, String basePropertyName) {
		if (source instanceof EnumerablePropertySource e) {
			for (var name : e.getPropertyNames()) {
				if (name.startsWith(basePropertyName))
					putPropertyValueInMap(map, name.substring(basePropertyName.length()), e.getProperty(name));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void putPropertyValueInMap(Map<String, Serializable> map, String name, Object value) {
		if (value instanceof Serializable v) {
			int i = name.indexOf('.');
			if (i < 0) {
				map.put(name, v);
			} else {
				String key = name.substring(0, i);
				Serializable kv = map.get(key);
				if (kv == null || !(kv instanceof Map)) {
					kv = new HashMap<String, Serializable>();
					map.put(key, kv);
				}
				putPropertyValueInMap((Map<String, Serializable>) kv, name.substring(i + 1), value);
			}
		}
	}
	
}
