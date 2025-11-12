package ca.bc.gov.nrs.vdyp.batch.configuration;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcJobInstanceDao;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Native Image configuration for GraalVM. This class registers runtime hints for classes that are loaded dynamically
 * and need to be included in the native image.
 */
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeImageConfiguration.VdypRuntimeHints.class)
@SuppressWarnings("java:S1118") // Utility class pattern not needed - Spring manages this as a bean
public class NativeImageConfiguration {
	// No @Bean methods, so proxyBeanMethods=false to avoid CGLIB enhancement

	static class VdypRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			// Register Spring Boot initialization classes
			registerSpringBootClasses(hints, classLoader);

			// Register Spring Batch infrastructure classes
			registerSpringBatchClasses(hints);

			// Register H2 Database classes
			registerH2DatabaseClasses(hints);

			// Register VDYP model classes
			registerVdypModelClasses(hints);
		}

		private void registerClassIfExists(RuntimeHints hints, ClassLoader classLoader, String className) {
			try {
				Class<?> clazz = Class.forName(className, false, classLoader);
				hints.reflection().registerType(
						clazz,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS
						)
				);
			} catch (ClassNotFoundException e) {
				// Class not found, skip registration
			}
		}

		private void registerSpringBootClasses(RuntimeHints hints, ClassLoader classLoader) {
			// Register Spring Boot initializer classes that are loaded via SpringFactoriesLoader
			registerClassIfExists(
					hints, classLoader,
					"org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer"
			);

			registerClassIfExists(
					hints, classLoader, "org.springframework.boot.autoconfigure.BackgroundPreinitializer"
			);

			registerClassIfExists(
					hints, classLoader, "org.springframework.boot.context.ContextIdApplicationContextInitializer"
			);

			registerClassIfExists(
					hints, classLoader,
					"org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener"
			);
		}

		private void registerSpringBatchClasses(RuntimeHints hints) {
			// Spring Batch DAO classes
			hints.reflection().registerType(
					JdbcJobInstanceDao.class,
					hint -> hint.withMembers(
							MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
							MemberCategory.DECLARED_FIELDS
					)
			);

			hints.reflection().registerType(
					JdbcJobExecutionDao.class,
					hint -> hint.withMembers(
							MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
							MemberCategory.DECLARED_FIELDS
					)
			);
		}

		@SuppressWarnings("java:S4925") // Class.forName is necessary for runtime dependency
		private void registerH2DatabaseClasses(RuntimeHints hints) {
			// H2 Database driver - register by class name since it's a runtime dependency
			try {
				Class<?> h2DriverClass = Class.forName("org.h2.Driver");
				hints.reflection().registerType(
						h2DriverClass, hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
				);
			} catch (ClassNotFoundException e) {
				// H2 not found, skip registration
			}
		}

		private void registerVdypModelClasses(RuntimeHints hints) {
			// VDYP model classes for reflection (serialization, etc.)
			try {
				hints.reflection().registerType(
						Class.forName("ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon"),
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
								MemberCategory.DECLARED_FIELDS
						)
				);

				hints.reflection().registerType(
						Class.forName("ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer"),
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
								MemberCategory.DECLARED_FIELDS
						)
				);
			} catch (ClassNotFoundException e) {
				// Classes not found, skip registration
			}
		}
	}
}
