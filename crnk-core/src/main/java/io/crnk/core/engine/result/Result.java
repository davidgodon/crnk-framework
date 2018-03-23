package io.crnk.core.engine.result;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Basic implemention to avoid third-party dependencies by core.
 */
public interface Result<T> {

	T get();

	<D> Result<D> map(Function<T, D> function);

	void subscribe(Consumer<T> consumer, Consumer<Exception> exceptionConsumer);

	Result<T> doWork(Consumer<T> function);

	<D, R> Result<R> mergeMap(Result<D> other, BiFunction<T, D, R> function);

	<R> Result<R> merge(Function<T, Result<R>> other);

	Exception getException();
}
