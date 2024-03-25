package net.lecousin.ant.core.mapping.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import net.lecousin.ant.core.mapping.TypeConverter;
import net.lecousin.ant.core.reflection.ResolvedType;

public class TemporalConverter implements TypeConverter {

	private static final List<ResolvedType> TARGETS_FROM_INSTANT = Stream.of(
		Long.class,
		Date.class,
		LocalDate.class,
		LocalTime.class,
		LocalDateTime.class,
		OffsetDateTime.class
	).map(ResolvedType.SingleClass::new).map(ResolvedType.class::cast).toList();
	
	private static final List<ResolvedType> TARGETS_FROM_DATE = Stream.of(
		Long.class,
		Instant.class
	).map(ResolvedType.SingleClass::new).map(ResolvedType.class::cast).toList();
		
	private static final List<ResolvedType> TARGETS_FROM_LONG = Stream.of(
		Instant.class,
		Date.class
	).map(ResolvedType.SingleClass::new).map(ResolvedType.class::cast).toList();
	
	private static final List<ResolvedType> TARGETS_FROM_STRING = Stream.of(
		LocalDate.class,
		LocalTime.class,
		LocalDateTime.class
	).map(ResolvedType.SingleClass::new).map(ResolvedType.class::cast).toList();
	
	private static final List<ResolvedType> TARGETS_FROM_OFFSET_DATE_TIME = Stream.of(
		Instant.class
	).map(ResolvedType.SingleClass::new).map(ResolvedType.class::cast).toList();
	
	@Override
	public List<ResolvedType> canConvert(ResolvedType sourceType, ResolvedType finalType) {
		if (sourceType instanceof ResolvedType.SingleClass c) {
			Class<?> clazz = c.getSingleClass();
			if (Instant.class.equals(clazz)) return TARGETS_FROM_INSTANT;
			if (Date.class.equals(clazz)) return TARGETS_FROM_DATE;
			if (Long.class.equals(clazz) || long.class.equals(clazz)) return TARGETS_FROM_LONG;
			if (String.class.equals(clazz)) return TARGETS_FROM_STRING;
			if (OffsetDateTime.class.equals(clazz)) return TARGETS_FROM_OFFSET_DATE_TIME;
		}
		return Collections.emptyList();
	}
	
	@Override
	public Object doConvert(ResolvedType sourceType, Object sourceValue, ResolvedType targetType) {
		if (sourceValue == null) return null;
		Class<?> to = ((ResolvedType.SingleClass) targetType).getSingleClass();
		if (sourceValue instanceof Instant instant) {
			if (Long.class.equals(to)) return instant.toEpochMilli();
			if (Date.class.equals(to)) return new java.util.Date(instant.toEpochMilli());
			if (LocalDate.class.equals(to)) return LocalDate.ofInstant(instant, ZoneId.systemDefault());
			if (LocalTime.class.equals(to)) return LocalTime.ofInstant(instant, ZoneId.systemDefault());
			if (LocalDateTime.class.equals(to)) return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
			if (OffsetDateTime.class.equals(to)) return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
		} else if (sourceValue instanceof Date date) {
			if (Long.class.equals(to)) return date.getTime();
			if (Instant.class.equals(to)) return Instant.ofEpochMilli(date.getTime());
		} else if (sourceValue instanceof Long l) {
			if (Instant.class.equals(to)) return Instant.ofEpochMilli(l);
			if (Date.class.equals(to)) return new Date(l);
		} else if (sourceValue instanceof String s) {
			if (LocalDate.class.equals(to)) return LocalDate.parse(s);
			if (LocalTime.class.equals(to)) return LocalTime.parse(s);
			if (LocalDateTime.class.equals(to)) return LocalDateTime.parse(s);
		} else if (sourceValue instanceof OffsetDateTime odt) {
			if (Instant.class.equals(to)) return odt.toInstant();
		}
		return null;
	}
	
}
