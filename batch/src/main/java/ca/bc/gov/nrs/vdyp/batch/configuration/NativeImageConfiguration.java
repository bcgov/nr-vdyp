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

			// Register VDYP Extended Core classes
			registerVdypEcoreModelClasses(hints);
			registerVdypEcoreProjectionClasses(hints);
			registerVdypEcoreInputClasses(hints);
			registerVdypEcoreOutputClasses(hints);

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

		/**
		 * Register VDYP Extended Core model classes (ca.bc.gov.nrs.vdyp.ecore.model.v1.*)
		 */
		private void registerVdypEcoreModelClasses(RuntimeHints hints) {
			try {
				// API model classes
				String[] modelClasses = { "ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.FilterParameters",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.UtilizationParameter",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.UtilizationClassSet",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.ParameterDetailsMessage",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.MessagesInner",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessage" };

				for (String className : modelClasses) {
					hints.reflection().registerType(
							Class.forName(className),
							hint -> hint.withMembers(
									MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
									MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS,
									MemberCategory.PUBLIC_FIELDS
							)
					);
				}

				// Parameters nested enums
				String[] parameterEnums = { "ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters$ExecutionOption",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters$OutputFormat",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters$DebugOption",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters$AgeYearRangeCombinationKind",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters$MetadataToOutputDirective" };

				for (String className : parameterEnums) {
					hints.reflection().registerType(
							Class.forName(className),
							hint -> hint.withMembers(
									MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
									MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.PUBLIC_FIELDS
							)
					);
				}

				// Other enums
				String[] otherEnums = { "ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.PolygonMessageKind",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.StandYieldMessageKind",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.ValidationMessageKind",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.ProgressFrequency",
						"ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind" };

				for (String className : otherEnums) {
					hints.reflection().registerType(
							Class.forName(className),
							hint -> hint.withMembers(
									MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
									MemberCategory.PUBLIC_FIELDS
							)
					);
				}

			} catch (ClassNotFoundException e) {
				// Classes not found, skip registration
			}
		}

		/**
		 * Register VDYP Extended Core projection classes (ca.bc.gov.nrs.vdyp.ecore.projection.*)
		 */
		private void registerVdypEcoreProjectionClasses(RuntimeHints hints) {
			try {
				// Core projection model classes
				String[] projectionModelClasses = { "ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.Species",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.Stand",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.SiteSpecies",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.History",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerAdjustments",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.ProjectionParameters",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonReportingInfo",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.SpeciesReportingInfo",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonMessage",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonMessageList" };

				for (String className : projectionModelClasses) {
					hints.reflection().registerType(
							Class.forName(className),
							hint -> hint.withMembers(
									MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
									MemberCategory.DECLARED_FIELDS, MemberCategory.PUBLIC_FIELDS
							)
					);
				}

				// Projection model enumerations
				String[] projectionEnums = { "ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.CfsEcoZoneCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.GrowthModelCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.InventoryStandard",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.LayerSummarizationModeCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.NonVegetationTypeCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.OtherVegetationTypeCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.PolygonProcessingStateCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProcessingModeCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ReturnCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.SilviculturalBaseCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.SpeciesProjectionTypeCode",
						"ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.Vdyp7LayerTypeCode" };

				for (String className : projectionEnums) {
					hints.reflection().registerType(
							Class.forName(className),
							hint -> hint.withMembers(
									MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
									MemberCategory.PUBLIC_FIELDS
							)
					);
				}

			} catch (ClassNotFoundException e) {
				// Classes not found, skip registration
			}
		}

		/**
		 * Register VDYP Extended Core input classes (ca.bc.gov.nrs.vdyp.ecore.projection.input.*)
		 */
		private void registerVdypEcoreInputClasses(RuntimeHints hints) {
			try {
				// OpenCSV bean classes - need unsafeAllocated for instantiation
				String[] csvBeanClasses = { "ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvPolygonRecordBean",
						"ca.bc.gov.nrs.vdyp.ecore.projection.input.HcsvLayerRecordBean" };

				for (String className : csvBeanClasses) {
					Class<?> clazz = Class.forName(className);
					hints.reflection().registerType(
							clazz,
							hint -> hint.withMembers(
									MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
									MemberCategory.DECLARED_FIELDS, MemberCategory.PUBLIC_FIELDS
							)
					);
					// Also register inner classes for record beans
					for (Class<?> innerClass : clazz.getDeclaredClasses()) {
						hints.reflection().registerType(
								innerClass,
								hint -> hint.withMembers(
										MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
										MemberCategory.DECLARED_FIELDS, MemberCategory.PUBLIC_FIELDS
								)
						);
					}
				}

			} catch (ClassNotFoundException e) {
				// Classes not found, skip registration
			}
		}

		/**
		 * Register VDYP Extended Core output classes (ca.bc.gov.nrs.vdyp.ecore.projection.output.*)
		 */
		private void registerVdypEcoreOutputClasses(RuntimeHints hints) {
			try {
				// Yield table output bean classes
				String[] yieldTableBeanClasses = {
						"ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.DCSVYieldTableRecordBean",
						"ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.PLOTSYYieldTableRecordBean",
						"ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.TextYieldTableRowValuesBean",
						"ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.CSVYieldTableRowValuesBean",
						"ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.EntityVolumeDetails",
						"ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.EntityGrowthDetails",
						"ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.CfsBiomassVolumeDetails",
						"ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.LayerYields",
						"ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTableRowContext",
						"ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTableSpeciesDetails" };

				for (String className : yieldTableBeanClasses) {
					hints.reflection().registerType(
							Class.forName(className),
							hint -> hint.withMembers(
									MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS,
									MemberCategory.DECLARED_FIELDS, MemberCategory.PUBLIC_FIELDS
							)
					);
				}

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
