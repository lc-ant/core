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
public class ValueNotInExpression<T extends Serializable, C extends Collection<T> & Serializable> implements BinaryOperationExpression<T, C, Boolean> {

	private static final long serialVersionUID = 1L;
	
	private Expression<T> valueNotIn;
	private Expression<C> notInValues;
	
	@Override
	public Expression<? extends T> leftOperand() {
		return valueNotIn;
	}
	
	@Override
	public Expression<? extends C> rightOperand() {
		return notInValues;
	}
	
}
