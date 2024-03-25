package net.lecousin.ant.core.expression;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import net.lecousin.ant.core.expression.impl.CollectionFieldReference;
import net.lecousin.ant.core.expression.impl.CollectionSizeExpression;
import net.lecousin.ant.core.expression.impl.ConditionAnd;
import net.lecousin.ant.core.expression.impl.ConditionOr;
import net.lecousin.ant.core.expression.impl.EnumFieldReference;
import net.lecousin.ant.core.expression.impl.FieldReference;
import net.lecousin.ant.core.expression.impl.GreaterOrEqualExpression;
import net.lecousin.ant.core.expression.impl.GreaterThanExpression;
import net.lecousin.ant.core.expression.impl.IsEqualExpression;
import net.lecousin.ant.core.expression.impl.IsNotEqualExpression;
import net.lecousin.ant.core.expression.impl.LessOrEqualExpression;
import net.lecousin.ant.core.expression.impl.LessThanExpression;
import net.lecousin.ant.core.expression.impl.NumberFieldReference;
import net.lecousin.ant.core.expression.impl.RegexpMatchExpression;
import net.lecousin.ant.core.expression.impl.StringFieldReference;
import net.lecousin.ant.core.expression.impl.TemporalFieldReference;
import net.lecousin.ant.core.expression.impl.ValueExpression;
import net.lecousin.ant.core.expression.impl.ValueInExpression;
import net.lecousin.ant.core.expression.impl.ValueNotInExpression;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
	@JsonSubTypes.Type(ConditionAnd.class),
	@JsonSubTypes.Type(ConditionOr.class),
	@JsonSubTypes.Type(CollectionSizeExpression.class),
	@JsonSubTypes.Type(FieldReference.class),
	@JsonSubTypes.Type(StringFieldReference.class),
	@JsonSubTypes.Type(NumberFieldReference.class),
	@JsonSubTypes.Type(IsEqualExpression.class),
	@JsonSubTypes.Type(IsNotEqualExpression.class),
	@JsonSubTypes.Type(LessThanExpression.class),
	@JsonSubTypes.Type(LessOrEqualExpression.class),
	@JsonSubTypes.Type(GreaterThanExpression.class),
	@JsonSubTypes.Type(GreaterOrEqualExpression.class),
	@JsonSubTypes.Type(ValueInExpression.class),
	@JsonSubTypes.Type(ValueNotInExpression.class),
	@JsonSubTypes.Type(ValueExpression.class),
	@JsonSubTypes.Type(CollectionFieldReference.class),
	@JsonSubTypes.Type(EnumFieldReference.class),
	@JsonSubTypes.Type(TemporalFieldReference.class),
	@JsonSubTypes.Type(RegexpMatchExpression.class),
})
public interface Expression<T extends Serializable> extends Serializable {

	default IsEqualExpression<T> is(Expression<T> right) {
		return new IsEqualExpression<>(this, right);
	}
	
	default IsEqualExpression<T> is(T value) {
		return is(new ValueExpression<>(value));
	}
	
	default IsNotEqualExpression<T> isNot(Expression<T> right) {
		return new IsNotEqualExpression<>(this, right);
	}
	
	default IsNotEqualExpression<T> isNot(T value) {
		return isNot(new ValueExpression<>(value));
	}
	
	default <C extends Collection<T> & Serializable> ValueInExpression<T, C> in(Expression<C> values) {
		return new ValueInExpression<>(this, values);
	}
	
	@SuppressWarnings("unchecked")
	default <C extends Collection<T>, CS extends Collection<T> & Serializable> ValueInExpression<T, CS> in(C values) {
		CS values2;
		if (values instanceof Serializable v) values2 = (CS) v;
		else values2 = (CS) new LinkedList<>(values);
		return in(new ValueExpression<CS>(values2));
	}
	
	default <C extends Collection<T> & Serializable> ValueNotInExpression<T, C> notIn(Expression<C> values) {
		return new ValueNotInExpression<>(this, values);
	}
	
	@SuppressWarnings("unchecked")
	default <C extends Collection<T>, CS extends Collection<T> & Serializable> ValueNotInExpression<T, CS> notIn(C values) {
		CS values2;
		if (values instanceof Serializable v) values2 = (CS) v;
		else values2 = (CS) new LinkedList<>(values);
		return notIn(new ValueExpression<CS>(values2));
	}
	
	interface Nullable<T extends Serializable> extends Expression<T> {
		
		default IsEqualExpression<T> isNull() {
			return is(new ValueExpression<>(null));
		}
		
		default IsNotEqualExpression<T> isNotNull() {
			return isNot(new ValueExpression<>(null));
		}
		
		default IsEqualExpression<T> is(Optional<T> optional) {
			return is(optional.orElse(null));
		}
		
		default IsNotEqualExpression<T> isNot(Optional<T> optional) {
			return isNot(optional.orElse(null));
		}
		
	}
	
}
