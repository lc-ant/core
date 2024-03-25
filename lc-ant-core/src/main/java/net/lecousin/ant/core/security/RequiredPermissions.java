package net.lecousin.ant.core.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
	@JsonSubTypes.Type(RequiredPermissions.RootPermission.class),
	@JsonSubTypes.Type(RequiredPermissions.NodePathPermission.class),
	@JsonSubTypes.Type(RequiredPermissions.And.class),
	@JsonSubTypes.Type(RequiredPermissions.Or.class),
})
public interface RequiredPermissions extends Serializable {
	
	boolean isAllowed(List<Grant> grants);

	@AllArgsConstructor
	public static class RootPermission implements RequiredPermissions {
		private static final long serialVersionUID = 1L;
		
		private final String service;
		private final String right;
		
		public static RootPermission of(PermissionDeclaration perm) {
			return new RootPermission(perm.getService(), perm.getRight());
		}
		
		@Override
		public boolean isAllowed(List<Grant> grants) {
			return grants.stream().anyMatch(grant -> grant instanceof GrantedPermission gp && gp.getService().equals(service) && gp.getRight().equals(right));
		}
	}
	
	@AllArgsConstructor
	public static class NodePathPermission implements RequiredPermissions {
		private static final long serialVersionUID = 1L;
		
		private final String tenantId;
		private final List<String> nodeIdPath;
		private final RootPermission permission;
		
		@Override
		public boolean isAllowed(List<Grant> grants) {
			int grantedIndex = -2;
			int revokedIndex = -2;
			for (Grant g : grants) {
				if (g instanceof NodePermission np) {
					if (tenantId != null && !np.getTenantId().equals(NodePermission.TENANT_ID_ALL) && !np.getTenantId().equals(tenantId)) continue;
					if (!np.getService().equals(permission.service)) continue;
					if (!np.getRight().equals(permission.right)) continue;
					int index = nodeIdPath.indexOf(np.getNodeId());
					if (index < 0) {
						if (!np.getNodeId().equals(NodePermission.NODE_ID_ROOT)) continue;
					}
					if (np.isGranted()) {
						if (index == nodeIdPath.size() - 1) return true;
						if (index > grantedIndex) grantedIndex = index;
					} else {
						if (index == nodeIdPath.size() - 1) return false;
						if (index > revokedIndex) revokedIndex = index;
					}
				}
			}
			return grantedIndex > -2 && grantedIndex > revokedIndex;
		}
	}
	
	@AllArgsConstructor
	public static class And implements RequiredPermissions {
		private static final long serialVersionUID = 1L;
		
		private final Collection<RequiredPermissions> and;
		
		@Override
		public boolean isAllowed(List<Grant> grants) {
			for (var r : and) if (!r.isAllowed(grants)) return false;
			return true;
		}
	}
	
	@AllArgsConstructor
	public static class Or implements RequiredPermissions {
		private static final long serialVersionUID = 1L;
		
		private final Collection<RequiredPermissions> or;
		
		@Override
		public boolean isAllowed(List<Grant> grants) {
			if (or.isEmpty()) return true;
			for (var r : or) if (r.isAllowed(grants)) return true;
			return false;
		}
	}
	
}
