package net.lecousin.ant.core.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

	private Long total;
	private List<T> data;
	
	public Optional<T> first() {
		return data != null && !data.isEmpty() ? Optional.of(data.get(0)) : Optional.empty();
	}
	
	public T firstOrNull() {
		return first().orElse(null);
	}
	
	public <R> PageResponse<R> map(Function<T, R> mapper) {
		return new PageResponse<R>(total, data.stream().map(mapper).toList());
	}
	
	public PageResponse<T> merge(PageResponse<T> other) {
		List<T> newData = new ArrayList<>(data.size() + other.data.size());
		newData.addAll(data);
		newData.addAll(other.data);
		return new PageResponse<>(
			this.total != null && other.total != null ? this.total + other.total : null,
			newData
		);
	}
	
}
