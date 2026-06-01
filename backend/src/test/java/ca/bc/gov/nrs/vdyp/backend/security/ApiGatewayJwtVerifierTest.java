package ca.bc.gov.nrs.vdyp.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;

class ApiGatewayJwtVerifierTest {

	private static final String AUDIENCE = "nr-vdyp-test-backend";

	private static final String ISSUER = "https://aps-jwks-upstream-jwt-api-gov-bc-ca.test.api.gov.bc.ca";

	private KeyPair gatewayKey;

	private ApiGatewayJwtVerifier verifier;

	@BeforeEach
	void setUp() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		gatewayKey = keyPairGenerator.generateKeyPair();
		verifier = new ApiGatewayJwtVerifier(AUDIENCE, ISSUER, gatewayKey.getPublic(), new DefaultJWTParser());
	}

	@Test
	void testVerifyAcceptsValidGatewayJwt() throws Exception {
		String token = gatewayJwt(AUDIENCE, ISSUER, Instant.now().plusSeconds(300), SignatureAlgorithm.RS256);

		JsonWebToken claims = verifier.verify(token);

		assertEquals(ISSUER, claims.getIssuer());
		assertTrue(claims.getAudience().contains(AUDIENCE));
	}

	@Test
	void testVerifyRejectsWrongAudience() {
		String token = gatewayJwt("other-service", ISSUER, Instant.now().plusSeconds(300), SignatureAlgorithm.RS256);

		assertThrows(ParseException.class, () -> verifier.verify(token));
	}

	@Test
	void testVerifyRejectsWrongIssuer() {
		String token = gatewayJwt(
				AUDIENCE, "https://issuer.example", Instant.now().plusSeconds(300), SignatureAlgorithm.RS256
		);

		assertThrows(ParseException.class, () -> verifier.verify(token));
	}

	@Test
	void testVerifyRejectsExpiredJwt() {
		String token = gatewayJwt(AUDIENCE, ISSUER, Instant.now().minusSeconds(300), SignatureAlgorithm.RS256);

		assertThrows(ParseException.class, () -> verifier.verify(token));
	}

	@Test
	void testVerifyRejectsJwtSignedWithUnexpectedAlgorithm() {
		String token = gatewayJwt(AUDIENCE, ISSUER, Instant.now().plusSeconds(300), SignatureAlgorithm.RS512);

		assertThrows(ParseException.class, () -> verifier.verify(token));
	}

	@Test
	void testVerifySkipsJwtParsingWhenVerificationDisabled() throws Exception {
		ApiGatewayJwtVerifier disabledVerifier = new ApiGatewayJwtVerifier(
				AUDIENCE, ISSUER, gatewayKey.getPublic(), new DefaultJWTParser(), false
		);

		JsonWebToken claims = disabledVerifier.verify("not-a-jwt");

		assertNull(claims);
	}

	@Test
	void testConstructorRejectsBlankAudience() {
		assertThrows(
				IllegalStateException.class,
				() -> new ApiGatewayJwtVerifier("  ", ISSUER, gatewayKey.getPublic(), new DefaultJWTParser())
		);
	}

	private String gatewayJwt(String audience, String issuer, Instant expiresAt, SignatureAlgorithm algorithm) {
		return Jwt.issuer(issuer).audience(audience).subject("gateway").expiresAt(expiresAt).jws().algorithm(algorithm)
				.keyId("gateway-key").sign(gatewayKey.getPrivate());
	}
}
