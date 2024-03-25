package net.lecousin.ant.core.mapping.impl;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import net.lecousin.ant.core.mapping.TypeConverter;
import net.lecousin.ant.core.reflection.ResolvedType;

public class UUIDConverter implements TypeConverter {

	private static final List<ResolvedType> TARGETS_FROM_UUID = Stream.of(
		String.class
	).map(ResolvedType.SingleClass::new).map(ResolvedType.class::cast).toList();
	
	private static final List<ResolvedType> TARGETS_FROM_STRING = Stream.of(
		UUID.class
	).map(ResolvedType.SingleClass::new).map(ResolvedType.class::cast).toList();
	
	@Override
	public List<ResolvedType> canConvert(ResolvedType sourceType, ResolvedType finalType) {
		if (sourceType instanceof ResolvedType.SingleClass c) {
			Class<?> clazz = c.getSingleClass();
			if (String.class.isAssignableFrom(clazz)) return TARGETS_FROM_STRING;
			if (UUID.class.isAssignableFrom(clazz)) return TARGETS_FROM_UUID;
		}
		return Collections.emptyList();
	}
	
	@Override
	public Object doConvert(ResolvedType sourceType, Object sourceValue, ResolvedType targetType) {
		if (sourceValue == null) return null;
		if (sourceValue instanceof String s) return UUID.fromString(s);
		if (sourceValue instanceof UUID u) return u.toString();
		return null;
	}
	
}
