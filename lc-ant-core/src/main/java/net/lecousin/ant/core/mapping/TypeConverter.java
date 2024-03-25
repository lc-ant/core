package net.lecousin.ant.core.mapping;

import java.util.List;

import net.lecousin.ant.core.reflection.ResolvedType;

public interface TypeConverter {

	List<ResolvedType> canConvert(ResolvedType sourceType, ResolvedType finalType);
	
	Object doConvert(ResolvedType sourceType, Object sourceValue, ResolvedType targetType);
	
}
