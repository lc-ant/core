package net.lecousin.ant.core.expression.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.lecousin.ant.core.expression.ComparableExpression;
import net.lecousin.ant.core.expression.Expression;
import net.lecousin.ant.core.expression.FieldReferenceExpression;
import net.lecousin.ant.core.expression.StringExpression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StringFieldReference implements ComparableExpression<String, String>, FieldReferenceExpression<String>, StringExpression {

	private static final long serialVersionUID = 1L;

	private String stringField;
	
	@Override
	public String fieldName() {
		return stringField;
	}
	
	@Data
	@RequiredArgsConstructor
	public static class Nullable implements ComparableExpression<String, String>, Expression.Nullable<String>, FieldReferenceExpression<String>, StringExpression {
		
		private static final long serialVersionUID = 1L;

		private final String nullableStringField;
		
		@Override
		public String fieldName() {
			return nullableStringField;
		}
		
	}
}
