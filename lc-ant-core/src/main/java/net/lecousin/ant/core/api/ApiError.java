package net.lecousin.ant.core.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {

	private int httpCode;
	private String errorCode;
	private String errorMessage;
	private String correlationId;
	
}
