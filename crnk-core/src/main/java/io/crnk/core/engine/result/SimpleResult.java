package io.crnk.core.engine.result;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleResult<T> implements Result<T> {

	private T object;

	// FIXME needed?
	private RuntimeException exception;

	public SimpleResult(T object) {
		this.object = object;
	}

	@Override
	public T get() {
		if (exception != null) {
			throw exception;
		}
		return object;
	}

	@Override
	public <D> Result<D> map(Function<T, D> function) {
		return new SimpleResult<>(function.apply(object));
	}

	@Override
	public Result<T> doWork(Consumer<T> function) {
		function.accept(object);
		return this;
	}

	@Override
	public <D, R> Result<R> mergeMap(Result<D> other, BiFunction<T, D, R> function) {
		D otherObject = other.get();
		return new SimpleResult<>(function.apply(object, otherObject));
	}

	@Override
	public <R> Result<R> merge(Function<T, Result<R>> other) {
		return other.apply(object);
	}

	@Override
	public Exception getException() {
		return exception;
	}
}
