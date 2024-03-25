package net.lecousin.ant.core.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GrantedPermission implements Grant {
	
	public static final String AUTHORITY_PREFIX = "per";
	
	private String service;
	private String right;
	
	@Override
	public String toAuthority() {
		return AUTHORITY_PREFIX + ":" + service + ":" + right;
	}
	
	@Override
	public String toString() {
		return toAuthority();
	}
	
	public static GrantedPermission fromAuthoritySubString(String s) {
		int i = s.indexOf(':');
		return new GrantedPermission(s.substring(0, i), s.substring(i + 1));
	}
	
}
