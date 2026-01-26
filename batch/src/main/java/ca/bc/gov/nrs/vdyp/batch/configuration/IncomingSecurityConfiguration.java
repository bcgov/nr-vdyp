package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class IncomingSecurityConfiguration {
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(a -> a.anyRequest().permitAll()).oauth2Login(o -> o.disable())
				.formLogin(f -> f.disable()).httpBasic(b -> b.disable());
		return http.build();
	}
}
