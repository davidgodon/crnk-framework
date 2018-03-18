package io.crnk.core.engine.internal.dispatcher.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.core.engine.filter.ResourceFilterDirectory;
import io.crnk.core.engine.filter.ResourceModificationFilter;
import io.crnk.core.engine.internal.document.mapper.DocumentMapper;
import io.crnk.core.engine.parser.TypeParser;
import io.crnk.core.engine.properties.PropertiesProvider;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.result.ResultFactory;

import java.util.List;

public class ControllerContext {

	private ResourceRegistry resourceRegistry;

	private PropertiesProvider propertiesProvider;

	private TypeParser typeParser;


	private ObjectMapper objectMapper;

	private DocumentMapper documentMapper;

	private List<ResourceModificationFilter> modificationFilters;

	private ResultFactory resultFactory;

	private ResourceFilterDirectory resourceFilterDirectory;

	//  FIXME 	this.resourceFilterDirectory = documentMapper != null ? documentMapper.getFilterBehaviorManager() : null;


	public ResourceFilterDirectory getResourceFilterDirectory() {
		return resourceFilterDirectory;
	}

	public ResourceRegistry getResourceRegistry() {
		return resourceRegistry;
	}

	public PropertiesProvider getPropertiesProvider() {
		return propertiesProvider;
	}

	public TypeParser getTypeParser() {
		return typeParser;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public DocumentMapper getDocumentMapper() {
		return documentMapper;
	}

	public List<ResourceModificationFilter> getModificationFilters() {
		return modificationFilters;
	}

	public ResultFactory getResultFactory() {
		return resultFactory;
	}

}
