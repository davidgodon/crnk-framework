package io.crnk.core.engine.result;

public class SimpleResultFactory implements ResultFactory {
	@Override
	public <T> Result<T> just(T object) {
		return new SimpleResult<>(object);
	}
}
