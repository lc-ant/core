package net.lecousin.ant.core.springboot.api;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.lecousin.ant.core.api.PageRequest;
import net.lecousin.ant.core.api.PageResponse;
import reactor.core.publisher.Mono;

public interface TextSearchTenantService<T> {

	Mono<PageResponse<T>> textSearch(@NonNull String tenantId, @NonNull String text, @Nullable PageRequest pageRequest);
}
