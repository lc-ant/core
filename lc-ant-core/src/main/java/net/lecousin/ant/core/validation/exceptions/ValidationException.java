package net.lecousin.ant.core.validation.exceptions;

import net.lecousin.ant.core.api.exceptions.BadRequestException;
import net.lecousin.commons.io.text.i18n.I18nString;

public class ValidationException extends BadRequestException {

	private static final long serialVersionUID = 1L;
	
	public static final String I18N_NAMESPACE = "validation";

	public ValidationException(I18nString message, String errorCode) {
		super(message, errorCode);
	}
	
}
