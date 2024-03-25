package net.lecousin.ant.core.expression;

import java.io.Serializable;

public interface BinaryOperationExpression<T1 extends Serializable, T2 extends Serializable, R extends Serializable> extends Expression<R> {

	Expression<? extends T1> leftOperand();
	Expression<? extends T2> rightOperand();
	
}
