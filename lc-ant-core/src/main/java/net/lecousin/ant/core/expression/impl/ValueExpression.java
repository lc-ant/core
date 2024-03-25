package net.lecousin.ant.core.expression.impl;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.expression.Expression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValueExpression<T extends Serializable> implements Expression<T> {

	private static final long serialVersionUID = 1L;
	
	private T value;
	
}
