package net.lecousin.ant.core.springboot.messaging;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.security.RequiredPermissions;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiDataChangeEvent implements Serializable {
	
	public static final String TOPIC_EXCHANGE = "lc-ant.api-data-change.topic";

	private static final long serialVersionUID = 1L;
	
	public enum EventType {
		CREATED, UPDATED, DELETED
	}
	
	private String dataType;
	private EventType eventType;
	
	private String tenantId;
	
	private String dataId;
	private long dataVersion;
	private Serializable data;
	
	private RequiredPermissions requiredPermissions;
	
	public static String getRoutingKeyForDataType(String dataType) {
		if (dataType.startsWith("net.lecousin.ant."))
			dataType = dataType.substring("net.lecousin.ant.".length());
		dataType = dataType.replace('.', '-');
		return dataType;
	}

}
