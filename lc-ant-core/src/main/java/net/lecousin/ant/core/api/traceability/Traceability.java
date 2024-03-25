package net.lecousin.ant.core.api.traceability;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import io.micrometer.context.ContextRegistry;
import lombok.Getter;
import lombok.ToString;
import net.lecousin.commons.io.bytes.data.BytesData;
import reactor.core.publisher.Hooks;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

@ToString
public class Traceability {

	@Getter
	private String correlationId;
	@Getter
	private String tenantId;
	@Getter
	private String username;
	
	private final AtomicInteger sequence;
	
	private Traceability(int startSequence) {
		this.sequence = new AtomicInteger(startSequence);
	}
	
	private static final String CONTEXT_KEY = "lc-ant-tracability";
	private static final ThreadLocal<Traceability> THREAD_LOCAL = new ThreadLocal<>();
	private static final String MDC_CORRELATION_ID_KEY = "CORRELATION_ID";
	private static final String MDC_TENANT_ID_KEY = "TENANT_ID";
	private static final String MDC_USERNAME_KEY = "USERNAME";
	
	private static String getPaddedValue(String value, int padding) {
		if (value == null || value.isEmpty()) value = "-";
		return StringUtils.rightPad(value, padding);
	}
	
	static {
		Hooks.enableAutomaticContextPropagation();
		ContextRegistry.getInstance().registerThreadLocalAccessor(
				CONTEXT_KEY,
				THREAD_LOCAL::get,
				trace -> {
					THREAD_LOCAL.set(trace);
					MDC.put(MDC_CORRELATION_ID_KEY, getPaddedValue(trace.getCorrelationId(), 20));
					MDC.put(MDC_TENANT_ID_KEY, getPaddedValue(trace.getTenantId(), 24));
					MDC.put(MDC_USERNAME_KEY, getPaddedValue(trace.getUsername(), 40));
				},
				() -> {
					THREAD_LOCAL.remove();
					MDC.remove(MDC_CORRELATION_ID_KEY);
					MDC.remove(MDC_TENANT_ID_KEY);
					MDC.remove(MDC_USERNAME_KEY);
				});
	}
	
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
		if (this.correlationId.equals(MDC.get(MDC_CORRELATION_ID_KEY)))
			MDC.put(MDC_TENANT_ID_KEY, getPaddedValue(tenantId, 24));
	}
	
	public void setUsername(String username) {
		this.username = username;
		if (this.correlationId.equals(MDC.get(MDC_CORRELATION_ID_KEY)))
			MDC.put(MDC_USERNAME_KEY, getPaddedValue(username, 40));
	}
	
	public Context toContext(Context ctx) {
		return ctx.put(CONTEXT_KEY, this);
	}
	
	public Context toContext() {
		return Context.of(CONTEXT_KEY, this);
	}
	
	public static Optional<Traceability> fromContext(ContextView ctx) {
		return ctx.getOrEmpty(CONTEXT_KEY);
	}
	
	public int newSequence() {
		return sequence.getAndIncrement();
	}
	
	
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int NODE_RANDOM = RANDOM.nextInt();
	private static final AtomicInteger COUNT = new AtomicInteger(0);
	
	public static Traceability create() {
		Traceability trace = new Traceability(1);
		byte[] bytes = new byte[15];
		BytesData.BE.writeSigned8Bytes(bytes, System.currentTimeMillis());
		BytesData.BE.writeSigned4Bytes(bytes, 8, NODE_RANDOM);
		int count;
		while ((count = COUNT.getAndIncrement()) > 0xFFFFFF)
			COUNT.compareAndSet(0x1000000, 0);
		BytesData.BE.writeUnsigned3Bytes(bytes, 12, count);
		trace.correlationId = Base64.getUrlEncoder().encodeToString(bytes);
		return trace;
	}
	
	public static Traceability createWithCorrelationId(String correlationId) {
		Traceability trace = new Traceability(1);
		trace.correlationId = correlationId;
		return trace;
	}
	
	private static final String HEADER_TRACE_ID = "X-LC-ANT-TRACE-ID";
	private static final String HEADER_TRACE_WHO = "X-LC-ANT-TRACE-WHO";

	public static Optional<Traceability> fromHeaders(Map<String, List<String>> headers) {
		if (!headers.containsKey(HEADER_TRACE_ID))
			return Optional.empty();
		Traceability trace = new Traceability(2);
		trace.correlationId = headers.get(HEADER_TRACE_ID).get(0);
		List<String> list = headers.get(HEADER_TRACE_WHO);
		if (list != null && !list.isEmpty()) {
			String who = list.get(0);
			int i = who.indexOf(';');
			if (i < 0) {
				trace.tenantId = who;
			} else {
				trace.tenantId = who.substring(0, i);
				trace.username = who.substring(i + 1);
			}
		}
		return Optional.of(trace);
	}
	
	public void toHeaders(Map<String, List<String>> headers) {
		headers.put(HEADER_TRACE_ID, List.of(correlationId));
		if (tenantId != null) {
			StringBuilder s = new StringBuilder(tenantId);
			if (username != null)
				s.append(';').append(username);
			headers.put(HEADER_TRACE_WHO, List.of(s.toString()));
		}
	}
}
