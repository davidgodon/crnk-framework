package io.crnk.reactive;

import java.util.ArrayList;
import java.util.List;

import io.crnk.core.module.Module;
import io.crnk.reactive.internal.http.ReactiveJsonApiRequestProcessor;

public class ReactiveModule implements Module {

	private List<ReactiveJsonApiRequestProcessor> processors = new ArrayList<>();

	@Override
	public String getModuleName() {
		return "reactive";
	}

	@Override
	public void setupModule(ModuleContext context) {
		//	processors.add(new ReactiveJsonApiRequestProcessor(context));


	}
}
