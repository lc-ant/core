package net.lecousin.ant.core.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;

import net.lecousin.ant.core.reflection.ClassProperty;
import net.lecousin.ant.core.reflection.ObjectPropertiesCache;
import net.lecousin.ant.core.reflection.ReflectionUtils;
import net.lecousin.ant.core.reflection.ResolvedType;
import net.lecousin.ant.core.reflection.TypeResolver;
import net.lecousin.ant.core.validation.annotations.EqualToField;
import net.lecousin.ant.core.validation.annotations.GreaterOrEqualToField;
import net.lecousin.ant.core.validation.annotations.GreaterThanField;
import net.lecousin.ant.core.validation.annotations.Ignore;
import net.lecousin.ant.core.validation.annotations.LessOrEqualToField;
import net.lecousin.ant.core.validation.annotations.LessThanField;
import net.lecousin.ant.core.validation.annotations.Mandatory;
import net.lecousin.ant.core.validation.annotations.NotEqualToField;
import net.lecousin.ant.core.validation.annotations.StringConstraint;
import net.lecousin.ant.core.validation.exceptions.InvalidPropertyException;
import net.lecousin.ant.core.validation.exceptions.MissingRequiredPropertyException;
import net.lecousin.ant.core.validation.exceptions.ValidationException;
import net.lecousin.commons.io.text.i18n.TranslatedString;

public class Validator<T> {
	
	private Map<ValidationContext, List<ValidatorChain>> validatorsByContexts = new HashMap<>();

	public Validator(Class<T> type) {
		var properties = ReflectionUtils.getAllProperties(type);
		for (var context : ValidationContext.values()) {
			List<ValidatorChain> list = new LinkedList<>();
			validatorsByContexts.put(context, list);
			for (var property : properties.values()) {
				List<Annotation> annotations = filterAnnotationsForContext(property.getAnnotations(), context);
				if (annotations.isEmpty()) continue;
				list.add(createValidator("", property, properties, annotations));
			}
		}
	}
	
	
	public void validate(T object, ValidationContext context) throws ValidationException {
		ObjectPropertiesCache cache = new ObjectPropertiesCache();
		for (ValidatorChain chain : validatorsByContexts.get(context))
			chain.validate(object, cache);
	}

	private List<Annotation> filterAnnotationsForContext(List<Annotation> annotations, ValidationContext context) {
		return annotations.stream().filter(a -> {
			ValidationContext[] contexts = getContexts(a);
			if (contexts == null) return false;
			if (contexts.length == 0) return true;
			return ArrayUtils.contains(contexts, context);
		}).toList();
	}
	
