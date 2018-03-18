package io.crnk.core.engine.result;

import java.util.function.Function;

public class SimpleResult<T> implements Result<T> {

	private T object;

	public SimpleResult(T object) {
		this.object = object;
	}

	@Override
	public T get() {
		return object;
	}

	@Override
	public <D> Result<D> map(Function<T, D> function) {
		return new SimpleResult<>(function.apply(object));
	}

	@Override
	public Exception getException() {

	}
}
