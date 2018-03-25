package io.crnk.core.engine.result;

import java.util.List;

public interface ResultFactory {

	<T> Result<T> just(T object);

	<T> Result<List<T>> zip(List<Result<T>> results);
}
