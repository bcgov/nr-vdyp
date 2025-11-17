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

			// Register Jackson classes for JSON serialization/deserialization
			registerJacksonClasses(hints);
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

			try {
				Class<?> scopedProxyFactoryBean = Class.forName("org.springframework.aop.scope.ScopedProxyFactoryBean");
				hints.reflection().registerType(
						scopedProxyFactoryBean,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
								MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS,
								MemberCategory.DECLARED_FIELDS, MemberCategory.PUBLIC_FIELDS
						)
				);

				Class<?> jdkDynamicAopProxy = Class.forName("org.springframework.aop.framework.JdkDynamicAopProxy");
				hints.reflection().registerType(
						jdkDynamicAopProxy,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
								MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS
						)
				);

				Class<?> cglibAopProxy = Class.forName("org.springframework.aop.framework.CglibAopProxy");
				hints.reflection().registerType(
						cglibAopProxy,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
								MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_METHODS
						)
				);
			} catch (ClassNotFoundException e) {
				// Proxy classes not found, skip registration
			}

			try {
				Class<?> stepScope = Class.forName("org.springframework.batch.core.scope.StepScope");
				hints.reflection().registerType(
						stepScope,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
								MemberCategory.DECLARED_FIELDS
						)
				);

				Class<?> jobScope = Class.forName("org.springframework.batch.core.scope.JobScope");
				hints.reflection().registerType(
						jobScope,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
								MemberCategory.DECLARED_FIELDS
						)
				);
			} catch (ClassNotFoundException e) {
				// StepScope/JobScope not found, skip registration
			}

			try {
				Class<?> aopInfrastructureBean = Class
						.forName("org.springframework.aop.framework.AopInfrastructureBean");
				hints.reflection().registerType(
						aopInfrastructureBean, hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS)
				);

				Class<?> scopedObject = Class.forName("org.springframework.aop.scope.ScopedObject");
				hints.reflection()
						.registerType(scopedObject, hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
			} catch (ClassNotFoundException e) {
				// AOP infrastructure classes not found, skip registration
			}
		}

		@SuppressWarnings("java:S4925") // Class.forName is necessary for runtime dependency
		private void registerH2DatabaseClasses(RuntimeHints hints) {
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

				Class<?> parametersClass = Class.forName("ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters");
				hints.reflection().registerType(
						parametersClass,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
								MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS,
								MemberCategory.PUBLIC_FIELDS
						)
				);

				Class<?> executionOptionClass = Class
						.forName("ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters$ExecutionOption");
				hints.reflection().registerType(
						executionOptionClass,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
								MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.PUBLIC_FIELDS
						)
				);

				Class<?> outputFormatClass = Class.forName("ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters$OutputFormat");
				hints.reflection().registerType(
						outputFormatClass,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
								MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.PUBLIC_FIELDS
						)
				);

				Class<?> debugOptionClass = Class.forName("ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters$DebugOption");
				hints.reflection().registerType(
						debugOptionClass,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
								MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.PUBLIC_FIELDS
						)
				);

				Class<?> ageYearRangeClass = Class
						.forName("ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters$AgeYearRangeCombinationKind");
				hints.reflection().registerType(
						ageYearRangeClass,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
								MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.PUBLIC_FIELDS
						)
				);

				Class<?> metadataDirectiveClass = Class
						.forName("ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters$MetadataToOutputDirective");
				hints.reflection().registerType(
						metadataDirectiveClass,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
								MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.PUBLIC_FIELDS
						)
				);

			} catch (ClassNotFoundException e) {
				// Classes not found, skip registration
			}
		}

		private void registerJacksonClasses(RuntimeHints hints) {
			try {
				Class<?> jacksonAnnotationIntrospector = Class
						.forName("com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector");
				hints.reflection().registerType(
						jacksonAnnotationIntrospector,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS
						)
				);

				Class<?> enumDeserializer = Class.forName("com.fasterxml.jackson.databind.deser.std.EnumDeserializer");
				hints.reflection().registerType(
						enumDeserializer,
						hint -> hint.withMembers(
								MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS
						)
				);

			} catch (ClassNotFoundException e) {
				// Jackson classes not found, skip registration
			}
		}
	}
}
