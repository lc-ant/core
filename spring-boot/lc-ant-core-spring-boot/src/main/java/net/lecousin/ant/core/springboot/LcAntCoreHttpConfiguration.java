package net.lecousin.ant.core.springboot;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.function.server.ServerRequest;

import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.springboot.http.ApiErrorControllerAdvice;

@Configuration
@Import({WebFluxConfiguration.class})
@Slf4j
public class LcAntCoreHttpConfiguration {

	@Bean
	ApiErrorControllerAdvice apiErrorControllerAdvice() {
		return new ApiErrorControllerAdvice();
	}
	
	@Bean
	ErrorAttributes errorMapper() {
		return new DefaultErrorAttributes() {
			@Override
			public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
				Map<String, Object> map = super.getErrorAttributes(request, options);
				Throwable error = getError(request);
				if (error instanceof ErrorResponse e) {
					log.error("API error", error);
					map.put("httpCode", e.getStatusCode().value());
					map.put("errorCode", e.getDetailMessageCode());
					map.put("errorMessage", error.getMessage());
					map.put("details", e.getBody());
				}
				return map;
			}
		};
	}

}
