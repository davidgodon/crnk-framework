package io.crnk.reactive.repository;

import io.crnk.core.repository.RelationshipMatcher;
import io.crnk.core.repository.Repository;


public interface ReactiveRelationshipRepository<T, I, D, J> extends Repository {

	RelationshipMatcher getMatcher();

}
