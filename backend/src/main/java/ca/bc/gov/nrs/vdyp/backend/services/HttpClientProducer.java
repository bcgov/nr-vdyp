package ca.bc.gov.nrs.vdyp.backend.services;

import java.net.http.HttpClient;
import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class HttpClientProducer {

	@Produces
	@ApplicationScoped
	public HttpClient httpClient() {
		return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(10)).build();
	}
}
