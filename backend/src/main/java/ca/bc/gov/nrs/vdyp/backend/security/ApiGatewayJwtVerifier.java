package ca.bc.gov.nrs.vdyp.backend.security;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ApiGatewayJwtVerifier {

	private static final String AUDIENCE_PROPERTY = "vdyp.api-gateway.jwt.audience";

	private static final String VERIFICATION_ENABLED_PROPERTY = "vdyp.api-gateway.jwt.verification.enabled";

	private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

	private final boolean verificationEnabled;

	@Inject
	public ApiGatewayJwtVerifier(
			@ConfigProperty(name = "vdyp.api-gateway.jwt.jwks-uri") String jwksUri,
			@ConfigProperty(name = AUDIENCE_PROPERTY) String audience,
			@ConfigProperty(name = "vdyp.api-gateway.jwt.issuer") String issuer,
			@ConfigProperty(name = VERIFICATION_ENABLED_PROPERTY, defaultValue = "true") boolean verificationEnabled
	) {
		this(audience, issuer, JWKSourceBuilder.create(toUrl(jwksUri)).build(), verificationEnabled);
	}

	ApiGatewayJwtVerifier(String audience, String issuer, JWKSource<SecurityContext> jwkSource) {
		this(audience, issuer, jwkSource, true);
	}

	ApiGatewayJwtVerifier(
			String audience, String issuer, JWKSource<SecurityContext> jwkSource, boolean verificationEnabled
	) {
		this.verificationEnabled = verificationEnabled;
		this.jwtProcessor = createJwtProcessor(parseRequiredCsv(AUDIENCE_PROPERTY, audience), issuer, jwkSource);
	}

	public JWTClaimsSet verify(String token) throws ParseException, BadJOSEException, JOSEException {
		if (!verificationEnabled) {
			return new JWTClaimsSet.Builder().build();
		}

		return jwtProcessor.process(token, null);
	}

	private static ConfigurableJWTProcessor<SecurityContext>
			createJwtProcessor(Set<String> audiences, String issuer, JWKSource<SecurityContext> jwkSource) {
		ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
		JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
		processor.setJWSKeySelector(keySelector);

		DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier = new DefaultJWTClaimsVerifier<>(
				audiences, exactIssuerClaim(issuer), Set.of("exp"), Set.of()
		);
		processor.setJWTClaimsSetVerifier(claimsVerifier);

		return processor;
	}

	private static JWTClaimsSet exactIssuerClaim(String issuer) {
		String normalizedIssuer = normalize(issuer);
		if (normalizedIssuer == null) {
			return null;
		}

		return new JWTClaimsSet.Builder().issuer(normalizedIssuer).build();
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

	private static URL toUrl(String jwksUri) {
		try {
			return URI.create(jwksUri).toURL();
		} catch (IllegalArgumentException | MalformedURLException e) {
			throw new IllegalStateException("Invalid API gateway JWT JWKS URI: " + jwksUri, e);
		}
	}
}
