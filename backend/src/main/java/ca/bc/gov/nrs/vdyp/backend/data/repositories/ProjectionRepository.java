package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionRepository implements PanacheRepositoryBase<ProjectionEntity, UUID> {
	public List<ProjectionEntity> findByOwner(UUID vdypUserGUID) {
		Sort sort = Sort.by("updateDate").descending();
		return list("ownerUser.vdypUserGUID = ?1", sort, vdypUserGUID);
	}

	public long countUsesFileSet(UUID fileSetGUID) {
		// Check for this fileSet in polygon or layer file sets in any projection
		// Potential improvement, could remove result file set, confident we shouldn't share that one under any
		// circumstance
		return count(
				"polygonFileSet.projectionFileSetGUID = :fileSet " //
						+ "OR layerFileSet.projectionFileSetGUID = :fileSet " //
						+ "OR resultFileSet.projectionFileSetGUID = :fileSet", //
				Parameters.with("fileSet", fileSetGUID)
		);
	}

	public List<UUID> findExpiredIDs(int limit, int daysUntilExpiry) {
		OffsetDateTime expiryTime = OffsetDateTime.now().minusDays(daysUntilExpiry);
		return find("updateDate < ?1 order by updateDate", expiryTime) //
				.page(0, limit)//
				.stream().map(ProjectionEntity::getProjectionGUID).toList();
	}

	public long countCopyTitle(String title) {
		String titlePattern = "%\"copyTitle\" : \"" + title + "\"%";
		return count("projectionParameters like :titlePattern", Parameters.with("titlePattern", titlePattern));
	}
}
