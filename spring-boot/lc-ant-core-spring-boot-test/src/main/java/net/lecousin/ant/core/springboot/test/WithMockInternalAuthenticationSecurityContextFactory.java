package net.lecousin.ant.core.springboot.test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import net.lecousin.ant.core.security.LcAntSecurity;

public class WithMockInternalAuthenticationSecurityContextFactory implements WithSecurityContextFactory<WithMockInternalAuthentication> {

	@Override
	public SecurityContext createSecurityContext(WithMockInternalAuthentication annotation) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();

		String token = JWT.create()
		.withSubject(annotation.username())
		.withExpiresAt(Instant.now().plus(Duration.ofHours(1)))
		.withClaim(LcAntSecurity.CLAIM_AUTHORITIES, Arrays.asList(annotation.authorities()))
		.sign(Algorithm.RSA512(null, LcAntServiceTestContextCustomizer.privateKey));
		
		context.setAuthentication(new UsernamePasswordAuthenticationToken(annotation.username(), token, Arrays.stream(annotation.authorities()).map(SimpleGrantedAuthority::new).toList()));
		return context;
	}
	
}
