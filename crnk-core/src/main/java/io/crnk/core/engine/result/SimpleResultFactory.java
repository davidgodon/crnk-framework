package io.crnk.core.engine.result;

import java.util.ArrayList;
import java.util.List;

public class SimpleResultFactory implements ResultFactory {

	@Override
	public <T> Result<T> just(T object) {
		return new SimpleResult<>(object);
	}

	@Override
	public <T> Result<List<T>> all(List<Result<T>> results) {
		ArrayList<T> list = new ArrayList<>();
		for (Result<T> result : results) {
			list.add(result.get());
		}
		return new SimpleResult<>(list);
	}
}
