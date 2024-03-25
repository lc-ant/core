package net.lecousin.ant.core.api.exceptions;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import lombok.Getter;
import net.lecousin.ant.core.api.ApiError;
import net.lecousin.ant.core.api.traceability.Traceability;
import net.lecousin.commons.io.text.i18n.I18nString;
import net.lecousin.commons.io.text.i18n.StaticI18nString;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

public abstract class ApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public static final String I18N_NAMESPACE = "api-error";
	
	protected final I18nString message;
	@Getter
	protected final int httpCode;
	@Getter
	protected final String errorCode;
	@Getter
	protected final String correlationId;
	
	protected ApiException(I18nString message, int httpCode, String errorCode, String correlationId) {
		this.message = message;
		this.httpCode = httpCode;
		this.errorCode = errorCode;
		this.correlationId = correlationId;
	}
	
	protected ApiException(I18nString message, int httpCode, String errorCode) {
		this(message, httpCode, errorCode, "");
	}
	
	protected ApiException(I18nString message, int httpCode, String errorCode, Throwable cause, String correlationId) {
		super(cause);
		this.message = message;
		this.httpCode = httpCode;
		this.errorCode = errorCode;
		this.correlationId = correlationId;
	}
	
	protected ApiException(I18nString message, int httpCode, String errorCode, Throwable cause) {
		this(message, httpCode, errorCode, cause, "");
	}
	
	public static ApiException create(int httpCode, I18nString message, String correlationId) {
		return new ApiException(message, httpCode, "", correlationId) {
			private static final long serialVersionUID = 1L;
		};
	}
	
	public static ApiException create(int httpCode, String message, String correlationId) {
		return create(httpCode, new StaticI18nString(message), correlationId);
	}
	
	public static ApiException create(ApiError error) {
		return new ApiException(new StaticI18nString(error.getErrorMessage()), error.getHttpCode(), error.getErrorCode(), error.getCorrelationId()) {
			private static final long serialVersionUID = 1L;
		};
	}
	
	public static ApiException create(Throwable cause, String correlationId) {
		return new ApiException(new StaticI18nString(cause.getMessage()), 500, "internal-error", correlationId) {
			private static final long serialVersionUID = 1L;
		};
	}
	
	@Override
	public String getMessage() {
		return Objects.toString(message);
	}
	
	public Mono<String> localize(Locale locale) {
		return message != null ? Mono.fromFuture(message.localizeAsync(locale)) : Mono.just("");
	}
	
	public Mono<ApiError> toApiError() {
		return Mono.deferContextual(this::toApiError);
	}
	
	public Mono<ApiError> toApiError(ContextView ctx) {
		return toApiError(ctx.<Locale>getOrEmpty(Locale.class).orElse(Locale.ENGLISH), Traceability.fromContext(ctx));
	}
	
	public Mono<ApiError> toApiError(Locale locale, Optional<Traceability> traceability) {
		return localize(locale)
			.map(translatedMessage -> new ApiError(
				httpCode,
				errorCode,
				translatedMessage,
				traceability.map(Traceability::getCorrelationId).orElse(correlationId)
			));
	}
	
}
