package net.lecousin.ant.core.api.exceptions;

import net.lecousin.commons.io.text.i18n.I18nString;

public class BadRequestException extends ApiException {

	private static final long serialVersionUID = 1L;

	public BadRequestException(I18nString message, String errorCode) {
		super(message, 400, errorCode);
	}
	
}
