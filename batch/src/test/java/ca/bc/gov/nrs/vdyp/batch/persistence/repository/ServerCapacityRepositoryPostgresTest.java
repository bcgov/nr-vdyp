package ca.bc.gov.nrs.vdyp.batch.persistence.repository;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ServerCapacityRepositoryPostgresTest {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	private JdbcTemplate jdbcTemplate;
	private ServerCapacityRepository repository;

	@BeforeEach
	void setUp() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
		);
		dataSource.setDriverClassName("org.postgresql.Driver");
		jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("DROP TABLE IF EXISTS batch_worker_registry");
		jdbcTemplate.execute("""
				        CREATE TABLE "batch"."batch_worker_registry" (
				        	"worker_id" VARCHAR(512) PRIMARY KEY,
				        	"num_max_threads" INTEGER NOT NULL,
				        	"is_accepting_work" BOOLEAN NOT NULL,
				        	"last_heartbeat_time" TIMESTAMP NOT NULL
				        )
				""");
		repository = new ServerCapacityRepository(jdbcTemplate);
	}

}
