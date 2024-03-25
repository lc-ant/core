package net.lecousin.ant.core.api;

import java.util.LinkedList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

	private Integer page;
	private Integer pageSize;
	private List<Sort> sort;
	private boolean withTotal;
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Sort {
		private String field;
		private SortOrder order;
	}
	
	public enum SortOrder {
		ASC,
		DESC,
	}
	
	public void forcePaging(int size) {
		if (page == null)
			this.page = 0;
		if (pageSize == null || pageSize < size)
			pageSize = size;
	}
	
	public void forcePaging(int pageIndex, int size) {
		this.page = pageIndex;
		if (pageSize == null || pageSize < size)
			pageSize = size;
	}
	
	public PageRequest addSort(Sort s) {
		if (sort == null) sort = new LinkedList<>();
		sort.add(s);
		return this;
	}
	
	public PageRequest addSort(String fieldName, SortOrder order) {
		return addSort(new Sort(fieldName, order));
	}
	
	public static PageRequest first() {
		return new PageRequest(0, 1, null, false);
	}
	
	public static PageRequest noPaging() {
		return new PageRequest(null, null, null, false);
	}
	
	public static PageRequest of(int page, int pageSize) {
		return new PageRequest(page, pageSize, null, false);
	}
	
	public static PageRequest of(int pageSize) {
		return of(0, pageSize);
	}
	
}
