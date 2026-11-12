package ca.bc.gov.nrs.vdyp.batch.persistence.repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
		Properties connectionProperties = new Properties();
		connectionProperties.setProperty("options", "-c TimeZone=America/New_York");
		dataSource.setConnectionProperties(connectionProperties);
		jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("DROP TABLE IF EXISTS batch_worker_registry");
		jdbcTemplate.execute("""
				        CREATE TABLE "batch_worker_registry" (
				        	"worker_id" VARCHAR(512) PRIMARY KEY,
				        	"num_max_threads" INTEGER NOT NULL,
				        	"is_accepting_work" BOOLEAN NOT NULL,
				        	"last_heartbeat_time" TIMESTAMP NOT NULL
				        )
				""");
		repository = new ServerCapacityRepository(jdbcTemplate);
	}

	@Test
	void heartbeatIsUtcRegardlessOfDatabaseSessionZone() {
		LocalDateTime before = databaseNow();
		repository.recordCapacityHeartbeat("owner1", 5, true);
		LocalDateTime heartbeat = jdbcTemplate.queryForObject(
				"SELECT last_heartbeat_time FROM batch_worker_registry WHERE worker_id = 'owner1'",
				(rs, rowNum) -> rs.getObject(1, LocalDateTime.class)
		);
		Assertions.assertFalse(heartbeat.isBefore(before));
		Assertions.assertFalse(heartbeat.isAfter(databaseNow()));
		Assertions.assertEquals(5L, repository.getAggregateCapacity(10));
	}

	private LocalDateTime databaseNow() {
		return jdbcTemplate.queryForObject(
				"SELECT clock_timestamp()",
				(rs, rowNum) -> rs.getObject(1, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC)
						.toLocalDateTime()
		);
	}

	@Test
	void singleOwnerReportedCapacityIsAggregateCapacity() {
		String ownerId = "owner1";
		Long threadCapacity = 5L;
		Boolean acceptingWork = true;

		repository.recordCapacityHeartbeat(ownerId, threadCapacity.intValue(), acceptingWork);

		Long aggregateCapacity = repository.getAggregateCapacity(10);
		Assertions.assertEquals(threadCapacity, aggregateCapacity);
	}

	@Test
	void multipleOwnerReportedCapacityIsAggregateCapacity() {
		String ownerId = "owner1";
		String ownerId2 = "owner2";
		Long threadCapacity = 5L;
		Long threadCapacity2 = 7L;
		Boolean acceptingWork = true;

		repository.recordCapacityHeartbeat(ownerId, threadCapacity.intValue(), acceptingWork);
		repository.recordCapacityHeartbeat(ownerId2, threadCapacity2.intValue(), acceptingWork);

		Long aggregateCapacity = repository.getAggregateCapacity(10);
		Assertions.assertEquals(threadCapacity + threadCapacity2, aggregateCapacity);
	}

	@Test
	void deleteStaleWorkersRemovesOnlyEntriesOlderThanMaxAge() {
		jdbcTemplate.update("""
				INSERT INTO batch_worker_registry (worker_id, num_max_threads, is_accepting_work, last_heartbeat_time)
				VALUES ('stale', 4, true, (clock_timestamp() AT TIME ZONE 'UTC') - interval '2 days')
				""");
		repository.recordCapacityHeartbeat("fresh", 5, true);

		int deleted = repository.deleteStaleWorkers(86400);

		Assertions.assertEquals(1, deleted);
		List<String> remaining = jdbcTemplate.queryForList("SELECT worker_id FROM batch_worker_registry", String.class);
		Assertions.assertEquals(List.of("fresh"), remaining);
	}

}
