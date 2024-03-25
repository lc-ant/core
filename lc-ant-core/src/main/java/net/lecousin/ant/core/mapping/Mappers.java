package net.lecousin.ant.core.mapping;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.reflection.ClassProperty;
import net.lecousin.ant.core.reflection.ReflectionException;
import net.lecousin.ant.core.reflection.ReflectionUtils;
import net.lecousin.ant.core.reflection.ResolvedType;
import net.lecousin.ant.core.utils.OptionalNullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Mappers {

	public static final ObjectMapper YAML_MAPPER;
	public static final ObjectMapper OBJECT_MAPPER;
	
	static {
		YAML_MAPPER = new ObjectMapper(new YAMLFactory());
		YAML_MAPPER.findAndRegisterModules();
		OBJECT_MAPPER = new ObjectMapper();
		OBJECT_MAPPER.findAndRegisterModules();
	}

	private static final Map<Class<?>, Map<Class<?>, ClassMapper<?, ?>>> CLASS_MAPPERS = new HashMap<>();
	private static final List<GenericMapper> GENERIC_MAPPERS = new LinkedList<>();
	private static final List<TypeConverter> CONVERTERS = new LinkedList<>();
	
	public static <S, T> T map(S source, Class<T> targetType) {
		if (source != null && targetType.isAssignableFrom(source.getClass())) return (T) source;
		return (T) map(source, new ResolvedType.SingleClass(targetType));
	}

	public static Object map(Object source, ResolvedType targetType) {
		return map(new ResolvedType.SingleClass(source.getClass()), source, targetType);
	}
	
	public static Object map(ResolvedType sourceType, Object source, ResolvedType targetType) {
		OptionalNullable<Object> mapped = tryMap(sourceType, source, targetType);
		if (mapped.isPresent())
			return mapped.get();
		
		Class<?> from = source.getClass();
		Class<?> to = null;
		if (targetType instanceof ResolvedType.SingleClass c) {
			to = c.getSingleClass();
		} else if (targetType instanceof ResolvedType.Parameterized p) {
			to = p.getBase();
		} else
			throw new ReflectionException("Unexpected type: " + targetType);
		
		if (to.isAssignableFrom(String.class))
			return source.toString();

		Object target;
		try {
			Constructor ctor = to.getConstructor();
			target = ctor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new ReflectionException("Cannot map from " + source + " (" + from + ") to " + to, e);
		}
		
		Map<String, ClassProperty> sourceProperties = ReflectionUtils.getAllProperties(sourceType);
		Map<String, ClassProperty> targetProperties = ReflectionUtils.getAllProperties(targetType);
		for (var sourceProperty : sourceProperties.values()) {
			ClassProperty targetProperty = targetProperties.get(sourceProperty.getName());
			if (targetProperty == null)
				throw new ReflectionException("Cannot map property " + sourceProperty.getName() + " from " + from.getName() + " to class " + to.getName());
			targetProperty.setValue(target, map(sourceProperty.getType(), sourceProperty.getValue(source), targetProperty.getType()));
		}
		if (sourceType instanceof ResolvedType.SingleClass && targetType instanceof ResolvedType.SingleClass) {
			// we have a new Class to Class mapper working, register it
			try {
				registerClassMapper(createClassMapper(from, to));
			} catch (Exception e) {
				// should not happen
			}
		}
		return target;
	}
	
	public static <S, T> Function<S, T> createMapper(Class<S> from, Class<T> to) throws ReflectiveOperationException {
		ClassMapper<S, T> mapper = createClassMapper(from, to);
		registerClassMapper(mapper);
		return mapper::map;
	}
	
	private static <S, T> ClassMapper<S, T> createClassMapper(Class<S> from, Class<T> to) throws ReflectiveOperationException {
		Constructor ctor = to.getConstructor();
		return new ClassMapper<>() {
			@Override
			public Class<S> sourceType() {
				return from;
			}
			
			@Override
			public Class<T> targetType() {
				return to;
			}
			
			@Override
			public T map(S source) {
				Object target;
				try {
					target = ctor.newInstance();
				} catch (Exception e) {
					throw new ReflectionException("Cannot map from " + source + " (" + from + ") to " + to);
				}
				
				Map<String, ClassProperty> sourceProperties = ReflectionUtils.getAllProperties(from);
				Map<String, ClassProperty> targetProperties = ReflectionUtils.getAllProperties(to);
				for (var sourceProperty : sourceProperties.values()) {
					ClassProperty targetProperty = targetProperties.get(sourceProperty.getName());
					if (targetProperty == null) {
						if (sourceProperty.hasAnnotation(JsonIgnore.class))
							continue;
						throw new ReflectionException("Cannot map property " + sourceProperty.getName() + " from " + from.getName() + " to class " + to.getName());
					}
					targetProperty.setValue(target, Mappers.map(sourceProperty.getType(), sourceProperty.getValue(source), targetProperty.getType()));
				}
				return (T) target;
			}
		};
	}
	
	public static OptionalNullable<Object> tryMap(ResolvedType sourceType, Object source, ResolvedType targetType) {
		if (source == null) {
			if (targetType instanceof ResolvedType.SingleClass c) {
				if (c.getSingleClass().isPrimitive())
					throw new ReflectionException("Cannot map null to a primitive type");
				if (Optional.class.equals(c.getSingleClass()))
					return OptionalNullable.of(Optional.empty());
			}
			if (targetType instanceof ResolvedType.Parameterized p) {
				if (p.getBase().equals(Optional.class))
					return OptionalNullable.of(Optional.empty());
			}
			return OptionalNullable.of(null);
		}
		
		// get real type if given type is Object
		if (sourceType instanceof ResolvedType.SingleClass c && Object.class.equals(c.getSingleClass()))
			sourceType = new ResolvedType.SingleClass(source.getClass());
		
		// convert primitive to wrapper
		if (sourceType instanceof ResolvedType.SingleClass c && c.getSingleClass().isPrimitive())
			sourceType = new ResolvedType.SingleClass(primitiveToWrapper(c.getSingleClass()));
		if (targetType instanceof ResolvedType.SingleClass c && c.getSingleClass().isPrimitive())
			targetType = new ResolvedType.SingleClass(primitiveToWrapper(c.getSingleClass()));
		if (sourceType.equals(targetType)) return OptionalNullable.of(source);
		
		for (var gm : GENERIC_MAPPERS) {
			OptionalNullable<Object> mapped = gm.map(sourceType, source, targetType);
			if (mapped.isPresent())
				return mapped;
		}
		OptionalNullable<Object> mapped = useClassMapper(sourceType, source, targetType);
		if (mapped.isPresent())
			return mapped;

		LinkedList<ResolvedType> done = new LinkedList<>();
		done.add(sourceType);
		mapped = findPossibleConversionPath(done, new LinkedList<>(), List.of(), source, targetType);
		if (mapped.isPresent())
			return mapped;
		return OptionalNullable.empty();
	}
	
	public static Class<?> primitiveToWrapper(Class<?> primitive) {
		if (void.class.equals(primitive)) return Void.class;
		if (boolean.class.equals(primitive)) return Boolean.class;
		if (byte.class.equals(primitive)) return Byte.class;
		if (short.class.equals(primitive)) return Short.class;
		if (int.class.equals(primitive)) return Integer.class;
		if (long.class.equals(primitive)) return Long.class;
		if (char.class.equals(primitive)) return Character.class;
		if (float.class.equals(primitive)) return Float.class;
		return Double.class;
	}
	
	private static OptionalNullable<Object> findPossibleConversionPath(
		LinkedList<ResolvedType> alreadyDone,
		List<ResolvedType> toExclude,
		List<Pair<TypeConverter, ResolvedType>> currentPath,
		Object source,
		ResolvedType targetType
	) {
		Map<ResolvedType, TypeConverter> possibleTargets = new HashMap<>();
		for (var converter : CONVERTERS) {
			var canConvert = converter.canConvert(alreadyDone.getLast(), targetType);
			if (canConvert.contains(targetType)) {
				List<Pair<TypeConverter, ResolvedType>> path = new LinkedList<>(currentPath);
				path.add(Pair.of(converter, targetType));
				return OptionalNullable.of(applyConversionPath(path, source, alreadyDone.getFirst()));
			}
			canConvert.forEach(t -> possibleTargets.put(t, converter));
		}
		alreadyDone.forEach(done -> possibleTargets.remove(done));
		toExclude.forEach(t -> possibleTargets.remove(t));
		if (possibleTargets.isEmpty())
			return OptionalNullable.empty();
		toExclude.addAll(possibleTargets.keySet());
		for (var possibility : possibleTargets.entrySet()) {
			List<Pair<TypeConverter, ResolvedType>> newPath = new LinkedList<>(currentPath);
			newPath.add(Pair.of(possibility.getValue(), possibility.getKey()));

			for (var gm : GENERIC_MAPPERS) {
				if (gm.canMap(possibility.getKey(), targetType)) {
					Object o = applyConversionPath(newPath, source, alreadyDone.getFirst());
					return gm.map(possibility.getKey(), o, targetType);
				}
			}

			LinkedList<ResolvedType> newAlreadyDone = new LinkedList<>(alreadyDone);
			newAlreadyDone.add(possibility.getKey());
			OptionalNullable<Object> mapped = findPossibleConversionPath(newAlreadyDone, toExclude, newPath, source, targetType);
			if (mapped.isPresent())
				return mapped;
		}
		return OptionalNullable.empty();
	}
	
	private static Object applyConversionPath(List<Pair<TypeConverter, ResolvedType>> path, Object source, ResolvedType sourceType) {
		Object result = source;
		ResolvedType resultType = sourceType;
		for (var pair : path) {
			result = pair.getKey().doConvert(resultType, result, pair.getValue());
			resultType = pair.getValue();
		}
		return result;
	}
	
	private static OptionalNullable<Object> useClassMapper(ResolvedType sourceType, Object source, ResolvedType targetType) {
		if (sourceType instanceof ResolvedType.SingleClass c)
			return useClassMapperForSource(c.getSingleClass(), source, targetType);
		if (sourceType instanceof ResolvedType.Parameterized p)
			return useClassMapperForSource(p.getBase(), source, targetType);
		return OptionalNullable.empty();
	}
	
	private static OptionalNullable<Object> useClassMapperForSource(Class<?> sourceClass, Object source, ResolvedType targetType) {
		Map<Class<?>, ClassMapper<?, ?>> map = CLASS_MAPPERS.get(sourceClass);
		if (map == null) return OptionalNullable.empty();
		if (targetType instanceof ResolvedType.SingleClass c)
			return useClassMapperForTarget(source, c.getSingleClass(), map);
		if (targetType instanceof ResolvedType.Parameterized p)
			return useClassMapperForTarget(source, p.getBase(), map);
		return OptionalNullable.empty();
	}
	
	private static OptionalNullable<Object> useClassMapperForTarget(Object source, Class<?> targetClass, Map<Class<?>, ClassMapper<?, ?>> map) {
		ClassMapper<?, ?> mapper = map.get(targetClass);
		if (mapper == null) return OptionalNullable.empty();
		return OptionalNullable.of(((ClassMapper) mapper).map(source));
	}
	
	public static void registerClassMapper(ClassMapper<?, ?> mapper) {
		synchronized (CLASS_MAPPERS) {
			CLASS_MAPPERS.computeIfAbsent(mapper.sourceType(), k -> new HashMap<>())
			.put(mapper.targetType(), mapper);
		}
		
	}
	
	public static <S, T> void registerClassMapper(Class<S> sourceType, Class<T> targetType, Function<S, T> mapper) {
		registerClassMapper(new ClassMapper<S, T>() {
			@Override
			public Class<S> sourceType() {
				return sourceType;
			}
			
			@Override
			public Class<T> targetType() {
				return targetType;
			}
			
			@Override
			public T map(S source) {
				return mapper.apply(source);
			}
		});
	}
	
	
	static {
		ServiceLoader.load(ClassMapper.class).forEach(Mappers::registerClassMapper);
		ServiceLoader.load(GenericMapper.class).forEach(GENERIC_MAPPERS::add);
		ServiceLoader.load(TypeConverter.class).forEach(CONVERTERS::add);
	}
}
