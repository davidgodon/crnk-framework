package io.crnk.reactive.repository;

import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.internal.utils.PreconditionUtil;
import io.crnk.core.engine.registry.RegistryEntry;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.list.ResourceList;
import java.util.HashMap;
import java.util.Map;
import reactor.core.publisher.Mono;

public class InMemoryReactiveResourceRepository<T, I> extends ReactiveResourceRepositoryBase<T, I> {


	public Map<I, T> schedules = new HashMap<>();

	public InMemoryReactiveResourceRepository(Class<T> clazz) {
		super(clazz);
	}

	@Override
	public Mono<ResourceList<T>> findAll(QuerySpec querySpec) {
		return Mono.fromCallable(() -> querySpec.apply(schedules.values()));
	}

	@Override
	public Mono<T> save(T entity) {
		RegistryEntry entry = resourceRegistry.findEntry(getResourceClass());
		ResourceField idField = entry.getResourceInformation().getIdField();

		I id = (I) idField.getAccessor().getValue(entity);
		PreconditionUtil.assertNotNull("no id specified", entity);

		schedules.put(id, entity);
		return Mono.just(entity);
	}

	@Override
	public Mono<Void> delete(I id) {
		return Mono.fromRunnable(() -> schedules.remove(id));
	}

	public void clear() {
		schedules.clear();
	}
}
