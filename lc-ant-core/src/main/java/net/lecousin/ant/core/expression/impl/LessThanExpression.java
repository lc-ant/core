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
public class LessThanExpression<T1 extends Serializable, T2 extends Serializable> implements BinaryOperationExpression<T1, T2, Boolean> {

	private static final long serialVersionUID = 1L;
	
	private Expression<T1> leftIsLessThan;
	private Expression<? extends T2> right;
	
	@Override
	public Expression<T1> leftOperand() {
		return leftIsLessThan;
	}
	
	@Override
	public Expression<? extends T2> rightOperand() {
		return right;
	}
}
