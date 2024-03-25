package net.lecousin.ant.core.manifest;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DockerImageDependency {

	private String image;
	private List<String> supportedTags;
	private List<Integer> exposedPorts;
	private String healthCheck;
	private TestDescriptor test;
	
}
