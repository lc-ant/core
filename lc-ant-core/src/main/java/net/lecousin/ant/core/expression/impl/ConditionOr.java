package net.lecousin.ant.core.expression.impl;

import java.util.Arrays;
import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.expression.Expression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConditionOr implements Expression<Boolean> {

	private static final long serialVersionUID = 1L;
	
	private Collection<? extends Expression<Boolean>> or;
	
	@SafeVarargs
	public ConditionOr(Expression<Boolean>... expressions) {
		this(Arrays.asList(expressions));
	}
	
}
