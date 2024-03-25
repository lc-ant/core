package net.lecousin.ant.core.mapping.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import net.lecousin.ant.core.BasicConstants;
import net.lecousin.ant.core.mapping.TypeConverter;
import net.lecousin.ant.core.reflection.ResolvedType;

public class NumberHandler implements TypeConverter {
	
	private static final List<ResolvedType> TARGETS = Stream.of(
		Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
		AtomicInteger.class, AtomicLong.class,
		BigInteger.class, BigDecimal.class
	).map(ResolvedType.SingleClass::new).map(ResolvedType.class::cast).toList();
	
	@Override
	public List<ResolvedType> canConvert(ResolvedType sourceType, ResolvedType finalType) {
		if (sourceType instanceof ResolvedType.SingleClass c && Number.class.isAssignableFrom(c.getSingleClass())) {
			return TARGETS;
		}
		return Collections.emptyList();
	}
	
	@Override
	public Object doConvert(ResolvedType sourceType, Object sourceValue, ResolvedType targetType) {
		Class<?> targetClass = ((ResolvedType.SingleClass) targetType).getSingleClass();
		if (sourceValue == null) return null;
		Number source = (Number) sourceValue;
		if (Byte.class.equals(targetClass)) return source.byteValue();
		if (Short.class.equals(targetClass)) return source.shortValue();
		if (Integer.class.equals(targetClass)) return source.intValue();
		if (Long.class.equals(targetClass)) return source.longValue();
		if (Float.class.equals(targetClass)) return source.floatValue();
		if (Double.class.equals(targetClass)) return source.doubleValue();
		if (AtomicInteger.class.equals(targetClass)) return new AtomicInteger(source.intValue());
		if (AtomicLong.class.equals(targetClass)) return new AtomicLong(source.longValue());
		if (BigInteger.class.equals(targetClass)) return new BigInteger(source.toString(), BasicConstants.DECIMAL_RADIX);
		if (BigDecimal.class.equals(targetClass)) return new BigDecimal(source.toString());
		return null;
	}
	
}
