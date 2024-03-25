package net.lecousin.ant.core.security;

import java.util.Optional;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LcAntSecurity {

	public static final String SUBJECT_TYPE_SERVICE = "service";
	public static final String SUBJECT_TYPE_USER = "user";
	
	public static final String CLAIM_AUTHORITIES = "authorities";
	
	public static Stream<Grant> toGrants(Stream<String> authorities) {
		return authorities.map(
			authority -> {
				int i = authority.indexOf(':');
				if (i < 0) {
					if (Root.AUTHORITY.equals(authority))
						return Optional.of(Root.SINGLETON);
					return Optional.<Grant>empty();
				}
				String prefix = authority.substring(0, i);
				if (GrantedPermission.AUTHORITY_PREFIX.equals(prefix))
					return Optional.of(GrantedPermission.fromAuthoritySubString(authority.substring(i + 1)));
				if (NodePermission.AUTHORITY_PREFIX.equals(prefix))
					return Optional.of(NodePermission.fromAuthoritySubString(authority.substring(i + 1)));
				if (TenantPermission.AUTHORITY_PREFIX.equals(prefix))
					return Optional.of(new TenantPermission(authority.substring(i + 1)));
				return Optional.<Grant>empty();
			}
		).filter(Optional::isPresent).map(Optional::get);
	}
	
}
