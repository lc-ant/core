package net.lecousin.ant.core.springboot.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtRequest {

	private String accessToken;
	private String refreshToken;
	
}
