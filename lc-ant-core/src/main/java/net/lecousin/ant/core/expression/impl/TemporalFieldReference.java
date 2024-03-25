package net.lecousin.ant.core.expression.impl;

import java.io.Serializable;
import java.time.temporal.Temporal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.lecousin.ant.core.expression.ComparableExpression;
import net.lecousin.ant.core.expression.Expression;
import net.lecousin.ant.core.expression.FieldReferenceExpression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemporalFieldReference<T extends Temporal & Comparable<T> & Serializable> implements ComparableExpression<T, T>, FieldReferenceExpression<T> {

	private static final long serialVersionUID = 1L;
	
	private String temporalField;
	
	@Override
	public String fieldName() {
		return temporalField;
	}
	
	@Data
	@RequiredArgsConstructor
	public static class Nullable<T extends Temporal & Comparable<T> & Serializable> implements ComparableExpression<T, T>, Expression.Nullable<T>, FieldReferenceExpression<T> {
		
		private static final long serialVersionUID = 1L;

		private final String nullableTemporalField;
		
		@Override
		public String fieldName() {
			return nullableTemporalField;
		}
		
	}
}
