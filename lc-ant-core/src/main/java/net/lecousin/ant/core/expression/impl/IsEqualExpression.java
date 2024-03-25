package net.lecousin.ant.core.expression.impl;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.expression.BinaryOperationExpression;
import net.lecousin.ant.core.expression.Expression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IsEqualExpression<T extends Serializable> implements BinaryOperationExpression<T, T, Boolean> {

	private static final long serialVersionUID = 1L;
	
	private Expression<T> leftIsEqualTo;
	private Expression<T> right;
	
	@Override
	public Expression<T> leftOperand() {
		return leftIsEqualTo;
	}
	
	@Override
	public Expression<T> rightOperand() {
		return right;
	}
	
}
