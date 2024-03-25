package net.lecousin.ant.core.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Root implements Grant {

	public static final String AUTHORITY = "root";
	
	public static final Root SINGLETON = new Root();
	
	@Override
	public String toAuthority() {
		return AUTHORITY;
	}
	
}
