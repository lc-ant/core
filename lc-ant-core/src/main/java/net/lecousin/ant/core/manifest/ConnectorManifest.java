package net.lecousin.ant.core.manifest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.mapping.Mappers;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ConnectorManifest {

	private String name;
	private String impl;
	private String connectorClass;
	private Map<String, String> stack = new HashMap<>();
	private Map<String, DockerImageDependency> dependencies = new HashMap<>();
	
	private static List<ConnectorManifest> connectors = null;
	
	public static List<ConnectorManifest> load() {
		synchronized (ConnectorManifest.class) {
			if (connectors == null) {
				connectors = new LinkedList<>();
				try {
					var urls = MicroServiceManifest.class.getClassLoader().getResources("META-INF/connector.yaml");
					while (urls.hasMoreElements()) {
						URL url = urls.nextElement();
						try (var input = url.openStream()) {
							connectors.add(load(input));
						} catch (Exception e) {
							log.error("Error parsing micro-service manifest {}", url, e);
						}
					}
				} catch (Exception e) {
					log.error("Error retrieving micro-services manifests", e);
				}

			}
		}
		return connectors;
	}
	
	public static ConnectorManifest load(InputStream input) throws IOException {
		return Mappers.YAML_MAPPER.readValue(input, ConnectorManifest.class);
	}
}
