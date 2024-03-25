package net.lecousin.ant.core.springboot.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.api.exceptions.UnauthorizedException;
import net.lecousin.ant.core.api.traceability.Traceability;
import net.lecousin.ant.core.security.LcAntSecurity;
import net.lecousin.ant.core.security.TenantPermission;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtils {
	
	public static final Mono<Authentication> UNAUTHORIZED = Mono.error(new UnauthorizedException());
	
	public static Mono<Authentication> getAuthentication() {
		return ReactiveSecurityContextHolder.getContext()
			.flatMap(ctx -> Mono.justOrEmpty(Optional.ofNullable(ctx.getAuthentication())))
			.switchIfEmpty(UNAUTHORIZED);
	}
	
	public static boolean isSubject(Authentication auth, String subjectType, String subjectId) {
		String subject = auth.getPrincipal().toString();
		String expected = subjectType + ':' + subjectId;
		return subject.equals(expected) || subject.startsWith(expected + ':');
	}
	
	public static void updateTraceability(Traceability trace, Authentication auth) {
		if (auth == null) return;
		String subject = auth.getPrincipal().toString();
		if (!subject.startsWith(LcAntSecurity.SUBJECT_TYPE_SERVICE)) {
       		if (trace.getUsername() == null) trace.setUsername(subject);
       		if (trace.getTenantId() == null) {
       			var tenantOpt = auth.getAuthorities().stream().filter(a -> a.getAuthority().startsWith(TenantPermission.AUTHORITY_PREFIX + ":")).findAny();
    			if (tenantOpt.isPresent()) {
    				var tenantId = tenantOpt.get().getAuthority().substring(TenantPermission.AUTHORITY_PREFIX.length() + 1);
    				trace.setTenantId(tenantId);
    			}
        	}
        }
	}
	
	public static void updateTraceability(ContextView ctx, Authentication auth) {
		if (auth == null) return;
		Traceability.fromContext(ctx).ifPresent(trace -> updateTraceability(trace, auth));
	}
	
}
