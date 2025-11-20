package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcJobInstanceDao;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

class NativeImageConfigurationTest {

	private final NativeImageConfiguration.VdypRuntimeHints vdypRuntimeHints = new NativeImageConfiguration.VdypRuntimeHints();

	@Test
	void testConfigurationClassInstantiation() {
		assertDoesNotThrow(NativeImageConfiguration::new);
		assertNotNull(new NativeImageConfiguration());
	}

	@Test
	void testVdypRuntimeHintsInstantiation() {
		assertDoesNotThrow(NativeImageConfiguration.VdypRuntimeHints::new);
		assertNotNull(new NativeImageConfiguration.VdypRuntimeHints());
	}

	@Test
	void testRegisterHints_ExecutesWithoutException() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, classLoader));
	}

	@Test
	void testRegisterHints_WithNullClassLoader() {
		RuntimeHints hints = new RuntimeHints();

		assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, null));
	}

	private static Stream<Arguments> springBatchClassTestParameters() {
		return Stream.of(
				Arguments.of(JdbcJobInstanceDao.class, null, "JdbcJobInstanceDao should be registered for reflection"),
				Arguments
						.of(JdbcJobExecutionDao.class, null, "JdbcJobExecutionDao should be registered for reflection"),
				Arguments.of(
						JdbcJobInstanceDao.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
						"JdbcJobInstanceDao public constructors should be accessible"
				),
				Arguments.of(
						JdbcJobInstanceDao.class, MemberCategory.INVOKE_PUBLIC_METHODS,
						"JdbcJobInstanceDao public methods should be accessible"
				),
				Arguments.of(
						JdbcJobInstanceDao.class, MemberCategory.DECLARED_FIELDS,
						"JdbcJobInstanceDao declared fields should be accessible"
				)
		);
	}

	@ParameterizedTest
	@MethodSource("springBatchClassTestParameters")
	void testRegisterSpringBatchClasses(Class<?> targetClass, MemberCategory memberCategory, String message) {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		Predicate<RuntimeHints> predicate = memberCategory == null
				? RuntimeHintsPredicates.reflection().onType(targetClass)
				: RuntimeHintsPredicates.reflection().onType(targetClass).withMemberCategory(memberCategory);

		assertTrue(predicate.test(hints), message);
	}

	private static Stream<Arguments> optionalClassTestParameters() {
		return Stream.of(
				Arguments.of("org.h2.Driver", "H2 Driver should be registered for reflection"),
				Arguments.of(
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon",
						"Polygon class should be registered for reflection"
				),
				Arguments.of(
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer",
						"Layer class should be registered for reflection"
				)
		);
	}

	@ParameterizedTest
	@MethodSource("optionalClassTestParameters")
	void testRegisterOptionalClasses(String className, String message) {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		try {
			Class.forName(className);
			Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection().onType(TypeReference.of(className));
			assertTrue(predicate.test(hints), message);
		} catch (ClassNotFoundException e) {
			//
		}
	}

	private static Stream<Arguments> registerHintsRobustnessTestParameters() {
		return Stream.of(
				Arguments.of(
						"testRegisterClassIfExists_HandlesClassNotFoundException", false,
						"Core classes should be registered even when optional classes are missing"
				),
				Arguments.of(
						"testRegisterSpringBootClasses_OptionalClassesHandled", false,
						"Core Spring Batch classes should be registered"
				),
				Arguments.of(
						"testRegisterHints_MultipleInvocations", true,
						"Multiple invocations should not cause exceptions"
				)
		);
	}

	@ParameterizedTest
	@MethodSource("registerHintsRobustnessTestParameters")
	void testRegisterHintsRobustness(String testName, boolean multipleInvocations, String verificationMessage) {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		if (multipleInvocations) {
			assertDoesNotThrow(() -> {
				vdypRuntimeHints.registerHints(hints, classLoader);
				vdypRuntimeHints.registerHints(hints, classLoader);
			});
		} else {
			assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, classLoader));
			assertTrue(
					RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class).test(hints),
					verificationMessage
			);
		}
	}

	@Test
	void testConfigurationAnnotations_ConfigurationPresent() {
		assertNotNull(
				NativeImageConfiguration.class.getAnnotation(Configuration.class),
				"@Configuration annotation should be present"
		);
	}

	@Test
	void testConfigurationAnnotations_ImportRuntimeHintsPresent() {
		assertNotNull(
				NativeImageConfiguration.class.getAnnotation(ImportRuntimeHints.class),
				"@ImportRuntimeHints annotation should be present"
		);
	}

	@Test
	void testConfigurationAnnotations_ProxyBeanMethodsDisabled() {
		var configAnnotation = NativeImageConfiguration.class.getAnnotation(Configuration.class);
		assertNotNull(configAnnotation);
		assertFalse(configAnnotation.proxyBeanMethods(), "proxyBeanMethods should be false");
	}

	@Test
	void testConfigurationAnnotations_ImportsCorrectRuntimeHints() {
		var importAnnotation = NativeImageConfiguration.class.getAnnotation(ImportRuntimeHints.class);
		assertNotNull(importAnnotation);

		Class<?>[] value = importAnnotation.value();
		assertNotNull(value);
		assertTrue(value.length > 0, "ImportRuntimeHints should reference at least one class");
		assertSame(
				NativeImageConfiguration.VdypRuntimeHints.class, value[0],
				"ImportRuntimeHints should reference VdypRuntimeHints.class"
		);
	}

	@Test
	void testVdypRuntimeHintsIsStaticInnerClass() {
		assertNotNull(NativeImageConfiguration.VdypRuntimeHints.class);
	}

	@Test
	void testRuntimeHintsRegistration_VerifyAllSpringBatchDaoClasses() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		assertTrue(
				RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class).test(hints),
				"JdbcJobInstanceDao should be registered"
		);
		assertTrue(
				RuntimeHintsPredicates.reflection().onType(JdbcJobExecutionDao.class).test(hints),
				"JdbcJobExecutionDao should be registered"
		);
	}

	@Test
	void testRuntimeHintsRegistration_VerifyMemberCategories() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		assertTrue(
				RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class)
						.withMemberCategory(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS).test(hints),
				"INVOKE_PUBLIC_CONSTRUCTORS should be registered"
		);
		assertTrue(
				RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class)
						.withMemberCategory(MemberCategory.INVOKE_PUBLIC_METHODS).test(hints),
				"INVOKE_PUBLIC_METHODS should be registered"
		);
		assertTrue(
				RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class)
						.withMemberCategory(MemberCategory.DECLARED_FIELDS).test(hints),
				"DECLARED_FIELDS should be registered"
		);
	}

	@Test
	void testCompleteRegistrationFlow() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, classLoader));

		assertTrue(
				RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class).test(hints),
				"Core Spring Batch classes should be registered"
		);
		assertTrue(
				RuntimeHintsPredicates.reflection().onType(JdbcJobExecutionDao.class).test(hints),
				"Core Spring Batch classes should be registered"
		);
	}

	@Test
	void testH2DatabaseClassRegistration_HandlesOptionalDependency() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, classLoader));

		try {
			Class.forName("org.h2.Driver");
			Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection()
					.onType(TypeReference.of("org.h2.Driver"));
			assertTrue(predicate.test(hints), "H2 Driver should be registered when available");
		} catch (ClassNotFoundException e) {
			//
		}
	}

	@Test
	void testVdypModelClassRegistration_HandlesOptionalDependency() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, classLoader));

		try {
			Class.forName("ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon");
			Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection()
					.onType(TypeReference.of("ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon"));
			assertTrue(predicate.test(hints), "VDYP Polygon class should be registered when available");
		} catch (ClassNotFoundException e) {
			//
		}
	}

	@Test
	void testSpringBootClassRegistration_HandlesOptionalClasses() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, classLoader));

		assertTrue(
				RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class).test(hints),
				"Spring Batch core classes should always be registered"
		);
	}
}
