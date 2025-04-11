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
import ca.bc.gov.nrs.vdyp.backend.utils.FileHelper;

public class PolygonLayerMergeTest {

	private AbstractPolygonStream polygonStream;

	@BeforeEach
	void beforeEach() throws IOException, PolygonValidationException, ProjectionRequestException {
		var parameters = new Parameters().ageStart(10).ageEnd(20);

		{
			var polygonStreamFile = FileHelper
					.getStubResourceFile(FileHelper.HCSV, FileHelper.VDYP_240, "VDYP7_INPUT_MERGE_POLY.csv");
			var layerStreamFile = FileHelper
					.getStubResourceFile(FileHelper.HCSV, FileHelper.VDYP_240, "VDYP7_INPUT_MERGE_LAYER.csv");

			var streams = new HashMap<String, InputStream>();
			streams.put(ParameterNames.HCSV_POLYGON_INPUT_DATA, polygonStreamFile);
			streams.put(ParameterNames.HCSV_LAYERS_INPUT_DATA, layerStreamFile);

			var state = new ProjectionContext(ProjectionRequestKind.HCSV, "PolygonTest", parameters, false);

			polygonStream = AbstractPolygonStream.build(state, streams);
		}
	}

	@Test
	void testSiteSpeciesMerge() throws PolygonValidationException {
		Assert.assertTrue(polygonStream.hasNextPolygon());

		var polygon1 = polygonStream.getNextPolygon();
		Assert.assertNotNull(polygon1);
		Assert.assertEquals(13919428, polygon1.getFeatureId());
		Assert.assertEquals(2, polygon1.getLayers().size());

		var p1Layer1 = polygon1.getLayers().get("1");
		Assert.assertNotNull(p1Layer1);
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
		Assert.assertNotNull(p1Layer2);
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
