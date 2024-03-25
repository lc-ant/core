package net.lecousin.ant.core.expression.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.expression.CollectionExpression;
import net.lecousin.ant.core.expression.Expression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionSizeExpression implements Expression<Integer> {

	private static final long serialVersionUID = 1L;
	
	private CollectionExpression<?, ?> sizeOfCollection;
	
}
