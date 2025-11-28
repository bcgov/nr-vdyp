package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import ca.bc.gov.nrs.vdyp.backend.data.entities.UserTypeCodeEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserTypeCodeRepository implements PanacheRepositoryBase<UserTypeCodeEntity, String> {

}
