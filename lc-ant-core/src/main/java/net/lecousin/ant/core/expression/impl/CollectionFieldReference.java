package net.lecousin.ant.core.expression.impl;

import java.io.Serializable;
import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.expression.CollectionExpression;
import net.lecousin.ant.core.expression.FieldReferenceExpression;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionFieldReference<E extends Serializable, T extends Collection<E> & Serializable> implements CollectionExpression<E, T>, FieldReferenceExpression<T> {

	private static final long serialVersionUID = 1L;
	
	private String collectionField;
	
	@Override
	public String fieldName() {
		return collectionField;
	}

}
