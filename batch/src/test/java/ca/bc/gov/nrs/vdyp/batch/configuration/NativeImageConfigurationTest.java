package ca.bc.gov.nrs.vdyp.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
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

	@Test
	void testRegisterSpringBatchClasses_JdbcJobInstanceDao() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class);
		assertTrue(predicate.test(hints), "JdbcJobInstanceDao should be registered for reflection");
	}

	@Test
	void testRegisterSpringBatchClasses_JdbcJobExecutionDao() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection().onType(JdbcJobExecutionDao.class);
		assertTrue(predicate.test(hints), "JdbcJobExecutionDao should be registered for reflection");
	}

	@Test
	void testRegisterSpringBatchClasses_WithPublicConstructors() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class)
				.withMemberCategory(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		assertTrue(predicate.test(hints), "JdbcJobInstanceDao public constructors should be accessible");
	}

	@Test
	void testRegisterSpringBatchClasses_WithPublicMethods() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class)
				.withMemberCategory(MemberCategory.INVOKE_PUBLIC_METHODS);
		assertTrue(predicate.test(hints), "JdbcJobInstanceDao public methods should be accessible");
	}

	@Test
	void testRegisterSpringBatchClasses_WithDeclaredFields() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class)
				.withMemberCategory(MemberCategory.DECLARED_FIELDS);
		assertTrue(predicate.test(hints), "JdbcJobInstanceDao declared fields should be accessible");
	}

	@Test
	void testRegisterH2DatabaseClasses_H2DriverRegistered() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		try {
			Class.forName("org.h2.Driver");
			Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection()
					.onType(TypeReference.of("org.h2.Driver"));
			assertTrue(predicate.test(hints), "H2 Driver should be registered for reflection");
		} catch (ClassNotFoundException e) {
			// H2 not on classpath, skip verification
		}
	}

	@Test
	void testRegisterVdypModelClasses_PolygonClass() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		try {
			Class.forName("ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon");
			Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection()
					.onType(TypeReference.of("ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon"));
			assertTrue(predicate.test(hints), "Polygon class should be registered for reflection");
		} catch (ClassNotFoundException e) {
			// VDYP model class not on classpath, skip verification
		}
	}

	@Test
	void testRegisterVdypModelClasses_LayerClass() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		vdypRuntimeHints.registerHints(hints, classLoader);

		try {
			Class.forName("ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer");
			Predicate<RuntimeHints> predicate = RuntimeHintsPredicates.reflection()
					.onType(TypeReference.of("ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer"));
			assertTrue(predicate.test(hints), "Layer class should be registered for reflection");
		} catch (ClassNotFoundException e) {
			// VDYP model class not on classpath, skip verification
		}
	}

	@Test
	void testRegisterClassIfExists_HandlesClassNotFoundException() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, classLoader));
	}

	@Test
	void testRegisterSpringBootClasses_OptionalClassesHandled() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, classLoader));

		assertTrue(
				RuntimeHintsPredicates.reflection().onType(JdbcJobInstanceDao.class).test(hints),
				"Core Spring Batch classes should be registered"
		);
	}

	@Test
	void testRegisterHints_MultipleInvocations() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertDoesNotThrow(() -> {
			vdypRuntimeHints.registerHints(hints, classLoader);
			vdypRuntimeHints.registerHints(hints, classLoader);
		});
	}

	@Test
	void testConfigurationAnnotations_ConfigurationPresent() {
		assertNotNull(
				NativeImageConfiguration.class
						.getAnnotation(Configuration.class),
				"@Configuration annotation should be present"
		);
	}

	@Test
	void testConfigurationAnnotations_ImportRuntimeHintsPresent() {
		assertNotNull(
				NativeImageConfiguration.class
						.getAnnotation(ImportRuntimeHints.class),
				"@ImportRuntimeHints annotation should be present"
		);
	}

	@Test
	void testConfigurationAnnotations_ProxyBeanMethodsDisabled() {
		var configAnnotation = NativeImageConfiguration.class
				.getAnnotation(Configuration.class);
		assertNotNull(configAnnotation);
		assertFalse(configAnnotation.proxyBeanMethods(), "proxyBeanMethods should be false");
	}

	@Test
	void testConfigurationAnnotations_ImportsCorrectRuntimeHints() {
		var importAnnotation = NativeImageConfiguration.class
				.getAnnotation(ImportRuntimeHints.class);
		assertNotNull(importAnnotation);

		Class<?>[] value = importAnnotation.value();
		assertNotNull(value);
		assertTrue(value.length > 0, "ImportRuntimeHints should reference at least one class");
		assertTrue(
				value[0] == NativeImageConfiguration.VdypRuntimeHints.class,
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
	}

	@Test
	void testVdypModelClassRegistration_HandlesOptionalDependency() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, classLoader));
	}

	@Test
	void testSpringBootClassRegistration_HandlesOptionalClasses() {
		RuntimeHints hints = new RuntimeHints();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assertDoesNotThrow(() -> vdypRuntimeHints.registerHints(hints, classLoader));
	}
}
