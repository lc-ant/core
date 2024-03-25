package net.lecousin.ant.core.api.exceptions;

import net.lecousin.commons.io.text.i18n.I18nString;

public class ForbiddenException extends ApiException {
	
	private static final long serialVersionUID = 1L;

	public ForbiddenException(I18nString message) {
		super(message, 403, "forbidden");
	}

	public ForbiddenException(I18nString message, String errorCode) {
		super(message, 403, errorCode);
	}

}
