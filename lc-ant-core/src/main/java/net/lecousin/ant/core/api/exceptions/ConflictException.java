package net.lecousin.ant.core.api.exceptions;

import net.lecousin.commons.io.text.i18n.I18nString;

public class ConflictException extends ApiException {

	private static final long serialVersionUID = 1L;

	public ConflictException(I18nString message) {
		super(message, 409, "conflict");
	}

	public ConflictException(I18nString message, String errorCode) {
		super(message, 409, errorCode);
	}
	
}
