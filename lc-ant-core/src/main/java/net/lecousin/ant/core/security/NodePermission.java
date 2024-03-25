package net.lecousin.ant.core.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NodePermission implements Grant {
	
	public static final String AUTHORITY_PREFIX = "nod";
	public static final String TENANT_ID_ALL = "0";
	public static final String NODE_ID_ROOT = "0";
	
	private String tenantId;
	private String nodeId;
	private String service;
	private String right;
	private boolean granted;
	
	@Override
	public String toAuthority() {
		return AUTHORITY_PREFIX + ":" + tenantId + ":" + nodeId + ":" + service + ":" + (granted ? "+" : "-") + right;
	}
	
	@Override
	public String toString() {
		return toAuthority();
	}
	
	public static NodePermission fromAuthoritySubString(String s) {
		String[] elements = s.split(":");
		return new NodePermission(elements[0], elements[1], elements[2], elements[3].substring(1), elements[3].charAt(0) == '+');
	}
	
}
