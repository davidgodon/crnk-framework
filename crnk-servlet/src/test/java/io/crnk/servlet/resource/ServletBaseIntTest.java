package io.crnk.servlet.resource;

import static org.junit.Assert.assertEquals;

import io.crnk.core.boot.CrnkBoot;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServletTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class ServletBaseIntTest {

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private CrnkBoot boot;


	@Test
	public void testTestEndpointWithQueryParams() {
		RestTemplate testRestTemplate = new RestTemplate();
		ResponseEntity<String> response = testRestTemplate
				.getForEntity("http://localhost:" + this.port + "/", String.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		//assertThatJson(response.getBody()).node("data").isPresent();
	}

}