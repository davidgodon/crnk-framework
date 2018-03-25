package io.crnk.servlet.resource;

import io.crnk.core.boot.CrnkBoot;
import io.crnk.core.engine.result.ResultFactory;
import io.crnk.reactive.internal.MonoResultFactory;
import io.crnk.servlet.AsyncCrnkServlet;
import io.crnk.test.mock.TestModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@RestController
@SpringBootApplication
public class ServletTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServletTestApplication.class, args);
	}

	@Bean
	public AsyncCrnkServlet servlet() {
		ResultFactory resultFactory = new MonoResultFactory();
		AsyncCrnkServlet servlet = new AsyncCrnkServlet();
		servlet.getBoot().getModuleRegistry().setResultFactory(resultFactory);
		servlet.getBoot().addModule(new TestModule());
		return servlet;
	}

	@Bean
	public ServletRegistrationBean exampleServletBean(AsyncCrnkServlet servlet) {
		ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/api/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public CrnkBoot crnkBoot(AsyncCrnkServlet servlet) {
		return servlet.getBoot();
	}
}
