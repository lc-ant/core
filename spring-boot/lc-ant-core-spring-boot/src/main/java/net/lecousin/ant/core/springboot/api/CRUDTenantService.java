package net.lecousin.ant.core.springboot.api;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import net.lecousin.ant.core.api.ApiData;
import net.lecousin.ant.core.api.PageRequest;
import net.lecousin.ant.core.api.PageResponse;
import net.lecousin.ant.core.expression.Expression;
import reactor.core.publisher.Mono;

public interface CRUDTenantService<T extends ApiData> {

	Mono<PageResponse<T>> search(@NonNull String tenantId, @Nullable Expression<Boolean> criteria, @Nullable PageRequest pageRequest);
	
	default Mono<T> findById(@NonNull String tenantId, @NonNull String id) {
		return search(
			tenantId,
			ApiData.FIELD_ID.is(id),
			PageRequest.first()
		).mapNotNull(PageResponse::firstOrNull);
	}
	
	Mono<T> create(@NonNull String tenantId, @NonNull T dto);
	
	Mono<T> update(@NonNull String tenantId, @NonNull T dto);
	
	Mono<Void> delete(@NonNull String tenantId, @NonNull String id);
	
}
