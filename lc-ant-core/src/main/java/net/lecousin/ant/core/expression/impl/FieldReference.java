package net.lecousin.ant.core.expression.impl;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.lecousin.ant.core.expression.Expression;
import net.lecousin.ant.core.expression.FieldReferenceExpression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldReference<T extends Serializable> implements Expression<T>, FieldReferenceExpression<T> {

	private static final long serialVersionUID = 1L;
	
	private String field;
	
	@Override
	public String fieldName() {
		return field;
	}
	
	@Data
	@RequiredArgsConstructor
	public static class Nullable<T extends Serializable> implements Expression.Nullable<T>, FieldReferenceExpression<T> {
		
		private static final long serialVersionUID = 1L;

		private final String nullableField;
		
		@Override
		public String fieldName() {
			return nullableField;
		}
		
	}

}
