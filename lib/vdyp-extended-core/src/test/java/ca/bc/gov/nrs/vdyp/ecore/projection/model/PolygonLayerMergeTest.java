package ca.bc.gov.nrs.vdyp.ecore.projection.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import ca.bc.gov.nrs.vdyp.ecore.utils.FileHelper;
import ca.bc.gov.nrs.vdyp.ecore.utils.ParameterNames;

public class PolygonLayerMergeTest {

	private AbstractPolygonStream polygonStream;

	@BeforeEach
	void beforeEach() throws PolygonValidationException, AbstractProjectionRequestException {
		var parameters = new Parameters().ageStart(10).ageEnd(20);

		{
			var polygonStreamFile = FileHelper
					.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_MERGE_POLY.csv");
			var layerStreamFile = FileHelper
					.getTestResourceFile(FileHelper.HCSV, FileHelper.COMMON, "VDYP7_INPUT_MERGE_LAYER.csv");

			var streams = new HashMap<String, InputStream>();
			streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
			streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, layerStreamFile);

			var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

			polygonStream = AbstractPolygonStream.build(state, streams);
		}
	}

	@Test
	void testSiteSpeciesMerge() throws PolygonValidationException {
		assertTrue(polygonStream.hasNextPolygon());

		var polygon1 = polygonStream.getNextPolygon();
		assertNotNull(polygon1);
		assertEquals(13919428, polygon1.getFeatureId());
		assertEquals(2, polygon1.getLayers().size());

		var p1Layer1 = polygon1.getLayers().get("1");
		assertNotNull(p1Layer1);
		assertThat(
				p1Layer1.getSiteSpecies(),
				contains(
						List.of(
								allOf(
										hasProperty("stand", hasProperty("sp0Code", is("PA"))),
										hasProperty("totalSpeciesPercent", is(100.0))
								)
						)
				)

		);

		var p1Layer2 = polygon1.getLayers().get("2");
		assertNotNull(p1Layer2);
		assertThat(
				p1Layer2.getSiteSpecies(),
				contains(
						List.of(
								allOf(
										hasProperty("stand", hasProperty("sp0Code", is("C"))),
										hasProperty("totalSpeciesPercent", is(100.0))
								)
						)
				)
		);
	}
}
