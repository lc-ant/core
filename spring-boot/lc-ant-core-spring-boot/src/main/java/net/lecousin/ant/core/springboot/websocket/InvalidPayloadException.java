package net.lecousin.ant.core.springboot.websocket;

import net.lecousin.ant.core.api.exceptions.BadRequestException;
import net.lecousin.commons.io.text.i18n.StaticI18nString;

public class InvalidPayloadException extends BadRequestException {

	private static final long serialVersionUID = 1L;

	public InvalidPayloadException(String message) {
		super(new StaticI18nString("Invalid websocket message payload: " + message), "invalid-payload");
	}
	
}
