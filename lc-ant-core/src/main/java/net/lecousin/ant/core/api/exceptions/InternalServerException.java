package net.lecousin.ant.core.api.exceptions;

import net.lecousin.commons.io.text.i18n.I18nString;
import net.lecousin.commons.io.text.i18n.StaticI18nString;

public class InternalServerException extends ApiException {

	private static final long serialVersionUID = 1L;

	public InternalServerException(I18nString message) {
		super(message, 500, "internal-error");
	}
	
	public InternalServerException(String message) {
		this(new StaticI18nString(message));
	}
	
	public InternalServerException(Throwable cause) {
		super(new StaticI18nString(cause.getMessage()), 500, "internal-error", cause);
	}
	
}
