/*
 * Variable Density Yield Projection
 * API for the Variable Density Yield Projection service
 *
 * The version of the OpenAPI document: 1.0.0
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package ca.bc.gov.nrs.vdyp.backend.v1.gen.responses;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import ca.bc.gov.nrs.vdyp.backend.v1.gen.model.ParameterDetailsMessage;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.core.UriInfo;

/**
 * HelpResponse
 */
@JsonPropertyOrder(
	{ //
			HelpResource.JSON_PROPERTY_LINKS, //
			HelpResource.JSON_PROPERTY_HELP_MESSAGES //
	}
)
@RegisterForReflection
@JsonSubTypes({ @Type(value = RootResource.class, name = ResourceTypes.HELP_RESOURCE) })
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type", defaultImpl = HelpResource.class
)
public class HelpResource extends VdypApiResource {

	public static final String JSON_PROPERTY_HELP_MESSAGES = "helpMessages";
	@JsonProperty(JSON_PROPERTY_HELP_MESSAGES)
	private List<ParameterDetailsMessage> helpMessages;

	public HelpResource(UriInfo uriInfo, List<ParameterDetailsMessage> helpMessages) {
		super(Set.of(Link.getSelfLink(uriInfo)));
		this.helpMessages = helpMessages;
	}

	public static HelpResource of(UriInfo uriInfo, List<ParameterDetailsMessage> helpMessages) {
		return new HelpResource(uriInfo, helpMessages);
	}

	/**
	 * Get helpMessages
	 *
	 * @return helpMessages
	 */
	@JsonProperty(value = JSON_PROPERTY_HELP_MESSAGES)

	public List<ParameterDetailsMessage> getHelpMessages() {
		return helpMessages;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		HelpResource helpResponse = (HelpResource) o;
		return Objects.equals(this.helpMessages, helpResponse.helpMessages);
	}

	@Override
	public int hashCode() {
		return Objects.hash(helpMessages);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class HelpResponse {\n");
		sb.append("    helpMessages: includes ").append(helpMessages.size()).append(" messages\n");
		sb.append("}");
		return sb.toString();
	}
}