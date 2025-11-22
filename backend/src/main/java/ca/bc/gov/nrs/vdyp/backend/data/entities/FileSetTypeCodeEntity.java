package ca.bc.gov.nrs.vdyp.backend.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "file_set_type_code")
@Getter
@Setter
public class FileSetTypeCodeEntity extends CodeTableEntity {
	@Id
	@Column(name = "file_set_type_code", length = 10, nullable = false)
	private String fileSetTypeCode;
}
