package ca.bc.gov.nrs.vdyp.batch.persistence.repository;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;

@Testcontainers
class JobOwnershipRepositoryPostgresTest {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	private JdbcTemplate jdbcTemplate;
	private JobOwnershipRepository repository;

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
		jdbcTemplate.execute("DROP TABLE IF EXISTS batch_job_claim");
		jdbcTemplate.execute("""
				CREATE TABLE batch_job_claim (
					projection_guid UUID PRIMARY KEY,
					owner_id VARCHAR(512) NOT NULL,
					lease_token_guid UUID NOT NULL,
					acquired_time TIMESTAMP NOT NULL DEFAULT (clock_timestamp() AT TIME ZONE 'UTC'),
					lease_expiry_time TIMESTAMP NOT NULL,
					version BIGINT NOT NULL DEFAULT 0
				)
				""");
		repository = new JobOwnershipRepository(jdbcTemplate);
	}

	@Test
	void claimTimesAreUtcRegardlessOfDatabaseSessionZone() {
		Instant before = databaseNow();
		JobClaim claim = repository
				.acquire(UUID.randomUUID().toString(), "owner-a", UUID.randomUUID(), Duration.ofMinutes(1))
				.orElseThrow();
		Instant after = databaseNow();
		assertFalse(claim.acquiredTime().isBefore(before));
		assertFalse(claim.acquiredTime().isAfter(after));
		assertTrue(claim.leaseExpiryTime().isAfter(after));
		assertEquals(claim, repository.findByProjectionGuid(claim.projectionGuid()).orElseThrow());
	}

	private Instant databaseNow() {
		return jdbcTemplate.queryForObject(
				"SELECT clock_timestamp()", (rs, rowNum) -> rs.getObject(1, OffsetDateTime.class).toInstant()
		);
	}

	@Test
	void manyContendersForAbsentClaimExactlyOneSucceeds() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		int contenders = 12;
		CountDownLatch ready = new CountDownLatch(contenders);
		CountDownLatch start = new CountDownLatch(1);
		var executor = Executors.newFixedThreadPool(contenders);
		try {
			var tasks = IntStream.range(0, contenders).mapToObj(i -> (Callable<Optional<JobClaim>>) () -> {
				ready.countDown();
				start.await();
				return repository.acquire(projectionGuid, "owner-" + i, UUID.randomUUID(), Duration.ofMinutes(1));
			}).toList();
			var futures = tasks.stream().map(executor::submit).toList();
			ready.await();
			start.countDown();

			long successes = 0;
			for (var future : futures) {
				if (future.get().isPresent()) {
					successes++;
				}
			}

			assertEquals(1, successes);
			assertEquals(1L, repository.countActiveClaims());
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void activeUnexpiredClaimCannotBeStolen() {
		String projectionGuid = UUID.randomUUID().toString();
		JobClaim first = repository.acquire(projectionGuid, "owner-a", UUID.randomUUID(), Duration.ofMinutes(1))
				.orElseThrow();

		Optional<JobClaim> stolen = repository
				.acquire(projectionGuid, "owner-b", UUID.randomUUID(), Duration.ofMinutes(1));

		assertFalse(stolen.isPresent());
		assertTrue(repository.isCurrent(first));
	}

	@Test
	void expiredClaimIsTakenOverOnceAndReplacesLeaseToken() {
		String projectionGuid = UUID.randomUUID().toString();
		JobClaim first = repository.acquire(projectionGuid, "owner-a", UUID.randomUUID(), Duration.ofMillis(100))
				.orElseThrow();
		await().atMost(2, TimeUnit.SECONDS).until(() -> !repository.isCurrent(first));

		JobClaim second = repository.acquire(projectionGuid, "owner-b", UUID.randomUUID(), Duration.ofMinutes(1))
				.orElseThrow();
		Optional<JobClaim> third = repository
				.acquire(projectionGuid, "owner-c", UUID.randomUUID(), Duration.ofMinutes(1));

		assertFalse(third.isPresent());
		assertFalse(repository.isCurrent(first));
		assertTrue(repository.isCurrent(second));
		assertNotEquals(first.leaseToken(), second.leaseToken());
	}

	@Test
	void releasedClaimCanBeAcquiredByAnotherOwner() {
		String projectionGuid = UUID.randomUUID().toString();
		JobClaim first = repository.acquire(projectionGuid, "owner-a", UUID.randomUUID(), Duration.ofMinutes(1))
				.orElseThrow();

		assertTrue(repository.release(first));

		JobClaim second = repository.acquire(projectionGuid, "owner-b", UUID.randomUUID(), Duration.ofMinutes(1))
				.orElseThrow();
		assertTrue(repository.isCurrent(second));
	}

	@Test
	void staleOwnerCannotRenewOrReleaseAfterTakeover() {
		String projectionGuid = UUID.randomUUID().toString();
		JobClaim first = repository.acquire(projectionGuid, "owner-a", UUID.randomUUID(), Duration.ofMillis(100))
				.orElseThrow();
		await().atMost(2, TimeUnit.SECONDS).until(() -> !repository.isCurrent(first));
		JobClaim second = repository.acquire(projectionGuid, "owner-b", UUID.randomUUID(), Duration.ofMinutes(1))
				.orElseThrow();

		assertFalse(repository.renew(first, Duration.ofMinutes(1)));
		assertFalse(repository.release(first));
		assertTrue(repository.isCurrent(second));
	}
}
