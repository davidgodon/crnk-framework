package io.crnk.reactive;

import io.crnk.core.module.Module;

public class ReactiveModule implements Module {


	@Override
	public String getModuleName() {
		return "reactive";
	}

	@Override
	public void setupModule(ModuleContext context) {
		//	processors.add(new ReactiveJsonApiRequestProcessor(context));


	}
}
