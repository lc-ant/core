package net.lecousin.ant.core.springboot.traceability;

import net.lecousin.ant.core.api.traceability.TraceType;
import net.lecousin.ant.core.api.traceability.Traceability;

/**
 * Handler notified for steps on a Traceability.<br/>
 * The sequence needs to be considered together with the application name:
 * the application creating the trace will start at sequence 1, the applications receiving the trace
 * will restart at sequence 2. So the end of sequence 1 can be used as the end of the trace, but
 * other sequence number needs to be associated with the application name. 
 */
public interface TraceabilityHandler {

	void start(Traceability trace, String app, int sequence, String service, TraceType type, String detail);
	
	void end(Traceability trace, String app, int sequence, String service, TraceType type, String detail, int resultCode, String error);
	
}
