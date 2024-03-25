package net.lecousin.ant.core.springboot.http;

import java.util.LinkedList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import net.lecousin.ant.core.api.PageRequest;
import net.lecousin.ant.core.api.PageRequest.Sort;
import net.lecousin.ant.core.api.PageRequest.SortOrder;
import reactor.core.publisher.Mono;

public class PageRequestMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return PageRequest.class.equals(parameter.getParameterType());
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
		var params = exchange.getRequest().getQueryParams();
		PageRequest result = new PageRequest();
		result.setPage(parseInt(params, "page"));
		result.setPageSize(parseInt(params, "pageSize"));
		result.setWithTotal(parseBoolean(params, "withTotal"));
		result.setSort(parseSort(params));
		return Mono.just(result);
	}
	
	private Integer parseInt(MultiValueMap<String, String> params, String paramName) {
		var list = params.get(paramName);
		if (list == null || list.isEmpty()) return null;
		try {
			return Integer.parseInt(list.get(0));
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	private boolean parseBoolean(MultiValueMap<String, String> params, String paramName) {
		var list = params.get(paramName);
		if (list == null || list.isEmpty()) return false;
		return Boolean.valueOf(list.get(0));
	}
	
	private List<Sort> parseSort(MultiValueMap<String, String> params) {
		var list = params.get("sort");
		if (list == null || list.isEmpty()) return null;
		List<Sort> result = new LinkedList<>();
		for (String s : list) {
			if (s.startsWith("-"))
				result.add(new Sort(s.substring(1), SortOrder.DESC));
			else
				result.add(new Sort(s, SortOrder.ASC));
		}
		return result;
	}

}
