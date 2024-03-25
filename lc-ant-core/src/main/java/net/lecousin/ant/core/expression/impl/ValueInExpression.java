package net.lecousin.ant.core.expression.impl;

import java.io.Serializable;
import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.expression.BinaryOperationExpression;
import net.lecousin.ant.core.expression.Expression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValueInExpression<T extends Serializable, C extends Collection<T> & Serializable> implements BinaryOperationExpression<T, C, Boolean> {

	private static final long serialVersionUID = 1L;
	
	private Expression<T> valueIn;
	private Expression<C> inValues;
	
	@Override
	public Expression<T> leftOperand() {
		return valueIn;
	}
	
	@Override
	public Expression<C> rightOperand() {
		return inValues;
	}
	
}
