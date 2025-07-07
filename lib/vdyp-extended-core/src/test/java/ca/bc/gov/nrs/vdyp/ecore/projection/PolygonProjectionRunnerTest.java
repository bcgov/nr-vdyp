package ca.bc.gov.nrs.vdyp.ecore.projection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonExecutionException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.MessageSeverityCode;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.PolygonMessageKind;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProgressFrequency;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.History;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.LayerReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Polygon;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.PolygonReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Species;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.SpeciesReportingInfo;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.Stand;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.InventoryStandard;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ReturnCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable.YieldTable;
import ca.bc.gov.nrs.vdyp.exceptions.BecMissingException;
import ca.bc.gov.nrs.vdyp.exceptions.BreastHeightAgeLowException;
import ca.bc.gov.nrs.vdyp.exceptions.CrownClosureLowException;
import ca.bc.gov.nrs.vdyp.exceptions.HeightLowException;
import ca.bc.gov.nrs.vdyp.exceptions.IncorrectLayerCodesException;
import ca.bc.gov.nrs.vdyp.exceptions.LayerMissingException;
import ca.bc.gov.nrs.vdyp.exceptions.LayerSpeciesDoNotSumTo100PercentException;
import ca.bc.gov.nrs.vdyp.exceptions.ResultBaseAreaLowException;
import ca.bc.gov.nrs.vdyp.exceptions.SiteIndexLowException;
import ca.bc.gov.nrs.vdyp.exceptions.StandProcessingException;
import ca.bc.gov.nrs.vdyp.exceptions.TotalAgeLowException;
import ca.bc.gov.nrs.vdyp.exceptions.UnsupportedModeException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.si32.vdyp.VdypMethods;

public class PolygonProjectionRunnerTest {
	History history;
	PolygonReportingInfo polygonReportingInfo;
	Polygon polygon;
	Layer layer;
	Parameters params;

	@BeforeEach
	void setup() {
		history = new History.Builder().build();
		polygonReportingInfo = new PolygonReportingInfo.Builder().featureId(13919428).polygonNumber(13919428L)
				.mapSheet("1").mapQuad("0").mapSubQuad("0").nonProdDescriptor("").district(null).referenceYear(2024)
				.build();
		polygon = new Polygon.Builder().featureId(13919428).polygonNumber(13919428L).referenceYear(2024)
				.inventoryStandard(InventoryStandard.FIP).history(history).becZone("MS")
				.reportingInfo(polygonReportingInfo).build();
		layer = new Layer.Builder().layerId("1").polygon(polygon).doSuppressPerHAYields(false).crownClosure((short) 20)
				.vdyp7LayerCode(ProjectionTypeCode.PRIMARY).build();
		polygon.getLayers().put(layer.getLayerId(), layer);
		params = new Parameters().ageStart(0).ageEnd(190).progressFrequency(ProgressFrequency.FrequencyKind.POLYGON)
				.addSelectedExecutionOptionsItem(Parameters.ExecutionOption.DO_ENABLE_PROGRESS_LOGGING);

	}

	void addStand(String speciesCode, double percent, double age, double height) {
		var stand = new Stand.Builder().layer(layer).sp0Code(VdypMethods.getVDYP7Species(speciesCode)).build();
		var sp0 = new Species.Builder().stand(stand).speciesCode(VdypMethods.getVDYP7Species(speciesCode))
				.speciesPercent(0).build();
		stand.addSpeciesGroup(sp0, layer.getSp0sAsSupplied().size());

		layer.addStand(stand);
		var sp64 = new Species.Builder().stand(stand).speciesCode(speciesCode).speciesPercent(percent).totalAge(age)
				.dominantHeight(height).build();
		stand.addSp64(sp64);
		layer.addSp64(sp64);
		var layerReportingInfo = new LayerReportingInfo.Builder() //
				.layer(layer) //
				.sourceLayerID(0) //
				.build();

		polygonReportingInfo.getLayerReportingInfos().put(layerReportingInfo.getLayerID(), layerReportingInfo);
		var speciesReportingInfoList = new ArrayList<SpeciesReportingInfo>();

		var speciesReportingInfo = new SpeciesReportingInfo.Builder() //
				.sp64Name(speciesCode) //
				.sp64Percent(percent) //
				.asSuppliedIndex(1) //
				.build();

		speciesReportingInfoList.add(speciesReportingInfo);

		layerReportingInfo.setSpeciesReportingInfos(speciesReportingInfoList);

	}

	@Test
	void testUnsupportedSpeciesFIPStart() throws AbstractProjectionRequestException, IOException {
		addStand("PL", 78.0, 8.0, 10.0);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		layer.setAssignedProjectionType(ProjectionTypeCode.PRIMARY);
		polygon.doCompleteDefinition(context);
		var componentRunner = new RealComponentRunner();
		var unit = PolygonProjectionRunner.of(polygon, context, componentRunner);

		assertThrows(PolygonExecutionException.class, unit::project);

	}

	@Test
	void testTooYoungFIPStartFallthroughVRI() throws AbstractProjectionRequestException, IOException {
		addStand("PL", 100.0, 8.0, 1.0);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		layer.setAssignedProjectionType(ProjectionTypeCode.PRIMARY);
		polygon.doCompleteDefinition(context);
		var componentRunner = new RealComponentRunner();
		var unit = PolygonProjectionRunner.of(polygon, context, componentRunner);
		unit.project();
		String progress = new String(context.getProgressLog().getAsStream().readAllBytes());
		YieldTable yieldTable = context.getYieldTable();
		yieldTable.startGeneration();
		yieldTable.endGeneration();

		var content = new String(yieldTable.getAsStream().readAllBytes());
		assertThat(content.length(), is(0));
		assertThat(progress.length(), is(0));
	}

