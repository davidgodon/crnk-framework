package io.crnk.core.engine.internal.repository;

import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.query.QueryAdapter;
import io.crnk.core.engine.result.Result;
import io.crnk.core.repository.response.JsonApiResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A repository adapter for relationship repository.
 */
@SuppressWarnings("unchecked")
public interface RelationshipRepositoryAdapter {

	Result<JsonApiResponse> setRelation(Object source, Object targetId, ResourceField field, QueryAdapter queryAdapter);

	Result<JsonApiResponse> setRelations(Object source, Collection targetIds, ResourceField field, QueryAdapter queryAdapter);

	Result<JsonApiResponse> addRelations(Object source, Collection targetIds, ResourceField field, QueryAdapter queryAdapter);

	Result<JsonApiResponse> removeRelations(Object source, Collection targetIds, ResourceField field, QueryAdapter queryAdapter);

	Result<JsonApiResponse> findOneTarget(Object sourceId, ResourceField field, QueryAdapter queryAdapter);


	Result<JsonApiResponse> findManyTargets(Object sourceId, ResourceField field, QueryAdapter queryAdapter);

	Result<Map<?, JsonApiResponse>> findBulkManyTargets(List sourceIds, ResourceField field, QueryAdapter queryAdapter);

	Result<Map<?, JsonApiResponse>> findBulkOneTargets(List sourceIds, ResourceField field, QueryAdapter queryAdapter);


}
