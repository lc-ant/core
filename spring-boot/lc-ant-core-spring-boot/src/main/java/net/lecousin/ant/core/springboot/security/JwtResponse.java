package net.lecousin.ant.core.springboot.security;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {

	private String accessToken;
	@JsonFormat(shape = Shape.NUMBER)
	private Instant accessTokenExpiresAt;
	private String refreshToken;
	@JsonFormat(shape = Shape.NUMBER)
	private Instant refreshTokenExpiresAt;
	
}
