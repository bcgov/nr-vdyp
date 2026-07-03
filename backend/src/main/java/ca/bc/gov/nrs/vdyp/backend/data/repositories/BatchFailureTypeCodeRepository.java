package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import ca.bc.gov.nrs.vdyp.backend.data.entities.BatchFailureTypeCodeEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BatchFailureTypeCodeRepository implements PanacheRepositoryBase<BatchFailureTypeCodeEntity, String> {

}
