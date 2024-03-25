package net.lecousin.ant.core.expression.impl;

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
public class NumberFieldReference<T extends Number> implements ComparableExpression<T, Number>, FieldReferenceExpression<T> {

	private static final long serialVersionUID = 1L;
	
	private String numberField;
	
	@Override
	public String fieldName() {
		return numberField;
	}
	
	@Data
	@RequiredArgsConstructor
	public static class Nullable<T extends Number> implements ComparableExpression<T, Number>, Expression.Nullable<T>, FieldReferenceExpression<T> {
		
		private static final long serialVersionUID = 1L;

		private final String nullableNumberField;
		
		@Override
		public String fieldName() {
			return nullableNumberField;
		}
		
	}
}
