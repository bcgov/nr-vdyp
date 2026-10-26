package ca.bc.gov.nrs.vdyp.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

class UserRolesAugmentorTest {

	private final UserRolesAugmentor augmentor = new UserRolesAugmentor();

	@ParameterizedTest
	@ValueSource(
			strings = { "AZUREIDIR", "azureidir", "AzureIDIR", "BCEIDBUSINESS", "bceidbusiness", "BCeIDBusiness",
					"VDYP-SERVICE-6216", "vdyp-service-6216", "Vdyp-Service-6216" }
	)
	void testAddsUserRoleForSupportedProviders(String provider) {
		SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(jwt(provider)).build();

		SecurityIdentity result = augment(identity);

		assertEquals(Set.of("USER"), result.getRoles());
		assertTrue(identity.getRoles().isEmpty());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "unknown-idp", "   ", "azureidir-other" })
	void testDoesNotAddUserRoleForMissingOrUnsupportedProviders(String provider) {
		SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(jwt(provider)).addRole("ADMIN")
				.build();

		SecurityIdentity result = augment(identity);

		assertEquals(Set.of("ADMIN"), result.getRoles());
	}

	@Test
	void testPreservesNonJwtIdentity() {
		Principal principal = () -> "gateway-consumer";
		SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(principal)
				.addRole("KONG_API_GATEWAY").build();

		SecurityIdentity result = augment(identity);

		assertSame(principal, result.getPrincipal());
		assertEquals(Set.of("KONG_API_GATEWAY"), result.getRoles());
		assertFalse(result.isAnonymous());
	}

	@Test
	void testPreservesAnonymousIdentity() {
		SecurityIdentity identity = QuarkusSecurityIdentity.builder().setAnonymous(true).build();

		SecurityIdentity result = augment(identity);

		assertTrue(result.isAnonymous());
		assertTrue(result.getRoles().isEmpty());
		assertSame(identity.getPrincipal(), result.getPrincipal());
	}

	@Test
	void testPreservesExistingIdentityDataWhenAddingUserRole() {
		JsonWebToken principal = jwt("azureidir");
		TokenCredential credential = new TokenCredential("test-token", "bearer");
		SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(principal).addRole("ADMIN")
				.addAttribute("auth_source", "oidc").addCredential(credential).build();

		SecurityIdentity result = augment(identity);

		assertSame(principal, result.getPrincipal());
		assertEquals(Set.of("ADMIN", "USER"), result.getRoles());
		assertEquals(identity.getAttributes(), result.getAttributes());
		assertSame(credential, result.getCredential(TokenCredential.class));
		assertFalse(result.isAnonymous());
		assertEquals(Set.of("ADMIN"), identity.getRoles());
	}

	@Test
	void testDoesNotDuplicateExistingUserRole() {
		SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(jwt("azureidir")).addRole("USER")
				.build();

		SecurityIdentity result = augment(identity);

		assertEquals(Set.of("USER"), result.getRoles());
	}

	private SecurityIdentity augment(SecurityIdentity identity) {
		return augmentor.augment(identity, null).await().indefinitely();
	}

	private JsonWebToken jwt(String provider) {
		JsonWebToken jwt = mock(JsonWebToken.class);
		when(jwt.getClaim("identity_provider")).thenReturn(provider);
		return jwt;
	}
}
