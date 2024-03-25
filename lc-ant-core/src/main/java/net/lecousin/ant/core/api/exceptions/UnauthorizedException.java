package net.lecousin.ant.core.api.exceptions;

import net.lecousin.commons.io.text.i18n.TranslatedString;

public class UnauthorizedException extends ApiException {

	private static final long serialVersionUID = 1L;
	
	public static final TranslatedString DEFAULT_MESSAGE = new TranslatedString(I18N_NAMESPACE, "unauthorized");

	public UnauthorizedException() {
		super(DEFAULT_MESSAGE, 401, "unauthorized");
	}
	
}
