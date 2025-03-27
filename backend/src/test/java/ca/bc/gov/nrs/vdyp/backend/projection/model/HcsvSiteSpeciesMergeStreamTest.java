package ca.bc.gov.nrs.vdyp.backend.projection.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestException;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class HcsvSiteSpeciesMergeStreamTest {

	private AbstractPolygonStream polygonStream;

	@BeforeEach
	void beforeEach() throws IOException, PolygonValidationException, ProjectionRequestException {
		var parameters = new Parameters().ageStart(10).ageEnd(20);

		{
			var polygonStreamFile = FileHelper
					.getStubResourceFile(FileHelper.HCSV, FileHelper.VDYP_240, "VDYP7_INPUT_MULTI_POLY.csv");
			var layerStreamFile = FileHelper
					.getStubResourceFile(FileHelper.HCSV, FileHelper.VDYP_240, "VDYP7_INPUT_MULTI_POLY_LAYER.csv");

			var streams = new HashMap<String, InputStream>();
			streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
			streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, layerStreamFile);

			var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

			polygonStream = AbstractPolygonStream.build(state, streams);
		}
	}

	@Test
	void testMultiplePolygonRead() throws PolygonValidationException {
		Assert.assertTrue(polygonStream.hasNextPolygon());

		var polygon1 = polygonStream.getNextPolygon();
		Assert.assertNotNull(polygon1);
		Assert.assertEquals(13919428, polygon1.getFeatureId());
		Assert.assertEquals(3, polygon1.getLayers().size());

		var p1Layer1 = polygon1.getLayers().get("1");
		assertThat(p1Layer1, allOf(hasProperty("layerId", is("1")) //
				, hasProperty("polygon", hasProperty("featureId", is(13919428L))) //
				, hasProperty("vdyp7LayerCode", is(ProjectionTypeCode.PRIMARY)) //
				, hasProperty("doSuppressPerHAYields", is(false)) //
				, hasProperty("rankCode", is("1")) //
				, hasProperty("isDeadLayer", is(false)) //
		));
		assertThat(
				p1Layer1.getSp0sAsSupplied(), //
				contains(List.of(hasProperty("sp0Code", is("PL")) //
						, hasProperty("sp0Code", is("S"))
				))
		);
		assertThat(
				p1Layer1.getSp0sByName(), //
				contains(List.of(hasProperty("sp0Code", is("PL")) //
						, hasProperty("sp0Code", is("S"))
				))
		);
		assertThat(
				p1Layer1.getSp0sByPercent(), //
				contains(List.of(hasProperty("sp0Code", is("PL")) //
						, hasProperty("sp0Code", is("S"))
				))
		);
		assertThat(
				p1Layer1.getSp64sAsSupplied(), //
				contains(List.of(hasProperty("speciesCode", is("PLI")) //
						, hasProperty("speciesCode", is("SX"))
				))
		);
		assertThat(
				p1Layer1.getSp64sByName(), //
				contains(List.of(hasProperty("speciesCode", is("PLI")) //
						, hasProperty("speciesCode", is("SX"))
				))
		);
		assertThat(
				p1Layer1.getSp64sByPercent(), //
				contains(List.of(hasProperty("speciesCode", is("PLI")) //
						, hasProperty("speciesCode", is("SX"))
				))
		);

		assertThat(
				p1Layer1.getSp0sAsSupplied().get(0), //
				allOf(hasProperty("layer", hasProperty("layerId", is("1"))) //
						, hasProperty("standIndex", is(0)), //
						hasProperty(
								"speciesGroup",
								allOf(
										hasProperty("speciesCode", is("PL")),
										hasProperty("stand", hasProperty("sp0Code", is("PL")))
								)
						) //
						,
						hasProperty(
								"species",
								contains(
										List.of(
												allOf(
														hasProperty("stand", hasProperty("standIndex", is(0))),
														hasProperty("speciesCode", is("PLI"))
												)
										)
								)
						)
				)
		);

		assertThat(
				p1Layer1.getSp0sAsSupplied().get(1), //
				allOf(hasProperty("layer", hasProperty("layerId", is("1"))) //
						, hasProperty("standIndex", is(1)), //
						hasProperty(
								"speciesGroup",
								allOf(
										hasProperty("speciesCode", is("S")),
										hasProperty("stand", hasProperty("sp0Code", is("S")))
								)
						), //
						hasProperty(
								"species",
								contains(
										List.of(
												allOf(
														hasProperty("stand", hasProperty("standIndex", is(1))),
														hasProperty("speciesCode", is("SX"))
												)
										)
								)
						)
				)
		);

		assertThat(
				p1Layer1.getSp0sAsSupplied().get(0).getSpecies(),
				contains(List.of(hasProperty("speciesCode", is("PLI"))))
		);

		assertThat(
				p1Layer1.getSp0sAsSupplied().get(1).getSpecies(),
				contains(List.of(hasProperty("speciesCode", is("SX"))))
		);

		var p1Layer2 = polygon1.getLayers().get("2");
		assertThat(p1Layer2, allOf(hasProperty("layerId", is("2")) //
				, hasProperty("polygon", hasProperty("featureId", is(13919428L))) //
				, hasProperty("vdyp7LayerCode", is(ProjectionTypeCode.REGENERATION)) //
				, hasProperty("doSuppressPerHAYields", is(false)) //
				, hasProperty("rankCode", nullValue()) //
				, hasProperty("isDeadLayer", is(false)) //
		));
		assertThat(
				p1Layer2.getSp0sAsSupplied(), //
				contains(List.of(hasProperty("sp0Code", is("PL"))))
		);
		assertThat(
				p1Layer2.getSp64sAsSupplied(), //
				contains(List.of(hasProperty("speciesCode", is("PLI"))))
		);

		var p1Layer3 = polygon1.getLayers().get("D");
		assertThat(p1Layer3, allOf(hasProperty("layerId", is("D")) //
				, hasProperty("polygon", hasProperty("featureId", is(13919428L))) //
				, hasProperty("vdyp7LayerCode", is(ProjectionTypeCode.DEAD)) //
				, hasProperty("doSuppressPerHAYields", is(false)) //
				, hasProperty("rankCode", nullValue()) //
				, hasProperty("isDeadLayer", is(true)) //
		));
		assertThat(
				p1Layer3.getSp0sAsSupplied(), //
				contains(List.of(hasProperty("sp0Code", is("PL"))))
		);
		assertThat(
				p1Layer3.getSp64sAsSupplied(), //
				contains(List.of(hasProperty("speciesCode", is("PLI"))))
		);

		assertThat(
				p1Layer1.getSiteSpecies(), //
				contains(List.of(allOf(hasProperty("stand", hasProperty("sp0Code", is("PL"))) //
						, hasProperty("hasSiteInfo", is(true)) //
						, hasProperty("hasBeenCombined", is(false)) //
						, hasProperty("totalSpeciesPercent", is(60.0))
				), hasProperty("stand", hasProperty("sp0Code", is("S")))))
		);

		var polygon2 = polygonStream.getNextPolygon();
		Assert.assertNotNull(polygon2);
		Assert.assertEquals(13919430, polygon2.getFeatureId());
		Assert.assertEquals(1, polygon2.getLayers().size());

		Assert.assertFalse(polygonStream.hasNextPolygon());
	}
}
