package net.lecousin.ant.core.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NodePermissionDeclaration {
	
	private Collection<String> nodeTypes;
	private String service;
	private String right;
	@JsonIgnore
	private List<NodePermissionDeclaration> includes = new LinkedList<>();
	
	public Set<NodePermissionDeclaration> allIncluded() {
		Set<NodePermissionDeclaration> set = new HashSet<>();
		set.add(this);
		includes.forEach(p -> set.addAll(p.allIncluded()));
		return set;
	}
	
	public NodePermission toGrant(String tenantId, String nodeId, boolean granted) {
		return new NodePermission(tenantId, nodeId, service, right, granted);
	}
	
	public boolean isDeclarationFor(NodePermission p) {
		return service.equals(p.getService()) && right.equals(p.getRight());
	}
	
}
