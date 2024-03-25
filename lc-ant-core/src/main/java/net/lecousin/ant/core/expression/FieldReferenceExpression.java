package net.lecousin.ant.core.expression;

import java.io.Serializable;

public interface FieldReferenceExpression<T extends Serializable> extends Expression<T> {

	String fieldName();
	
}
