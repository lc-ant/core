package net.lecousin.ant.core.validation.exceptions;

import net.lecousin.ant.core.validation.annotations.StringConstraint;
import net.lecousin.commons.io.text.i18n.CompositeI18nString;
import net.lecousin.commons.io.text.i18n.I18nString;
import net.lecousin.commons.io.text.i18n.NumberI18nString;
import net.lecousin.commons.io.text.i18n.TranslatedString;

public class InvalidPropertyException extends ValidationException {

	private static final long serialVersionUID = 1L;

	public InvalidPropertyException(String propertyName, I18nString message) {
		super(CompositeI18nString.of(new TranslatedString(I18N_NAMESPACE, "invalid property {}", propertyName), ": ", message), "invalid-property:" + propertyName);
	}
	
	public static InvalidPropertyException tooSmall(String propertyPath, StringConstraint annotation, String found) {
		return new InvalidPropertyException(propertyPath, CompositeI18nString.of(new TranslatedString(ValidationException.I18N_NAMESPACE, StringConstraint.I18N_KEY_MIN_LENGTH, new NumberI18nString(annotation.minLength())), ": ", found));
	}
	
	public static InvalidPropertyException tooLarge(String propertyPath, StringConstraint annotation, String found) {
		return new InvalidPropertyException(propertyPath, CompositeI18nString.of(new TranslatedString(ValidationException.I18N_NAMESPACE, StringConstraint.I18N_KEY_MAX_LENGTH, new NumberI18nString(annotation.minLength())), ": ", found));
	}

}
