package net.lecousin.ant.core.springboot.http;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import net.lecousin.ant.core.api.ApiError;
import net.lecousin.ant.core.api.exceptions.ApiException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class ApiErrorControllerAdvice {

	@ExceptionHandler(ApiException.class)
	public Mono<ResponseEntity<ApiError>> handle(ApiException error) {
		return error.toApiError()
		.map(apiError -> ResponseEntity.status(apiError.getHttpCode()).body(apiError));
	}
	
}
