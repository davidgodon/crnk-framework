package io.crnk.servlet.resource;

import io.crnk.core.boot.CrnkBoot;
import io.crnk.servlet.AsyncCrnkServlet;
import io.crnk.test.mock.TestModule;
import org.junit.Ignore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@RestController
@SpringBootApplication
@Ignore
public class ServletTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServletTestApplication.class, args);
	}

	@Bean
	public AsyncCrnkServlet springBootSampleCrnkFilter() {
		AsyncCrnkServlet servlet = new AsyncCrnkServlet();
		servlet.getBoot().addModule(new TestModule());
		return servlet;
	}

	@Bean
	public CrnkBoot crnkBoot(AsyncCrnkServlet servlet) {
		return servlet.getBoot();
	}
}