	@Test
	void testValidFIPStart() throws AbstractProjectionRequestException, IOException {
		addStand("PL", 100.0, 8.0, 10.0);
		var context = new ProjectionContext(ProjectionRequestKind.HCSV, "TEST", params, false);
		layer.setAssignedProjectionType(ProjectionTypeCode.PRIMARY);
		polygon.doCompleteDefinition(context);
		var componentRunner = new RealComponentRunner();
		var unit = PolygonProjectionRunner.of(polygon, context, componentRunner);
		unit.project();
		YieldTable yieldTable = context.getYieldTable();
		yieldTable.startGeneration();
		yieldTable.endGeneration();

		var content = new String(yieldTable.getAsStream().readAllBytes());
		assertThat(content.length(), greaterThan(0));
		assertThat(content, containsString("13919428"));
	}

	/**
	 * Examples of stand exeptions that can be produced by VRIStart
	 */
	static List<Arguments> fipErrors() {
		return List.of(
				Arguments.of(
						new UnsupportedModeException(Optional.empty()), false, ReturnCode.SUCCESS,
						PolygonMessageKind.LOW_SITE, MessageSeverityCode.WARNING
				),
				Arguments.of(
						new UnsupportedModeException(Optional.of(PolygonMode.BATC)), false, ReturnCode.SUCCESS,
						PolygonMessageKind.LOW_SITE, MessageSeverityCode.WARNING
				),

				Arguments.of(
						new TotalAgeLowException(LayerType.PRIMARY, Optional.of(10f), Optional.of(12f)), false,
						ReturnCode.SUCCESS, PolygonMessageKind.BREAST_HEIGHT_AGE_TOO_YOUNG, MessageSeverityCode.WARNING
				),

				Arguments.of(
						new BecMissingException(), true, ReturnCode.SUCCESS,
						PolygonMessageKind.BREAST_HEIGHT_AGE_TOO_YOUNG, MessageSeverityCode.WARNING
				),
				Arguments.of(
						new ResultBaseAreaLowException(LayerType.PRIMARY, Optional.of(10f), Optional.of(12f)), true,
						ReturnCode.SUCCESS, PolygonMessageKind.BREAST_HEIGHT_AGE_TOO_YOUNG, MessageSeverityCode.WARNING
				),

				Arguments.of(
						new BreastHeightAgeLowException(LayerType.PRIMARY, Optional.of(10f), Optional.of(12f)), false,
						ReturnCode.ERROR_CORELIBRARYERROR, PolygonMessageKind.GENERIC_FIPSTART_ERROR,
						MessageSeverityCode.ERROR
				),
				Arguments.of(
						new CrownClosureLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)), false,
						ReturnCode.ERROR_CORELIBRARYERROR, PolygonMessageKind.GENERIC_FIPSTART_ERROR,
						MessageSeverityCode.ERROR
				),
				Arguments.of(
						new HeightLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)), false,
						ReturnCode.ERROR_CORELIBRARYERROR, PolygonMessageKind.GENERIC_FIPSTART_ERROR,
						MessageSeverityCode.ERROR
				),
				Arguments.of(
						new IncorrectLayerCodesException("X"), false, ReturnCode.ERROR_CORELIBRARYERROR,
						PolygonMessageKind.GENERIC_FIPSTART_ERROR, MessageSeverityCode.ERROR
				),
				Arguments.of(
						new LayerMissingException(LayerType.PRIMARY), false, ReturnCode.ERROR_CORELIBRARYERROR,
						PolygonMessageKind.GENERIC_FIPSTART_ERROR, MessageSeverityCode.ERROR
				),
				Arguments.of(
						new LayerSpeciesDoNotSumTo100PercentException(LayerType.PRIMARY), false,
						ReturnCode.ERROR_CORELIBRARYERROR, PolygonMessageKind.GENERIC_FIPSTART_ERROR,
						MessageSeverityCode.ERROR
				),
				Arguments.of(
						new SiteIndexLowException(LayerType.PRIMARY, Optional.of(0.5f), Optional.of(0.75f)), false,
						ReturnCode.ERROR_CORELIBRARYERROR, PolygonMessageKind.GENERIC_FIPSTART_ERROR,
						MessageSeverityCode.ERROR
				)
		);
	}

	@ParameterizedTest
	@MethodSource("fipErrors")
	void testHandleFipError(
			StandProcessingException ex, boolean expectedRetryVri, ReturnCode returnCode, PolygonMessageKind kind,
			MessageSeverityCode severity
	) throws AbstractProjectionRequestException, IOException {
		boolean actualRetryVri = PolygonProjectionRunner.handleFipError(polygon, ex, layer);
		var actualMessages = polygon.getMessages();

		assertThat("retry using VRI", actualRetryVri, is(expectedRetryVri));

		assertThat(
				"messages", actualMessages,
				contains(
						allOf(
								hasProperty("layer", sameInstance(layer)), hasProperty("returnCode", is(returnCode)),
								hasProperty("kind", is(kind)), hasProperty("severity", is(severity)),
								hasProperty("stand", nullValue())
						)
				)
		);
	}
}
