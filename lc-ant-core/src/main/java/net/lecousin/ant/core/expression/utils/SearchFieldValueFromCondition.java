package net.lecousin.ant.core.expression.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.expression.BinaryOperationExpression;
import net.lecousin.ant.core.expression.Expression;
import net.lecousin.ant.core.expression.FieldReferenceExpression;
import net.lecousin.ant.core.expression.impl.ConditionAnd;
import net.lecousin.ant.core.expression.impl.ValueExpression;
import net.lecousin.ant.core.utils.OptionalNullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SearchFieldValueFromCondition {

	public static OptionalNullable<Object> searchFieldValue(String fieldName, Expression<Boolean> condition) {
		if (condition instanceof ConditionAnd and) {
			for (var e : and.getAnd()) {
				var r = searchFieldValue(fieldName, e);
				if (r.isPresent()) return r;
			}
			return OptionalNullable.empty();
		}
		if (condition instanceof BinaryOperationExpression e) {
			if (e.leftOperand() instanceof FieldReferenceExpression fe && fe.fieldName().equals(fieldName)) {
				if (e.rightOperand() instanceof ValueExpression ve) return OptionalNullable.of(ve.getValue());
			}
		}
		return OptionalNullable.empty();
	}
	
}
