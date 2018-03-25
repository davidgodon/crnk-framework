package io.crnk.reactive.internal;

import io.crnk.core.engine.result.Result;
import io.crnk.core.engine.result.ResultFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MonoResultFactory implements ResultFactory {
	@Override
	public <T> Result<T> just(T object) {
		return new MonoResult(Mono.justOrEmpty(object));
	}

	@Override
	public <T> Result<List<T>> zip(List<Result<T>> results) {
		List<Mono<T>> monos = new ArrayList<>();
		for (Result<T> result : results) {
			monos.add(((MonoResult) result).getMono());
		}
		Mono<List<T>> zipped = Mono.zip(monos, a -> Arrays.asList((T[]) a));
		return new MonoResult<>(zipped);
	}
}
