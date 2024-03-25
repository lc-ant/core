package net.lecousin.ant.core.springboot.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.api.exceptions.UnauthorizedException;
import net.lecousin.ant.core.security.LcAntSecurity;
import reactor.core.publisher.Mono;

@Slf4j
public class InternalJwtAuthenticationManager implements ReactiveAuthenticationManager, InitializingBean {

	@Value("${lc-ant.security.public-key}")
	private String publicKeyBase64;
	
    private JWTVerifier verifier;

    @Override
    public void afterPropertiesSet() throws GeneralSecurityException {
    	X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64));
    	PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
        this.verifier = JWT.require(Algorithm.RSA512((RSAPublicKey) publicKey, null)).build();
    }

	@Override
	public Mono<Authentication> authenticate(Authentication authentication) {
		return validate(authentication.getCredentials().toString()).checkpoint("validate received token").cache();
	}

    public Mono<Authentication> validate(String token) {
    	return Mono.deferContextual(ctx -> {
            DecodedJWT decoded = JWT.decode(token);
			try {
				verifier.verify(token);
			} catch (Exception e) {
				return Mono.error(new UnauthorizedException());
			}
            String subject = decoded.getSubject();
            var authorities = decoded.getClaim(LcAntSecurity.CLAIM_AUTHORITIES).asList(String.class);
            Authentication result = new UsernamePasswordAuthenticationToken(
            	subject, token,
                authorities.stream().map(SimpleGrantedAuthority::new).toList()
            );
            SecurityUtils.updateTraceability(ctx, result);
            log.debug("Authenticated: {} with authorities {}", subject, authorities);
            return Mono.just(result);
    	});
    }
	
}
