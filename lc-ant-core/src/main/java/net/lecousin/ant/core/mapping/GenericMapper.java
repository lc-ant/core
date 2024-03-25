package net.lecousin.ant.core.mapping;

import net.lecousin.ant.core.reflection.ResolvedType;
import net.lecousin.ant.core.utils.OptionalNullable;

public interface GenericMapper {

	boolean canMap(ResolvedType from, ResolvedType to);
	
	OptionalNullable<Object> map(ResolvedType sourceType, Object sourceValue, ResolvedType targetType);
	
}
