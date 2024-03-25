package net.lecousin.ant.core.springboot.http;

import org.springframework.core.MethodParameter;
import org.springframework.web.service.invoker.HttpRequestValues.Builder;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;

import net.lecousin.ant.core.api.PageRequest;
import net.lecousin.ant.core.api.PageRequest.Sort;
import net.lecousin.ant.core.api.PageRequest.SortOrder;

public class PageRequestHttpServiceArgumentResolver implements HttpServiceArgumentResolver {

	@Override
	public boolean resolve(Object argument, MethodParameter parameter, Builder requestValues) {
		if (argument instanceof PageRequest p) {
			if (p.getPage() != null) requestValues.addRequestParameter("page", p.getPage().toString());
			if (p.getPageSize() != null) requestValues.addRequestParameter("pageSize", p.getPageSize().toString());
			if (p.isWithTotal()) requestValues.addRequestParameter("withTotal", "true");
			if (p.getSort() != null && !p.getSort().isEmpty()) {
				String[] strs = new String[p.getSort().size()];
				for (int i = 0; i < strs.length; ++i) {
					Sort sort = p.getSort().get(i);
					strs[i] = (SortOrder.DESC.equals(sort.getOrder()) ? "-" : "") + sort.getField();
				}
				requestValues.addRequestParameter("sort", strs);
			}
			return true;
		}
		return false;
	}
	
}
