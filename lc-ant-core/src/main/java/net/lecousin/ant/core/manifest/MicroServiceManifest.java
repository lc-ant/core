package net.lecousin.ant.core.manifest;

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
public class MicroServiceManifest {

	private String name;
	private Map<String, String> stack = new HashMap<>();
	private Dependencies dependencies = new Dependencies();
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Dependencies {
		
		private Map<String, String> connectors = new HashMap<>();
		private Map<String, String> services = new HashMap<>();
		private Map<String, String> external = new HashMap<>();
		
	}
	
	public static List<MicroServiceManifest> load() {
		List<MicroServiceManifest> list = new LinkedList<>();
		try {
			var urls = MicroServiceManifest.class.getClassLoader().getResources("META-INF/microservice.yaml");
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				try (var input = url.openStream()) {
					list.add(Mappers.YAML_MAPPER.readValue(input, MicroServiceManifest.class));
				} catch (Exception e) {
					log.error("Error parsing micro-service manifest {}", url, e);
				}
			}
		} catch (Exception e) {
			log.error("Error retrieving micro-services manifests", e);
		}
		return list;
	}
	
}
