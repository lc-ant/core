package net.lecousin.ant.core.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtils {
	
	public static Optional<ClassProperty> getClassProperty(Class<?> clazz, String propertyName) {
		return Optional.ofNullable(getAllProperties(clazz).get(propertyName));
	}

	public static Map<String, ClassProperty> getAllProperties(Type type) {
		ClassPropertiesBuilder builder = new ClassPropertiesBuilder();
		TypeResolver.visit(type, builder);
		builder.properties.entrySet().removeIf(e -> !e.getValue().keepAccessible());
		return builder.properties;
	}
	
	public static Map<String, ClassProperty> getAllProperties(ResolvedType type) {
		ClassPropertiesBuilder builder = new ClassPropertiesBuilder();
		TypeResolver.visit(type, builder);
		builder.properties.entrySet().removeIf(e -> !e.getValue().keepAccessible());
		return builder.properties;
	}
	
	private static final class ClassPropertiesBuilder implements TypeVisitor {
		private Map<String, ClassProperty> properties = new HashMap<>();
		
		@Override
		public boolean visitField(Field field, ResolvedType type) {
			if ((field.getModifiers() & Modifier.STATIC) != 0) return true;
			ClassProperty p = properties.get(field.getName());
			if (p == null) {
				p = new ClassProperty();
				p.name = field.getName();
				p.type = type;
				p.field = field;
				p.annotations.addAll(Arrays.asList(field.getAnnotations()));
				properties.put(p.name, p);
			} else if (p.type.equals(type) && p.field == null) {
				p.field = field;
				p.annotations.addAll(Arrays.asList(field.getAnnotations()));
			}
			return true;
		}
		
		private static final String SETTER_PREFIX = "set";
		private static final String GETTER_PREFIX = "get";
		private static final String GETTER_BOOLEAN_PREFIX = "is";
		
		@Override
		public boolean visitMethod(Method method, ResolvedType returnType, ResolvedType[] parameters) {
			if ((method.getModifiers() & Modifier.STATIC) != 0) return true;
			if (returnType instanceof ResolvedType.SingleClass sc && sc.getSingleClass().equals(void.class)) {
				// no returnType => may be a setter
				if (parameters.length == 1) {
					// only one parameter
					String name = method.getName();
					if (name.startsWith(SETTER_PREFIX)) {
						// this is a setter
						name = getNameFromMethodName(name.substring(SETTER_PREFIX.length()));
						ClassProperty p = properties.get(name);
						if (p == null) {
							p = new ClassProperty();
							p.name = name;
							p.type = parameters[0];
							p.setter = method;
							p.annotations.addAll(Arrays.asList(method.getAnnotations()));
							properties.put(name, p);
						} else if (p.type.equals(parameters[0]) && p.setter == null) {
							p.setter = method;
							p.annotations.addAll(Arrays.asList(method.getAnnotations()));
						}
					}
				}
			} else if (parameters.length == 0) {
				// no parameter and return type => may be a setter
				String name = method.getName();
				int start = 0;
				if (name.startsWith(GETTER_PREFIX))
					start = GETTER_PREFIX.length();
				else if (name.startsWith(GETTER_BOOLEAN_PREFIX)) {
					if (returnType instanceof ResolvedType.SingleClass c) {
						if (c.getSingleClass().equals(boolean.class) || c.getSingleClass().equals(Boolean.class)) {
							start = GETTER_BOOLEAN_PREFIX.length();
						}
					}
				}
				if (start > 0 && name.length() > start) {
					name = getNameFromMethodName(name.substring(start));
					ClassProperty p = properties.get(name);
					if (p == null) {
						p = new ClassProperty();
						p.name = name;
						p.type = returnType;
						p.getter = method;
						p.annotations.addAll(Arrays.asList(method.getAnnotations()));
						properties.put(name, p);
					} else if (p.type.equals(returnType) && p.getter == null) {
						p.getter = method;
						p.annotations.addAll(Arrays.asList(method.getAnnotations()));
					}
				}
			}
			return true;
		}

		@Override
		public boolean enterClass(Class<?> clazz, Map<String, ResolvedType> parameters) {
			return true;
		}

		@Override
		public boolean doVisitFields(Class<?> clazz, Map<String, ResolvedType> parameters) {
			return true;
		}

		@Override
		public boolean doVisitMethods(Class<?> clazz, Map<String, ResolvedType> parameters) {
			return true;
		}

		@Override
		public boolean leaveClass(Class<?> clazz, Map<String, ResolvedType> parameters) {
			return true;
		}
	}
	
	private static String getNameFromMethodName(String name) {
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}
	
}
