package net.lecousin.ant.core.mapping.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.lecousin.ant.core.mapping.GenericMapper;
import net.lecousin.ant.core.mapping.Mappers;
import net.lecousin.ant.core.reflection.ClassProperty;
import net.lecousin.ant.core.reflection.ReflectionException;
import net.lecousin.ant.core.reflection.ReflectionUtils;
import net.lecousin.ant.core.reflection.ResolvedType;
import net.lecousin.ant.core.reflection.TypeResolver;
import net.lecousin.ant.core.utils.OptionalNullable;

public class MapHandler implements GenericMapper {

	@Override
	public boolean canMap(ResolvedType from, ResolvedType to) {
		if (from instanceof ResolvedType.SingleClass c && Map.class.isAssignableFrom(c.getSingleClass()))
			return true;
		if (from instanceof ResolvedType.Parameterized p && Map.class.isAssignableFrom(p.getBase()))
			return true;
		if (to instanceof ResolvedType.SingleClass c && Map.class.isAssignableFrom(c.getSingleClass()))
			return true;
		if (to instanceof ResolvedType.Parameterized p && Map.class.isAssignableFrom(p.getBase()))
			return true;
		return false;
	}

	@Override
	public OptionalNullable<Object> map(ResolvedType sourceType, Object sourceValue, ResolvedType targetType) {
		ResolvedType sourceMap = TypeResolver.getInheritedType(sourceType, Map.class);
		if (sourceMap instanceof ResolvedType.SingleClass c && Map.class.isAssignableFrom(c.getSingleClass()))
			return mapFromMap((Map) sourceValue, new ResolvedType.SingleClass(Object.class), new ResolvedType.SingleClass(Object.class), targetType);
		if (sourceMap instanceof ResolvedType.Parameterized p && Map.class.isAssignableFrom(p.getBase())) {
			if (p.getParameters().length == 2)
				return mapFromMap((Map) sourceValue, p.getParameters()[0], p.getParameters()[1], targetType);
			return mapFromMap((Map) sourceValue, new ResolvedType.SingleClass(Object.class), new ResolvedType.SingleClass(Object.class), targetType);
		}
		ResolvedType targetMap = TypeResolver.getInheritedType(targetType, Map.class);
		if (targetMap instanceof ResolvedType.SingleClass c && Map.class.isAssignableFrom(c.getSingleClass()))
			return mapObjectToMap(sourceType, sourceValue, c.getSingleClass(), new ResolvedType.SingleClass(Object.class));
		if (targetMap instanceof ResolvedType.Parameterized p && Map.class.isAssignableFrom(p.getBase()))
			return mapObjectToMap(sourceType, sourceValue, p.getBase(), p.getParameters().length == 2 ? p.getParameters()[1] : new ResolvedType.SingleClass(Object.class));
		return OptionalNullable.empty();
	}
	
	private OptionalNullable<Object> mapFromMap(Map source, ResolvedType keyType, ResolvedType valueType, ResolvedType targetType) {
		ResolvedType targetMap = TypeResolver.getInheritedType(targetType, Map.class);
		if (targetMap instanceof ResolvedType.SingleClass c && Map.class.isAssignableFrom(c.getSingleClass()))
			return mapFromMapToMap(source, keyType, valueType, c.getSingleClass(), new ResolvedType.SingleClass(Object.class), new ResolvedType.SingleClass(Object.class));
		if (targetMap instanceof ResolvedType.Parameterized p && Map.class.isAssignableFrom(p.getBase())) {
			if (p.getParameters().length == 2)
				return mapFromMapToMap(source, keyType, valueType, p.getBase(), p.getParameters()[0], p.getParameters()[1]);
			return mapFromMapToMap(source, keyType, valueType, p.getBase(), new ResolvedType.SingleClass(Object.class), new ResolvedType.SingleClass(Object.class));
		}
		return mapFromMapToObject(source, valueType, targetType);
	}
	
	private OptionalNullable<Object> mapFromMapToMap(
		Map<?, ?> source, ResolvedType sourceKeyType, ResolvedType sourceValueType,
		Class<?> mapClass, ResolvedType targetKeyType, ResolvedType targetValueType) {
		Map target = instantiateMap(mapClass, targetValueType);
		for (var src : source.entrySet())
			target.put(Mappers.map(sourceKeyType, src.getKey(), targetKeyType), Mappers.map(sourceValueType, src.getValue(), targetValueType));
		return OptionalNullable.of(target);
	}
	
	private OptionalNullable<Object> mapFromMapToObject(Map<?, ?> source, ResolvedType sourceValueType, ResolvedType targetType) {
		Class<?> to;
		if (targetType instanceof ResolvedType.SingleClass c) {
			to = c.getSingleClass();
		} else if (targetType instanceof ResolvedType.Parameterized p) {
			to = p.getBase();
		} else
			return OptionalNullable.empty();
		
		Object target;
		try {
			target = to.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			return OptionalNullable.empty();
		}
		
		Map<String, ClassProperty> properties = ReflectionUtils.getAllProperties(targetType);
		for (var entry : source.entrySet()) {
			var property = properties.get(Objects.toString(entry.getKey()));
			if (property != null && property.canSet())
				property.setValue(target, Mappers.map(sourceValueType, entry.getValue(), property.getType()));
			else
				throw new ReflectionException("Cannot map entry key " + entry.getKey() + " to a property of " + to.getName());
		}
		return OptionalNullable.of(target);
	}
	
	private OptionalNullable<Object> mapObjectToMap(ResolvedType sourceType, Object sourceValue, Class<?> mapClass, ResolvedType valueType) {
		Map target = instantiateMap(mapClass, valueType);
		Map<String, ClassProperty> properties = ReflectionUtils.getAllProperties(sourceType);
		for (var property : properties.values()) {
			if (!property.canGet()) continue;
			target.put(property.getName(), Mappers.map(property.getType(), property.getValue(sourceValue), valueType));
		}
		return OptionalNullable.of(target);
	}
	
	private Map instantiateMap(Class<?> mapClass, ResolvedType valueType) {
		if (mapClass.isAssignableFrom(HashMap.class))
			return new HashMap();
		throw new ReflectionException("Cannot instantiate Map of type " + mapClass.getName());
	}

}
