package ca.bc.gov.nrs.vdyp.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class ApiGatewayJwtVerifierTest {

	private static final String AUDIENCE = "nr-vdyp-test-backend";

	private static final String ISSUER = "https://aps-jwks-upstream-jwt-api-gov-bc-ca.test.api.gov.bc.ca";

	private RSAKey gatewayKey;

	private ApiGatewayJwtVerifier verifier;

	@BeforeEach
	void setUp() throws JOSEException {
		gatewayKey = new RSAKeyGenerator(2048).keyID("gateway-key").generate();
		verifier = new ApiGatewayJwtVerifier(
				AUDIENCE, ISSUER, new ImmutableJWKSet<>(new JWKSet(gatewayKey.toPublicJWK()))
		);
	}

	@Test
	void testVerifyAcceptsValidGatewayJwt() throws Exception {
		String token = gatewayJwt(AUDIENCE, ISSUER, Date.from(Instant.now().plusSeconds(300)), JWSAlgorithm.RS256);

		JWTClaimsSet claims = verifier.verify(token);

		assertEquals(ISSUER, claims.getIssuer());
		assertEquals(AUDIENCE, claims.getAudience().get(0));
	}

	@Test
	void testVerifyRejectsWrongAudience() throws Exception {
		String token = gatewayJwt(
				"other-service", ISSUER, Date.from(Instant.now().plusSeconds(300)), JWSAlgorithm.RS256
		);

		assertThrows(BadJOSEException.class, () -> verifier.verify(token));
	}

	@Test
	void testVerifyRejectsWrongIssuer() throws Exception {
		String token = gatewayJwt(
				AUDIENCE, "https://issuer.example", Date.from(Instant.now().plusSeconds(300)), JWSAlgorithm.RS256
		);

		assertThrows(BadJOSEException.class, () -> verifier.verify(token));
	}

	@Test
	void testVerifyRejectsExpiredJwt() throws Exception {
		String token = gatewayJwt(AUDIENCE, ISSUER, Date.from(Instant.now().minusSeconds(300)), JWSAlgorithm.RS256);

		assertThrows(BadJOSEException.class, () -> verifier.verify(token));
	}

	@Test
	void testVerifyRejectsJwtSignedWithUnexpectedAlgorithm() throws Exception {
		String token = gatewayJwt(AUDIENCE, ISSUER, Date.from(Instant.now().plusSeconds(300)), JWSAlgorithm.RS512);

		assertThrows(BadJOSEException.class, () -> verifier.verify(token));
	}

	@Test
	void testConstructorRejectsBlankAudience() {
		assertThrows(
				IllegalStateException.class,
				() -> new ApiGatewayJwtVerifier("  ", ISSUER, new ImmutableJWKSet<>(new JWKSet()))
		);
	}

	private String gatewayJwt(String audience, String issuer, Date expiresAt, JWSAlgorithm algorithm)
			throws JOSEException {
		JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject("gateway")
				.expirationTime(expiresAt).build();
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(gatewayKey.getKeyID()).build(), claims);
		jwt.sign(new RSASSASigner(gatewayKey));
		return jwt.serialize();
	}
}
