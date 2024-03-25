package net.lecousin.ant.core.validation.exceptions;

import net.lecousin.commons.io.text.i18n.CompositeI18nString;
import net.lecousin.commons.io.text.i18n.TranslatedString;

public class MissingRequiredPropertyException extends ValidationException {

	private static final long serialVersionUID = 1L;

	public static final TranslatedString MISSING_REQUIRED_PROPERTY_MESSAGE = new TranslatedString(I18N_NAMESPACE, "missing required property");

	public MissingRequiredPropertyException(String propertyPath) {
		super(CompositeI18nString.of(MISSING_REQUIRED_PROPERTY_MESSAGE, ": ", propertyPath), "missing-required-property:" + propertyPath);
	}
	
}
