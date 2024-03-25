package net.lecousin.ant.core.expression.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.lecousin.ant.core.expression.Expression;
import net.lecousin.ant.core.expression.FieldReferenceExpression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnumFieldReference<T extends Enum<T>> implements Expression<T>, FieldReferenceExpression<T> {

	private static final long serialVersionUID = 1L;
	
	private String enumField;
	
	@Override
	public String fieldName() {
		return enumField;
	}
	
	
	@Data
	@RequiredArgsConstructor
	public static class Nullable<T extends Enum<T>> implements Expression.Nullable<T>, FieldReferenceExpression<T> {
		
		private static final long serialVersionUID = 1L;
		
		private final String nullableEnumField;
		
		@Override
		public String fieldName() {
			return nullableEnumField;
		}
		
	}
	
}
