package net.lecousin.ant.core.api.exceptions;

import net.lecousin.commons.io.text.i18n.I18nString;

public class NotFoundException extends ApiException {

	private static final long serialVersionUID = 1L;

	public NotFoundException(I18nString message, String type) {
		super(message, 404, "not-found:" + type);
	}

}
