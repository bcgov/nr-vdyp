package ca.bc.gov.nrs.vdyp.backend.endpoints.v1.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class ParametersProvider implements MessageBodyReader<Parameters> {

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return Parameters.class.equals(type) && MediaType.APPLICATION_JSON.equals(mediaType.toString());
	}

	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public Parameters readFrom(
			Class<Parameters> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream
	) throws IOException, WebApplicationException {

		return mapper.readValue(entityStream, Parameters.class);
	}

}
