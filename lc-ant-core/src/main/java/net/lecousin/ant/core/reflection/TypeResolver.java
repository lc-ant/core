package net.lecousin.ant.core.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.mutable.MutableObject;

public class TypeResolver {

	public static void visit(Type type, TypeVisitor visitor) {
		visitType(type, Map.of(), visitor);
	}
	
	public static void visit(ResolvedType type, TypeVisitor visitor) {
		if (type instanceof ResolvedType.SingleClass c)
			visitClass(c.getSingleClass(), Map.of(), visitor);
		else if (type instanceof ResolvedType.Parameterized p) {
			Class<?> base = p.getBase();
			TypeVariable[] params = base.getTypeParameters();
			Map<String, ResolvedType> parameters = new HashMap<>();
			for (int i = 0; i < params.length; ++i)
				parameters.put(params[i].getName(), p.getParameters()[i]);
			visitClass(base, parameters, visitor);
		} else if (type instanceof ResolvedType.Bounded b) {
			// TODO
			throw new ReflectionException("TODO: " + type);
		} else if (type instanceof ResolvedType.Array a) {
			throw new ReflectionException("Cannot visit an array");
		} else {
			throw new ReflectionException("Unexpected resolved type: " + type);
		}
			
	}
	
	private static boolean visitClass(Class<?> clazz, Map<String, ResolvedType> parameters, TypeVisitor visitor) {
		if (clazz.equals(Object.class)) return true;
		if (!visitor.enterClass(clazz, parameters)) return false;
		if (visitor.doVisitFields(clazz, parameters)) {
			for (Field f : clazz.getDeclaredFields()) {
				ResolvedType type = resolve(f.getGenericType(), parameters, new Type[0]);
				if (!visitor.visitField(f, type)) return false;
			}
		}
		if (visitor.doVisitMethods(clazz, parameters)) {
			for (Method m : clazz.getDeclaredMethods()) {
				TypeVariable[] methodGenerics = m.getTypeParameters();
				Map<String, ResolvedType> methodParameters;
				if (methodGenerics.length == 0)
					methodParameters = parameters;
				else {
					methodParameters = new HashMap<>(parameters);
					for (int i = 0; i < methodGenerics.length; ++i) {
						Type[] bounds = methodGenerics[i].getBounds();
						ResolvedType type;
						if (bounds.length == 1)
							type = resolve(bounds[0], methodParameters, new Type[0]);
						else {
							ResolvedType[] resolved = new ResolvedType[bounds.length];
							for (int j = 0; j < resolved.length; ++j)
								resolved[j] = resolve(bounds[i], methodParameters, new Type[0]);
							type = new ResolvedType.Bounded(new ResolvedType[0], resolved);
						}
						methodParameters.put(methodGenerics[i].getName(), type);
					}
				}
				ResolvedType returnType = resolve(m.getGenericReturnType(), methodParameters, new Type[0]);
				ResolvedType[] params = new ResolvedType[m.getParameterCount()];
				for (int i = 0; i < params.length; ++i)
					params[i] = resolve(m.getGenericParameterTypes()[i], methodParameters, new Type[0]);
				if (!visitor.visitMethod(m, returnType, params)) return false;
			}
		}
		Type superClassType = clazz.getGenericSuperclass();
		if (superClassType != null)
			if (!visitType(superClassType, parameters, visitor)) return false;
		Type[] interfaces = clazz.getGenericInterfaces();
		for (Type i : interfaces)
			if (!visitType(i, parameters, visitor)) return false;
		if (!visitor.leaveClass(clazz, parameters)) return false;
		return true;
	}
	
	private static boolean visitType(Type type, Map<String, ResolvedType> parameters, TypeVisitor visitor) {
		if (type instanceof Class c) {
			return visitClass(c, Map.of(), visitor);
		}
		if (type instanceof ParameterizedType parameterized) {
			return visitParameterized(parameterized, parameters, visitor);
		}
		if (type instanceof TypeVariable variable) {
			ResolvedType resolved = parameters.get(variable.getName());
			if (resolved == null) throw new ReflectionException("Type variable " + variable + " cannot be resolved");
			if (resolved instanceof ResolvedType.SingleClass singleClass) {
				return visitClass(singleClass.getSingleClass(), Map.of(), visitor);
			}
			if (resolved instanceof ResolvedType.Parameterized parameterized) {
				return visitResolvedParameterized(parameterized, visitor);
			}
			if (resolved instanceof ResolvedType.Bounded bounded) {
				// upper = extends, lower = super
				// we are sure it is of types 'upper' as it extend it
				// there is at least one upper wich is Object
				for (ResolvedType bound : bounded.getUpperBounds()) {
					if (bound instanceof ResolvedType.SingleClass boundClass) {
						if (!visitClass(boundClass.getSingleClass(), Map.of(), visitor)) return false;
					} else if (bound instanceof ResolvedType.Parameterized boundParameterized) {
						if (!visitResolvedParameterized(boundParameterized, visitor)) return false;
					} else
						throw new ReflectionException("Unexpected resolved bound: " + bound);
				}
				return true;
			}
		}
		throw new ReflectionException("Unexpected type: " + type);
	}
	
