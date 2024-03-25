package net.lecousin.ant.core.expression;

import java.io.Serializable;
import java.util.Collection;

import net.lecousin.ant.core.expression.impl.CollectionSizeExpression;

public interface CollectionExpression<E extends Serializable, T extends Collection<E> & Serializable> extends Expression<T> {

	default CollectionSizeExpression size() {
		return new CollectionSizeExpression(this);
	}
	
}
