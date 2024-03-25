package net.lecousin.ant.core.mapping.impl;

import net.lecousin.ant.core.mapping.GenericMapper;
import net.lecousin.ant.core.reflection.ResolvedType;
import net.lecousin.ant.core.utils.OptionalNullable;

public class EnumStringMapper implements GenericMapper {

	@Override
	public boolean canMap(ResolvedType from, ResolvedType to) {
		if (from instanceof ResolvedType.SingleClass src) {
			if (to instanceof ResolvedType.SingleClass dst) {
				if (Enum.class.isAssignableFrom(src.getSingleClass())) {
					return dst.getSingleClass().isAssignableFrom(String.class);
				}
				if (Enum.class.isAssignableFrom(dst.getSingleClass())) {
					return src.getSingleClass().isAssignableFrom(String.class);
				}
			}
		}
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public OptionalNullable<Object> map(ResolvedType sourceType, Object sourceValue, ResolvedType targetType) {
		if (sourceValue instanceof CharSequence s) {
			if (targetType instanceof ResolvedType.SingleClass c && Enum.class.isAssignableFrom(c.getSingleClass())) {
				try {
					return OptionalNullable.of(Enum.valueOf((Class<? extends Enum>) c.getSingleClass(), s.toString()));
				} catch (IllegalArgumentException e) {
					return OptionalNullable.empty();
				}
			}
		}
		if (sourceValue instanceof Enum e) {
			if (targetType instanceof ResolvedType.SingleClass c && String.class.isAssignableFrom(c.getSingleClass())) {
				return OptionalNullable.of(e.name());
			}
		}
		return OptionalNullable.empty();
	}


}