	private static boolean visitParameterized(ParameterizedType parameterized, Map<String, ResolvedType> parameters, TypeVisitor visitor) {
		ResolvedType base = resolve(parameterized.getRawType(), parameters, new Type[0]);
		if (base instanceof ResolvedType.SingleClass baseSingleClass) {
			Class<?> baseClass = baseSingleClass.getSingleClass();
			TypeVariable<?>[] baseClassVariables = baseClass.getTypeParameters();
			Map<String, ResolvedType> baseClassParameters = new HashMap<>();
			for (int i = 0; i < baseClassVariables.length; ++i) {
				String name = baseClassVariables[i].getName();
				ResolvedType resolvedParameter = resolve(parameterized.getActualTypeArguments()[i], parameters, baseClassVariables[i].getBounds());
				baseClassParameters.put(name, resolvedParameter);
			}
			return visitClass(baseClass, baseClassParameters, visitor);
		}
		throw new ReflectionException("Unexpected resolved base class for parameterized type: " + base);
	}
	
	private static boolean visitResolvedParameterized(ResolvedType.Parameterized parameterized, TypeVisitor visitor) {
		Class<?> baseClass = parameterized.getBase();
		TypeVariable<?>[] baseClassVariables = baseClass.getTypeParameters();
		Map<String, ResolvedType> baseClassParameters = new HashMap<>();
		for (int i = 0; i < baseClassVariables.length; ++i) {
			String name = baseClassVariables[i].getName();
			ResolvedType resolvedParameter = parameterized.getParameters()[i];
			baseClassParameters.put(name, resolvedParameter);
		}
		return visitClass(baseClass, baseClassParameters, visitor);
	}
	
	private static ResolvedType resolve(Type type, Map<String, ResolvedType> parameters, Type[] upperBounds) {
		if (type instanceof Class c) {
			if (c.isArray()) {
				return new ResolvedType.Array(new ResolvedType.SingleClass(c.getComponentType()));
			}
			return new ResolvedType.SingleClass(c);
		}
		if (type instanceof ParameterizedType p) {
			ResolvedType base = resolve(p.getRawType(), parameters, upperBounds);
			if (base instanceof ResolvedType.SingleClass baseSingleClass) {
				Class<?> baseClass = baseSingleClass.getSingleClass();
				ResolvedType[] params = new ResolvedType[p.getActualTypeArguments().length];
				for (int i = 0; i < params.length; ++i)
					params[i] = resolve(p.getActualTypeArguments()[i], parameters, new Type[0]);
				return new ResolvedType.Parameterized(baseClass, params);
			} else
				throw new ReflectionException("Unexpected parameterized base type: " + base);
		}
		if (type instanceof TypeVariable v) {
			ResolvedType t = parameters.get(v.getName());
			if (t == null)
				return new ResolvedType.SingleClass(Object.class);
			return t;
		}
		if (type instanceof GenericArrayType a) {
			return new ResolvedType.Array(resolve(a.getGenericComponentType(), parameters, new Type[0]));
		}
		if (type instanceof WildcardType w) {
			ResolvedType[] lower = new ResolvedType[w.getLowerBounds().length];
			for (int i = 0; i < lower.length; ++i)
				lower[i] = resolve(w.getLowerBounds()[i], parameters, new Type[0]);
			ResolvedType[] upper = new ResolvedType[w.getUpperBounds().length];
			for (int i = 0; i < upper.length; ++i)
				upper[i] = resolve(w.getUpperBounds()[i], parameters, new Type[0]);
			return new ResolvedType.Bounded(lower, upper);
		}
		// TODO
		throw new ReflectionException("TODO: " + type + " (" + type.getClass() + ")");
	}
	
	public static ResolvedType getInheritedType(ResolvedType type, Class<?> inheritedClass) {
		MutableObject<ResolvedType> found = new MutableObject<>(null);
		visit(type, new TypeVisitor() {

			@Override
			public boolean enterClass(Class<?> clazz, Map<String, ResolvedType> parameters) {
				if (clazz.equals(inheritedClass)) {
					TypeVariable[] types = clazz.getTypeParameters();
					if (types.length == 0)
						found.setValue(new ResolvedType.SingleClass(clazz));
					else {
						ResolvedType[] resolved = new ResolvedType[types.length];
						for (int i = 0; i < resolved.length; ++i) {
							resolved[i] = Optional.ofNullable(parameters.get(types[i].getName())).orElse(new ResolvedType.SingleClass(Object.class));
						}
						found.setValue(new ResolvedType.Parameterized(clazz, resolved));
					}
					return false;
				}
				return true;
			}

			@Override
			public boolean visitField(Field field, ResolvedType type) {
				return true;
			}

			@Override
			public boolean visitMethod(Method method, ResolvedType returnType, ResolvedType[] parameters) {
				return true;
			}

			@Override
			public boolean leaveClass(Class<?> clazz, Map<String, ResolvedType> parameters) {
				return true;
			}

			@Override
			public boolean doVisitFields(Class<?> clazz, Map<String, ResolvedType> parameters) {
				return false;
			}

			@Override
			public boolean doVisitMethods(Class<?> clazz, Map<String, ResolvedType> parameters) {
				return false;
			}
			
		});
		return found.getValue();
	}
	
}
