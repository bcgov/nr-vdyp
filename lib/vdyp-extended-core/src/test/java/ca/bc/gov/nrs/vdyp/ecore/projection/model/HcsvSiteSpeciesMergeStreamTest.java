package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.AbstractProjectionRequestException;
import ca.bc.gov.nrs.vdyp.ecore.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.ecore.model.v1.ProjectionRequestKind;
import ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.ecore.projection.input.AbstractPolygonStream;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;

public class HcsvSiteSpeciesMergeStreamTest {

	private AbstractPolygonStream polygonStream;

	@BeforeEach
	void beforeEach() throws PolygonValidationException, AbstractProjectionRequestException {
		var parameters = new Parameters().ageStart(10).ageEnd(20);

		{
			var polygonStreamFile = FileHelper
					.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_MULTI_POLY.csv");
			var layerStreamFile = FileHelper
					.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_MULTI_POLY_LAYER.csv");

			var streams = new HashMap<String, InputStream>();
			streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
			streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, layerStreamFile);

			var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

			polygonStream = AbstractPolygonStream.build(state, streams);
		}
	}

	@Test
	void testMultiplePolygonRead() throws PolygonValidationException {
		assertTrue(polygonStream.hasNextPolygon());

		var polygon1 = polygonStream.getNextPolygon();
		assertNotNull(polygon1);
		assertEquals(13919428, polygon1.getFeatureId());
		assertEquals(3, polygon1.getLayers().size());

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
								"speciesByPercent",
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
								"speciesByPercent",
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
				p1Layer1.getSp0sAsSupplied().get(0).getSpeciesByPercent(),
				contains(List.of(hasProperty("speciesCode", is("PLI"))))
		);

		assertThat(
				p1Layer1.getSp0sAsSupplied().get(1).getSpeciesByPercent(),
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
		assertNotNull(polygon2);
		assertEquals(13919430, polygon2.getFeatureId());
		assertEquals(1, polygon2.getLayers().size());

		assertFalse(polygonStream.hasNextPolygon());
	}
}
