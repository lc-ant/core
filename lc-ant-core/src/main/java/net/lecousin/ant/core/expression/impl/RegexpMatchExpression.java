package net.lecousin.ant.core.expression.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.expression.BinaryOperationExpression;
import net.lecousin.ant.core.expression.Expression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegexpMatchExpression implements BinaryOperationExpression<String, String, Boolean> {

	private static final long serialVersionUID = 1L;
	
	private Expression<String> toMatch;
	private Expression<String> regexp;
	
	@Override
	public Expression<String> leftOperand() {
		return toMatch;
	}
	
	@Override
	public Expression<String> rightOperand() {
		return regexp;
	}
	
}
