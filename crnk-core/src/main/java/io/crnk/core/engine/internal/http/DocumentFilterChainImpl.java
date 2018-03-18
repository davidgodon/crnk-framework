package io.crnk.core.engine.internal.http;

import io.crnk.core.engine.dispatcher.Response;
import io.crnk.core.engine.filter.DocumentFilter;
import io.crnk.core.engine.filter.DocumentFilterChain;
import io.crnk.core.engine.filter.DocumentFilterContext;
import io.crnk.core.engine.internal.dispatcher.controller.BaseController;
import io.crnk.core.module.Module;

import java.util.List;

class DocumentFilterChainImpl implements DocumentFilterChain {

	private final Module.ModuleContext moduleContext;
	protected int filterIndex = 0;

	protected BaseController controller;

	public DocumentFilterChainImpl(Module.ModuleContext moduleContext, BaseController controller) {
		this.moduleContext = moduleContext;
		this.controller = controller;
	}

	@Override
	public Response doFilter(DocumentFilterContext context) {
		List<DocumentFilter> filters = moduleContext.getInstancesByType(DocumentFilter.class);
		if (filterIndex == filters.size()) {
			return controller.handle(context.getJsonPath(), context.getQueryAdapter(), context.getParameterProvider(),
					context.getRequestBody());
		} else {
			DocumentFilter filter = filters.get(filterIndex);
			filterIndex++;
			return filter.filter(context, this);
		}
	}
}