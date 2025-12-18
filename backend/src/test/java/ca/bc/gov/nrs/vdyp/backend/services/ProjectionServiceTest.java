package ca.bc.gov.nrs.vdyp.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.bc.gov.nrs.vdyp.backend.data.assemblers.ProjectionResourceAssembler;
import ca.bc.gov.nrs.vdyp.backend.data.entities.ProjectionEntity;
import ca.bc.gov.nrs.vdyp.backend.data.models.ProjectionModel;
import ca.bc.gov.nrs.vdyp.backend.data.repositories.ProjectionRepository;

@ExtendWith(MockitoExtension.class)
class ProjectionServiceTest {
	@Mock
	ProjectionRepository repository;
	ProjectionResourceAssembler assembler;

	ProjectionService service;

	@BeforeEach
	void setUp() {
		assembler = new ProjectionResourceAssembler();

		service = new ProjectionService(assembler, repository);
	}

	@Test
	void getAllProjectionsForUser_throwsException_ForInvalidGUID() {
		assertThrows(IllegalArgumentException.class, () -> service.getAllProjectionsForUser("INVALIDGUID"));
	}

	@Test
	void getAllProjectionsForUser_returnsEmpty_ForNull() {
		List<ProjectionModel> results = service.getAllProjectionsForUser(null);

		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	@Test
	void getAllProjectionsForUser_returnsEmpty_ForUserWithoutRecords() {
		UUID doesNotExist = UUID.randomUUID();
		when(repository.findByOwner(doesNotExist)).thenReturn(Collections.emptyList());

		List<ProjectionModel> results = service.getAllProjectionsForUser(doesNotExist.toString());

		assertNotNull(results);
		assertTrue(results.isEmpty());
		verify(repository).findByOwner(doesNotExist);
	}

	@Test
	void getAllProjectionsForUser_returnsList_forUserWithRecords() {
		UUID doesNotExist = UUID.randomUUID();

		ProjectionEntity entityResult = new ProjectionEntity();
		entityResult.setProjectionGUID(UUID.randomUUID());
		entityResult.setReportTitle("Test Projection");
		entityResult.setReportDescription("Test Description");

		when(repository.findByOwner(doesNotExist)).thenReturn(List.of(entityResult));

		List<ProjectionModel> results = service.getAllProjectionsForUser(doesNotExist.toString());

		assertNotNull(results);
		assertThat(results).hasSize(1);

		// Validate fields were mapped through assembler
		ProjectionModel result = results.get(0);
		assertThat(result.getProjectionGUID()).isEqualTo(entityResult.getProjectionGUID().toString());
		assertThat(result.getReportTitle()).isEqualTo("Test Projection");
		assertThat(result.getReportDescription()).isEqualTo("Test Description");

		verify(repository).findByOwner(doesNotExist);
	}

}
