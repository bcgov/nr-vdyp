package ca.bc.gov.nrs.vdyp.backend.data.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BatchThreadCapacityModel(@JsonProperty("threadCapacity") int threadCapacity) {
}
