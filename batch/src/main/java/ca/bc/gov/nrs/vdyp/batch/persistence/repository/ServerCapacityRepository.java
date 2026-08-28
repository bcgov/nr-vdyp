package ca.bc.gov.nrs.vdyp.batch.persistence.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ServerCapacityRepository {

	private final JdbcTemplate jdbcTemplate;

	public ServerCapacityRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void recordCapacityHeartbeat(String ownerId, Integer threadCapacity, Boolean acceptingWork) {
		String sql = """
				INSERT INTO batch_worker_registry (
					worker_id, num_max_threads, is_accepting_work, last_heartbeat_time
				)
				VALUES (?, ?, ?, clock_timestamp())
				ON CONFLICT (worker_id) DO UPDATE
				SET num_max_threads = EXCLUDED.num_max_threads,
					is_accepting_work = EXCLUDED.is_accepting_work,
					last_heartbeat_time = clock_timestamp()
				""";
		jdbcTemplate.update(sql, ownerId, threadCapacity, acceptingWork);
	}

	public Long getAggregateCapacity(long heartbeatMaxAge) {
		String sql = """
				SELECT COALESCE(SUM(num_max_threads),0)
				FROM batch_worker_registry
				WHERE last_heartbeat_time >= clock_timestamp() - (? * interval '1 second')
				""";
		return jdbcTemplate.queryForObject(sql, Long.class, heartbeatMaxAge);
	}

}
