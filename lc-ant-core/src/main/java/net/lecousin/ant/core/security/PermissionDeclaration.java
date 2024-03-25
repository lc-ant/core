package net.lecousin.ant.core.security;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDeclaration {
	
	private String service;
	private String right;
	@JsonIgnore
	private List<PermissionDeclaration> includes = new LinkedList<>();
	
	public Set<PermissionDeclaration> allIncluded() {
		Set<PermissionDeclaration> set = new HashSet<>();
		set.add(this);
		includes.forEach(p -> set.addAll(p.allIncluded()));
		return set;
	}
	
	public GrantedPermission toGrant() {
		return new GrantedPermission(service, right);
	}
	
	public boolean isDeclarationFor(GrantedPermission p) {
		return service.equals(p.getService()) && right.equals(p.getRight());
	}
	
}
