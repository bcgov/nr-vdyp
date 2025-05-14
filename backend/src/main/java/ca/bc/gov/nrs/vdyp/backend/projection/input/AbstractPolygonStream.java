package ca.bc.gov.nrs.vdyp.backend.projection.input;

import static ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind.*;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.PolygonValidationException;
import ca.bc.gov.nrs.vdyp.backend.api.v1.exceptions.ProjectionRequestValidationException;
import ca.bc.gov.nrs.vdyp.backend.endpoints.v1.ParameterNames;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.ProjectionContext;
import ca.bc.gov.nrs.vdyp.backend.projection.model.Polygon;

public abstract class AbstractPolygonStream {

	protected ProjectionContext context;

	public AbstractPolygonStream(ProjectionContext context) {
		this.context = context;
	}

	public abstract Polygon getNextPolygon() throws PolygonValidationException;

	protected static void verifyStreamTypes(Map<String, InputStream> streams, String... streamTypeNames)
			throws ProjectionRequestValidationException {

		var expectedStreamTypeNameSet = Set.of(streamTypeNames);
		var suppliedStreamTypeNameSet = streams.keySet();

		var e_minus_s = Sets.difference(expectedStreamTypeNameSet, suppliedStreamTypeNameSet);
		var s_minus_e = Sets.difference(suppliedStreamTypeNameSet, expectedStreamTypeNameSet);

		var messages = new ArrayList<ValidationMessage>();

		if (e_minus_s.size() > 0) {
			var message = new ValidationMessage(EXPECTED_STREAMS_NOT_SUPPLIED, e_minus_s.toArray());
			messages.add(message);
		}

		if (s_minus_e.size() > 0) {
			var message = new ValidationMessage(UNEXPECTED_STREAMS_SUPPLIED, s_minus_e.toArray());
			messages.add(message);
		}

		if (messages.size() > 0) {
			throw new ProjectionRequestValidationException(messages);
		}
	}

	public abstract boolean hasNextPolygon();

	public static AbstractPolygonStream build(ProjectionContext context, Map<String, InputStream> streams)
			throws ProjectionRequestValidationException {
		switch (context.getRequestKind()) {
		case DCSV:
			verifyStreamTypes(streams, ParameterNames.DCSV_INPUT_DATA);
			return new DcsvPolygonStream(context, streams.get(ParameterNames.DCSV_INPUT_DATA));
		case HCSV:
			verifyStreamTypes(streams, ParameterNames.HCSV_POLYGON_INPUT_DATA, ParameterNames.HCSV_LAYERS_INPUT_DATA);
			return new HcsvPolygonStream(
					context, streams.get(ParameterNames.HCSV_POLYGON_INPUT_DATA),
					streams.get(ParameterNames.HCSV_LAYERS_INPUT_DATA)
			);
		case ICSV:
			verifyStreamTypes(streams, ParameterNames.ICSV_INPUT_DATA);
			return new IcsvPolygonStream(context, streams.get(ParameterNames.ICSV_INPUT_DATA));
		case SCSV:
			verifyStreamTypes(
					streams, //
					ParameterNames.SCSV_POLYGON_INPUT_DATA, ParameterNames.SCSV_LAYERS_INPUT_DATA,
					ParameterNames.SCSV_HISTORY_INPUT_DATA, ParameterNames.SCSV_NON_VEGETATION_INPUT_DATA,
					ParameterNames.SCSV_OTHER_VEGETATION_INPUT_DATA, ParameterNames.SCSV_POLYGON_ID_INPUT_DATA,
					ParameterNames.SCSV_SPECIES_INPUT_DATA, ParameterNames.SCSV_VRI_ADJUST_INPUT_DATA
			);
			return new ScsvPolygonStream(
					context, streams.get(ParameterNames.SCSV_POLYGON_INPUT_DATA),
					streams.get(ParameterNames.SCSV_LAYERS_INPUT_DATA),
					streams.get(ParameterNames.SCSV_HISTORY_INPUT_DATA),
					streams.get(ParameterNames.SCSV_NON_VEGETATION_INPUT_DATA),
					streams.get(ParameterNames.SCSV_OTHER_VEGETATION_INPUT_DATA),
					streams.get(ParameterNames.SCSV_POLYGON_ID_INPUT_DATA),
					streams.get(ParameterNames.SCSV_SPECIES_INPUT_DATA),
					streams.get(ParameterNames.SCSV_VRI_ADJUST_INPUT_DATA)
			);
		default:
			throw new IllegalStateException(MessageFormat.format("Projection kind {0} is not recognized", context));
		}
	}
}
