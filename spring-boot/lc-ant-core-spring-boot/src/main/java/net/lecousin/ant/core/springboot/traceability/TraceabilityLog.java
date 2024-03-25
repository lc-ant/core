package net.lecousin.ant.core.springboot.traceability;

import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.api.traceability.TraceType;
import net.lecousin.ant.core.api.traceability.Traceability;

@Slf4j
public class TraceabilityLog implements TraceabilityHandler {

	@Override
	public void start(Traceability trace, String app, int sequence, String service, TraceType type, String detail) {
		log.debug("->> {} ¤ {} ¤ {} ¤ {}", sequence, service, type, detail);
	}

	@Override
	public void end(Traceability trace, String app, int sequence, String service, TraceType type, String detail, int resultCode, String error) {
		log.debug("<<- {} ¤ {} ¤ {} ¤ {} ¤ {} ¤ {}", sequence, service, type, detail, resultCode, error);
	}

}
