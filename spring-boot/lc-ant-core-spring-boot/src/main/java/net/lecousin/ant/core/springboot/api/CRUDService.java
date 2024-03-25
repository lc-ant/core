package net.lecousin.ant.core.springboot.api;

import org.springframework.lang.Nullable;

import net.lecousin.ant.core.api.ApiData;
import net.lecousin.ant.core.api.PageRequest;
import net.lecousin.ant.core.api.PageResponse;
import net.lecousin.ant.core.expression.Expression;
import reactor.core.publisher.Mono;

public interface CRUDService<T extends ApiData> {

	Mono<PageResponse<T>> search(@Nullable Expression<Boolean> criteria, @Nullable PageRequest pageRequest);
	
	default Mono<T> findById(String id) {
		return search(
			ApiData.FIELD_ID.is(id),
			PageRequest.first()
		).mapNotNull(PageResponse::firstOrNull);
	}
	
	Mono<T> create(T dto);
	
	Mono<T> update(T dto);
	
	Mono<Void> delete(String id);
	
}
