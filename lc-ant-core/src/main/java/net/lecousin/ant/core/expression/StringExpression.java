package net.lecousin.ant.core.expression;

import net.lecousin.ant.core.expression.impl.RegexpMatchExpression;
import net.lecousin.ant.core.expression.impl.ValueExpression;

public interface StringExpression extends Expression<String> {

	default RegexpMatchExpression matchesRegexp(Expression<String> regexp) {
		return new RegexpMatchExpression(this, regexp);
	}

	default RegexpMatchExpression matchesRegexp(String regexp) {
		return new RegexpMatchExpression(this, new ValueExpression<>(regexp));
	}
	
}
