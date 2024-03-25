package net.lecousin.ant.core.manifest;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestDescriptor {

	private List<String> tags;
	private IODescriptor input;
	private IODescriptor output;
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public class IODescriptor {
		private Map<String, String> env;
		private Map<String, String> properties;
	}
	
}
