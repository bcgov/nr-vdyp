package ca.bc.gov.nrs.vdyp.batch.client.coms;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ComsRestClientConfig {

	@Bean
	RestClient comsRestClient(ComsProperties props) {
		return RestClient.builder().baseUrl(props.baseUrl()).defaultHeaders(h -> {
			h.setBasicAuth(props.basicAuth().username(), props.basicAuth().password());

		}).build();
	}
}
