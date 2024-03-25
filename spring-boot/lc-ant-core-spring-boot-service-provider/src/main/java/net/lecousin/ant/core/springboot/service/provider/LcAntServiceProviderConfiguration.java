package net.lecousin.ant.core.springboot.service.provider;

import java.security.GeneralSecurityException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.FormLoginSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.HttpBasicSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.LogoutSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.session.WebSessionManager;

import net.lecousin.ant.core.springboot.LcAntCoreConfiguration;
import net.lecousin.ant.core.springboot.LcAntCoreHttpConfiguration;
import net.lecousin.ant.core.springboot.http.HttpFilter;
import net.lecousin.ant.core.springboot.messaging.LcAntMessagingConfiguration;
import net.lecousin.ant.core.springboot.security.InternalJwtAuthenticationManager;
import net.lecousin.ant.core.springboot.security.JwtFilter;
import net.lecousin.ant.core.springboot.security.ServiceAuthenticationService;
import net.lecousin.ant.core.springboot.service.client.LcAntServiceClientConfiguration;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService;
import reactor.core.publisher.Mono;

@Configuration
@Import({LcAntCoreConfiguration.class, LcAntCoreHttpConfiguration.class, LcAntMessagingConfiguration.class, LcAntServiceClientConfiguration.class})
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@ComponentScan(basePackages = {
	"net.lecousin.ant.core.springboot.service.provider.init",
	"net.lecousin.ant.core.springboot.service.provider.health",
	"net.lecousin.ant.core.springboot.service.provider.info"
})
public class LcAntServiceProviderConfiguration {
	
	@Bean
	InternalJwtAuthenticationManager authenticationManager() throws GeneralSecurityException {
		return new InternalJwtAuthenticationManager();
	}
	
	@Bean
	WebSessionManager webSessionManager() {
		return exchange -> Mono.empty();
	}
	
	@Bean
	ServiceAuthenticationService serviceSelfAuthentication() {
		return new ServiceAuthenticationService();
	}
	
	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveAuthenticationManager authManager, TraceabilityService traceabilityService) {
		return http
		.csrf(CsrfSpec::disable)
		.formLogin(FormLoginSpec::disable)
		.httpBasic(HttpBasicSpec::disable)
		.logout(LogoutSpec::disable)
		.authorizeExchange(auth -> auth
			.pathMatchers(HttpMethod.GET, "/actuator/**").permitAll()
			.pathMatchers("/public-api/*/v*/**").permitAll()
			.pathMatchers("/**").authenticated()
		)
		.addFilterBefore(new HttpFilter(traceabilityService), SecurityWebFiltersOrder.HTTP_BASIC)
		.addFilterBefore(new JwtFilter(authManager), SecurityWebFiltersOrder.HTTP_BASIC)
		.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
		.build();
	}
	
}
