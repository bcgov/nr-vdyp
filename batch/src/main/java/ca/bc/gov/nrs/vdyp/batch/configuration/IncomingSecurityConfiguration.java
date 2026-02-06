package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class IncomingSecurityConfiguration {
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/batch/startWithGUIDs", "/api/batch/stop/**", "/api/batch/status/**"))
				.authorizeHttpRequests(a -> a.anyRequest().permitAll())//
				.oauth2Login(AbstractHttpConfigurer::disable)//
				.formLogin(AbstractHttpConfigurer::disable)//
				.httpBasic(AbstractHttpConfigurer::disable);
		return http.build();
	}
}
