package ca.bc.gov.nrs.vdyp.batch.persistence.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ca.bc.gov.nrs.vdyp.batch.persistence.model.JobClaim;

@Repository
public class JobOwnershipRepository {

	private final JdbcTemplate jdbcTemplate;

	public JobOwnershipRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<JobClaim> acquire(String projectionGuid, String ownerId, UUID leaseToken, Duration leaseDuration) {
		String sql = """
				INSERT INTO batch_job_claim (
				    projection_guid, owner_id, lease_token_guid, acquired_time, lease_expiry_time
				)
				VALUES (?::uuid, ?, ?::uuid, clock_timestamp(), clock_timestamp() + (? * interval '1 millisecond'))
				ON CONFLICT (projection_guid) DO UPDATE
				SET owner_id = EXCLUDED.owner_id,
				    lease_token_guid = EXCLUDED.lease_token_guid,
				    acquired_time = clock_timestamp(),
				    lease_expiry_time = clock_timestamp() + (? * interval '1 millisecond'),
				    version = batch_job_claim.version + 1
				WHERE batch_job_claim.lease_expiry_time <= clock_timestamp()
				    OR (batch_job_claim.owner_id = ? AND batch_job_claim.lease_token_guid = ?::uuid)
				RETURNING projection_guid::text AS projection_guid, owner_id, lease_token_guid, acquired_time, lease_expiry_time
				""";
		List<JobClaim> claims = jdbcTemplate.query(
				sql, this::mapClaim, projectionGuid, ownerId, leaseToken, leaseDuration.toMillis(),
				leaseDuration.toMillis(), ownerId, leaseToken
		);
		return claims.stream().findFirst();
	}

	public boolean renew(JobClaim claim, Duration leaseDuration) {
		String sql = """
				UPDATE batch_job_claim
				SET lease_expiry_time = clock_timestamp() + (? * interval '1 millisecond'),
					version = version + 1
				WHERE projection_guid = ?::uuid
					AND owner_id = ?
					AND lease_token_guid = ?::uuid
				""";
		return jdbcTemplate.update(
				sql, leaseDuration.toMillis(), claim.projectionGuid(), claim.ownerId(), claim.leaseToken()
		) == 1;
	}

	public boolean release(JobClaim claim) {
		String sql = """
				DELETE FROM batch_job_claim
				WHERE projection_guid = ?::uuid
					AND owner_id = ?
					AND lease_token_guid = ?::uuid
				""";
		return jdbcTemplate.update(sql, claim.projectionGuid(), claim.ownerId(), claim.leaseToken()) == 1;
	}

	public boolean isCurrent(JobClaim claim) {
		String sql = """
				SELECT count(*)
				FROM batch_job_claim
				WHERE projection_guid = ?::uuid
					AND owner_id = ?
					AND lease_token_guid = ?::uuid
					AND lease_expiry_time > clock_timestamp()
				""";
		Integer count = jdbcTemplate
				.queryForObject(sql, Integer.class, claim.projectionGuid(), claim.ownerId(), claim.leaseToken());
		return count != null && count == 1;
	}

	public Optional<JobClaim> findByProjectionGuid(String projectionGuid) {
		String sql = """
				SELECT projection_guid::text AS projection_guid, owner_id, lease_token_guid, acquired_time, lease_expiry_time
				FROM batch_job_claim
				WHERE projection_guid = ?::uuid
				""";
		List<JobClaim> claims = jdbcTemplate.query(sql, this::mapClaim, projectionGuid);
		return claims.stream().findFirst();
	}

	public long countActiveClaims() {
		String sql = """
				SELECT count(*)
				FROM batch_job_claim
				WHERE lease_expiry_time > clock_timestamp()
				""";
		Long count = jdbcTemplate.queryForObject(sql, Long.class);
		return count == null ? 0 : count;
	}

	private JobClaim mapClaim(ResultSet rs, int rowNum) throws SQLException {
		return new JobClaim(
				rs.getString("projection_guid"), rs.getString("owner_id"),
				UUID.fromString(rs.getString("lease_token_guid")), rs.getTimestamp("acquired_time").toInstant(),
				rs.getTimestamp("lease_expiry_time").toInstant()
		);
	}
}