	private ValidationContext[] getContexts(Annotation a) {
		try {
			Method m = a.annotationType().getMethod("context");
			if (m.getReturnType().equals(ValidationContext[].class)) return (ValidationContext[]) m.invoke(a);
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	private <A extends Annotation> A getAnnotation(Class<A> annotationType, List<Annotation> annotations) {
		return (A) annotations.stream().filter(a -> annotationType.isAssignableFrom(a.getClass())).findAny().orElse(null);
	}
	
	private ValidatorChain createValidator(String containerPath, ClassProperty property, Map<String, ClassProperty> properties, List<Annotation> annotations) {
		ValidatorChain chain = new ValidatorChain();
		Ignore ignore = getAnnotation(Ignore.class, annotations);
		if (ignore != null && ignore.value())
			return chain;
		Mandatory mandatory = getAnnotation(Mandatory.class, annotations);
		if (mandatory != null)
			if (mandatory.value()) {
				chain.add((obj, cache) -> {
					if (cache.get(obj, property) == null)
						throw new MissingRequiredPropertyException(containerPath + property.getName());
					return true;
				});
			} else {
				chain.add((obj, cache) -> cache.get(obj, property) != null);
			}
		handleStringConstraints(getAnnotation(StringConstraint.class, annotations), chain, containerPath, property);
		handleFieldsComparisons(
				getAnnotation(EqualToField.class, annotations),
				getAnnotation(NotEqualToField.class, annotations),
				getAnnotation(GreaterThanField.class, annotations),
				getAnnotation(GreaterOrEqualToField.class, annotations),
				getAnnotation(LessThanField.class, annotations),
				getAnnotation(LessOrEqualToField.class, annotations),
				chain, containerPath, property, properties
		);
		// TODO handle properties of nested objects
		return chain;
	}
	
	private void handleStringConstraints(StringConstraint a, ValidatorChain chain, String containerPath, ClassProperty property) {
		if (a == null) return;
		var opt = ResolvedType.getRawClass(property.getType());
		if (opt.isEmpty() || !CharSequence.class.isAssignableFrom(opt.get())) throw new RuntimeException("Cannot apply StringConstraint on type " + property.getType());
		chain.add((obj, cache) -> {
			CharSequence s = (CharSequence) cache.get(obj, property);
			if (s.length() < a.minLength()) throw InvalidPropertyException.tooSmall(containerPath + property.getName(), a, s.toString());
			if (a.maxLength() > 0 && s.length() > a.maxLength()) throw InvalidPropertyException.tooLarge(containerPath + property.getName(), a, s.toString());
			return true;
		});
	}
	
	@SuppressWarnings("rawtypes")
	private void handleFieldsComparisons(
		EqualToField equalTo, NotEqualToField notEqualTo,
		GreaterThanField greaterThan, GreaterOrEqualToField greaterOrEqualTo,
		LessThanField lessThan, LessOrEqualToField lessOrEqualTo,
		ValidatorChain chain, String containerPath, ClassProperty property, Map<String, ClassProperty> properties
	) {
		Set<String> fields = new HashSet<>();
		if (equalTo != null) fields.add(equalTo.value());
		if (notEqualTo != null) fields.add(notEqualTo.value());
		if (greaterThan != null) fields.add(greaterThan.value());
		if (greaterOrEqualTo != null) fields.add(greaterOrEqualTo.value());
		if (lessThan != null) fields.add(lessThan.value());
		if (lessOrEqualTo != null) fields.add(lessOrEqualTo.value());
		if (fields.isEmpty()) return;
		var opt = ResolvedType.getRawClass(property.getType());
		if (opt.isEmpty() || !Comparable.class.isAssignableFrom(opt.get())) throw new RuntimeException("Cannot apply a comparison validation on type " + property.getType());
		ResolvedType comparableType = ((ResolvedType.Parameterized) TypeResolver.getInheritedType(property.getType(), Comparable.class)).getParameters()[0];
		// TODO handle field path
		for (String field : fields) {
			ClassProperty p2 = properties.get(field);
			if (p2 == null) throw new RuntimeException("Unknown property " + containerPath + field);
			opt = ResolvedType.getRawClass(p2.getType());
			if (opt.isEmpty() || !Comparable.class.isAssignableFrom(opt.get())) throw new RuntimeException("Property " + containerPath + property.getName() + " cannot be compared with " + containerPath + field);
			ResolvedType comparableType2 = ((ResolvedType.Parameterized) TypeResolver.getInheritedType(p2.getType(), Comparable.class)).getParameters()[0];
			if (!comparableType2.equals(comparableType)) throw new RuntimeException("Property " + containerPath + property.getName() + " cannot be compared with " + containerPath + field);
		}
		chain.add((obj, cache) -> {
			if (equalTo != null) checkComparable((Comparable) cache.get(obj, property), (Comparable) cache.get(obj, properties.get(equalTo.value())), containerPath, property.getName(), equalTo.value(), "=",  c -> c == 0);
			if (notEqualTo != null) checkComparable((Comparable) cache.get(obj, property), (Comparable) cache.get(obj, properties.get(notEqualTo.value())), containerPath, property.getName(), notEqualTo.value(), "!=",  c -> c != 0);
			if (greaterThan != null) checkComparable((Comparable) cache.get(obj, property), (Comparable) cache.get(obj, properties.get(greaterThan.value())), containerPath, property.getName(), greaterThan.value(), ">",  c -> c > 0);
			if (greaterOrEqualTo != null) checkComparable((Comparable) cache.get(obj, property), (Comparable) cache.get(obj, properties.get(greaterOrEqualTo.value())), containerPath, property.getName(), greaterOrEqualTo.value(), ">=",  c -> c >= 0);
			if (lessThan != null) checkComparable((Comparable) cache.get(obj, property), (Comparable) cache.get(obj, properties.get(lessThan.value())), containerPath, property.getName(), lessThan.value(), "<",  c -> c < 0);
			if (lessOrEqualTo != null) checkComparable((Comparable) cache.get(obj, property), (Comparable) cache.get(obj, properties.get(lessOrEqualTo.value())), containerPath, property.getName(), lessOrEqualTo.value(), "<=",  c -> c <= 0);
			return true;
		});
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void checkComparable(Comparable value1, Comparable value2, String containerPath, String p1, String p2, String cmp, Function<Integer, Boolean> checker) throws InvalidPropertyException {
		if (value1 == null || value2 == null) return;
		int c = value1.compareTo(value2);
		if (!checker.apply(c))
			throw new InvalidPropertyException(containerPath + p1, new TranslatedString(ValidationException.I18N_NAMESPACE, "must be {} compared to {}", cmp, containerPath + p2));
	}

}
