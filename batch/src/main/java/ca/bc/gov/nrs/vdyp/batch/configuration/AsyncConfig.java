package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "backendProgressExecutor")
	public ThreadPoolTaskExecutor backendProgressExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(6);
		ex.setQueueCapacity(12);
		ex.setThreadNamePrefix("backend-progress-");
		ex.initialize();
		return ex;
	}
}
