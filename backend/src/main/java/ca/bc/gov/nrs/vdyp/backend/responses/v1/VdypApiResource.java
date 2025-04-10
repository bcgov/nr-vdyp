package ca.bc.gov.nrs.vdyp.backend.responses.v1;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
abstract public class VdypApiResource {
	public static final String NAMESPACE = "http://vdyp.nrs.gov.bc.ca/v8/";

	public static final String JSON_PROPERTY_LINKS = "links";
	@JsonProperty(JSON_PROPERTY_LINKS)
	private final Set<Link> links;

	VdypApiResource(Set<Link> links) {
		this.links = links;
	}

	public Set<Link> getLinks() {
		return links;
	}
}
