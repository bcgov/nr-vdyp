package ca.bc.gov.nrs.vdyp.backend.data.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

@ExtendWith(MockitoExtension.class)
class FileMappingRepositoryTest {

	@Mock
	EntityManager em;
	@Mock
	TypedQuery<UUID> query;

	FileMappingRepository repository;

	@BeforeEach
	void setUp() {
		repository = spy(new FileMappingRepository());
	}

	@Test
	void findFileSetGUIDsWithFiles_emptyInput_returnsEmptyWithoutQuery() {
		assertThat(repository.findFileSetGUIDsWithFiles(List.of())).isEmpty();

		verify(repository, never()).getEntityManager();
	}

	@Test
	void findFileSetGUIDsWithFiles_returnsDistinctFileSetGUIDsFromQuery() {
		UUID withFile = UUID.randomUUID();
		UUID withoutFile = UUID.randomUUID();
		List<UUID> fileSetGUIDs = List.of(withFile, withoutFile);

		doReturn(em).when(repository).getEntityManager();
		when(em.createQuery(anyString(), eq(UUID.class))).thenReturn(query);
		when(query.setParameter("fileSetGUIDs", fileSetGUIDs)).thenReturn(query);
		when(query.getResultList()).thenReturn(List.of(withFile));

		assertThat(repository.findFileSetGUIDsWithFiles(fileSetGUIDs)).containsExactly(withFile);

		verify(query).setParameter("fileSetGUIDs", fileSetGUIDs);
	}
}
