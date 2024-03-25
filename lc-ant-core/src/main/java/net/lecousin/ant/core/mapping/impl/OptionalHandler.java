package net.lecousin.ant.core.mapping.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import net.lecousin.ant.core.mapping.TypeConverter;
import net.lecousin.ant.core.reflection.ResolvedType;

public class OptionalHandler implements TypeConverter {

	@Override
	public List<ResolvedType> canConvert(ResolvedType sourceType, ResolvedType finalType) {
		List<ResolvedType> result = new LinkedList<>();
		// can convert into an optional of source type
		int optionalLevels = countOptionalLevels(finalType);
		if (optionalLevels > 0) {
			int sourceLevels = countOptionalLevels(sourceType);
			if (optionalLevels > sourceLevels) {
				if (finalType instanceof ResolvedType.SingleClass c && c.getSingleClass().equals(Optional.class))
					result.add(finalType);
				else if (finalType instanceof ResolvedType.Parameterized p && p.getParameters().length == 1 && p.getBase().equals(Optional.class))
					result.add(new ResolvedType.Parameterized(Optional.class, new ResolvedType[] { sourceType }));
			}
		}
		// if source type is optional, can convert into a non-optional
		if (sourceType instanceof ResolvedType.Parameterized p && p.getParameters().length == 1 && p.getBase().equals(Optional.class))
			result.add(p.getParameters()[0]);
		return result;
	}
	
	private int countOptionalLevels(ResolvedType type) {
		if (type instanceof ResolvedType.SingleClass c && c.getSingleClass().equals(Optional.class))
			return 1;
		if (type instanceof ResolvedType.Parameterized p && p.getParameters().length == 1 && p.getBase().equals(Optional.class))
			return countOptionalLevels(p.getParameters()[0]) + 1;
		return 0;
	}
	
	@Override
	public Object doConvert(ResolvedType sourceType, Object sourceValue, ResolvedType targetType) {
		if (targetType instanceof ResolvedType.Parameterized p &&
			p.getParameters().length == 1 &&
			p.getBase().equals(Optional.class) &&
			p.getParameters()[0].equals(sourceType)) {
			// wrap
			return Optional.ofNullable(sourceValue);
		}
		if (targetType instanceof ResolvedType.SingleClass c && c.getSingleClass().equals(Optional.class))
			return Optional.ofNullable(sourceValue);
		// unwrap
		return sourceValue != null ? ((Optional) sourceValue).orElse(null) : null;
	}
	
}
