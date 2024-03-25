package net.lecousin.ant.core.security;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TenantPermission implements Grant {

	public static final String AUTHORITY_PREFIX = "ten";
	
	private String tenantId;
	
	@Override
	public String toAuthority() {
		return AUTHORITY_PREFIX + ":" + tenantId;
	}
	
	@Override
	public String toString() {
		return toAuthority();
	}
	
}
