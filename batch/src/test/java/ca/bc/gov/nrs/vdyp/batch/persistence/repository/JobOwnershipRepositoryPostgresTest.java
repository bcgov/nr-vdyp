package ca.bc.gov.nrs.vdyp.batch.persistence.repository;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
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
		jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("DROP TABLE IF EXISTS batch_job_claim");
		jdbcTemplate.execute("""
				CREATE TABLE batch_job_claim (
					projection_guid UUID PRIMARY KEY,
					owner_id VARCHAR(512) NOT NULL,
					lease_token_guid UUID NOT NULL,
					acquired_time TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
					lease_expiry_time TIMESTAMPTZ NOT NULL,
					version BIGINT NOT NULL DEFAULT 0
				)
				""");
		repository = new JobOwnershipRepository(jdbcTemplate);
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
	void expiredClaimIsTakenOverOnceAndReplacesLeaseToken() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobClaim first = repository.acquire(projectionGuid, "owner-a", UUID.randomUUID(), Duration.ofMillis(100))
				.orElseThrow();
		await().atMost(250, TimeUnit.MILLISECONDS).until(() -> true);

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
	void staleOwnerCannotRenewOrReleaseAfterTakeover() throws Exception {
		String projectionGuid = UUID.randomUUID().toString();
		JobClaim first = repository.acquire(projectionGuid, "owner-a", UUID.randomUUID(), Duration.ofMillis(100))
				.orElseThrow();
		await().atMost(250, TimeUnit.MILLISECONDS).until(() -> true);
		JobClaim second = repository.acquire(projectionGuid, "owner-b", UUID.randomUUID(), Duration.ofMinutes(1))
				.orElseThrow();

		assertFalse(repository.renew(first, Duration.ofMinutes(1)));
		assertFalse(repository.release(first));
		assertTrue(repository.isCurrent(second));
	}
}
