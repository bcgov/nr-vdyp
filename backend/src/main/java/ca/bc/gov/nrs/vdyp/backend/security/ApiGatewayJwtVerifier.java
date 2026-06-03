package ca.bc.gov.nrs.vdyp.backend.security;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ApiGatewayJwtVerifier {

	private static final Logger logger = LoggerFactory.getLogger(ApiGatewayJwtVerifier.class);
	private static final String AUDIENCE_PROPERTY = "vdyp.api-gateway.jwt.audience";

	private static final String VERIFICATION_ENABLED_PROPERTY = "vdyp.api-gateway.jwt.verification.enabled";

	private final JWTParser jwtParser;

	private final JWTAuthContextInfo jwtAuthContextInfo;

	private final boolean verificationEnabled;

	@Inject
	public ApiGatewayJwtVerifier(
			@ConfigProperty(name = "vdyp.api-gateway.jwt.jwks-uri") String jwksUri,
			@ConfigProperty(name = AUDIENCE_PROPERTY) String audience,
			@ConfigProperty(name = "vdyp.api-gateway.jwt.issuer") String issuer,
			@ConfigProperty(name = VERIFICATION_ENABLED_PROPERTY, defaultValue = "true") boolean verificationEnabled,
			JWTParser jwtParser
	) {
		this(
				jwtParser,
				createJwtAuthContextInfo(parseRequiredCsv(AUDIENCE_PROPERTY, audience), issuer, toUrlString(jwksUri)),
				verificationEnabled
		);
	}

	ApiGatewayJwtVerifier(String audience, String issuer, PublicKey publicKey, JWTParser jwtParser) {
		this(audience, issuer, publicKey, jwtParser, true);
	}

	ApiGatewayJwtVerifier(
			String audience, String issuer, PublicKey publicKey, JWTParser jwtParser, boolean verificationEnabled
	) {
		this(
				jwtParser, createJwtAuthContextInfo(parseRequiredCsv(AUDIENCE_PROPERTY, audience), issuer, publicKey),
				verificationEnabled
		);
	}

	private ApiGatewayJwtVerifier(
			JWTParser jwtParser, JWTAuthContextInfo jwtAuthContextInfo, boolean verificationEnabled
	) {
		this.jwtParser = jwtParser;
		this.jwtAuthContextInfo = jwtAuthContextInfo;
		this.verificationEnabled = verificationEnabled;
	}

	public JsonWebToken verify(String token) throws ParseException {
		if (!verificationEnabled) {
			return null;
		}
		String[] parts = token.split("\\.");
		String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
		String claimsJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

		logger.info("Verify JWT header: {}", headerJson);
		logger.info("Verify JWT claims: {}", claimsJson);
		logger.debug("Test Debug output");
		logger.info(
				"against Auth Context Info: " + jwtAuthContextInfo.getIssuedBy() + ", "
						+ jwtAuthContextInfo.getExpectedAudience() + ", " + jwtAuthContextInfo.getPublicKeyLocation()
						+ ", " + jwtAuthContextInfo.getPublicVerificationKey()
		);
		return jwtParser.parse(token, jwtAuthContextInfo);
	}

	private static JWTAuthContextInfo createJwtAuthContextInfo(Set<String> audiences, String issuer, String jwksUri) {
		JWTAuthContextInfo authContextInfo = createJwtAuthContextInfo(audiences, issuer);
		authContextInfo.setPublicKeyLocation(jwksUri);
		return authContextInfo;
	}

	private static JWTAuthContextInfo
			createJwtAuthContextInfo(Set<String> audiences, String issuer, PublicKey publicKey) {
		JWTAuthContextInfo authContextInfo = createJwtAuthContextInfo(audiences, issuer);
		authContextInfo.setPublicVerificationKey(publicKey);
		return authContextInfo;
	}

	private static JWTAuthContextInfo createJwtAuthContextInfo(Set<String> audiences, String issuer) {
		JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
		String normalizedIssuer = normalize(issuer);
		if (normalizedIssuer == null) {
			throw new IllegalStateException("issuer must be set");
		}

		authContextInfo.setIssuedBy(normalizedIssuer);
		authContextInfo.setExpectedAudience(audiences);
		authContextInfo.setSignatureAlgorithm(SignatureAlgorithm.RS256);
		authContextInfo.setRequiredClaims(Set.of("exp"));
		authContextInfo.setRequireNamedPrincipal(false);
		return authContextInfo;
	}

	private static Set<String> parseRequiredCsv(String propertyName, String value) {
		String source = value == null ? "" : value;
		Set<String> values = Arrays.stream(source.split(",")).map(ApiGatewayJwtVerifier::normalize)
				.filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));

		if (values.isEmpty()) {
			throw new IllegalStateException(propertyName + " must contain at least one value");
		}

		return Collections.unmodifiableSet(values);
	}

	private static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}

	private static String toUrlString(String jwksUri) {
		try {
			URL url = URI.create(jwksUri).toURL();
			return url.toExternalForm();
		} catch (IllegalArgumentException | MalformedURLException e) {
			throw new IllegalStateException("Invalid API gateway JWT JWKS URI: " + jwksUri, e);
		}
	}
}
