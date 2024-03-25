package net.lecousin.ant.core.expression;

import java.io.Serializable;

import net.lecousin.ant.core.expression.impl.GreaterOrEqualExpression;
import net.lecousin.ant.core.expression.impl.GreaterThanExpression;
import net.lecousin.ant.core.expression.impl.LessOrEqualExpression;
import net.lecousin.ant.core.expression.impl.LessThanExpression;
import net.lecousin.ant.core.expression.impl.ValueExpression;

public interface ComparableExpression<T extends Serializable, C extends Serializable> extends Expression<T> {

	default LessThanExpression<T, C> lessThan(Expression<? extends C> right) {
		return new LessThanExpression<>(this, right);
	}
	
	default LessThanExpression<T, C> lessThan(C value) {
		return lessThan(new ValueExpression<>(value));
	}

	default LessOrEqualExpression<T, C> lessOrEqualTo(Expression<? extends C> right) {
		return new LessOrEqualExpression<>(this, right);
	}
	
	default LessOrEqualExpression<T, C> lessOrEqualTo(C value) {
		return lessOrEqualTo(new ValueExpression<>(value));
	}

	default GreaterThanExpression<T, C> greaterThan(Expression<? extends C> right) {
		return new GreaterThanExpression<>(this, right);
	}
	
	default GreaterThanExpression<T, C> greaterThan(C value) {
		return greaterThan(new ValueExpression<>(value));
	}

	default GreaterOrEqualExpression<T, C> greaterOrEqualTo(Expression<? extends C> right) {
		return new GreaterOrEqualExpression<>(this, right);
	}
	
	default GreaterOrEqualExpression<T, C> greaterOrEqualTo(C value) {
		return greaterOrEqualTo(new ValueExpression<>(value));
	}
	
}
