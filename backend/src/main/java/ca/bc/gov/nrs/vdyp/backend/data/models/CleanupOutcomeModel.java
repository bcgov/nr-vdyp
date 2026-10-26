package ca.bc.gov.nrs.vdyp.backend.data.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

/** Mirrors the batch service's StorageCleanupService.Outcome enum for deserializing its cleanup report. */
@RegisterForReflection
public enum CleanupOutcomeModel {
	DELETABLE, DELETED, PROTECTED, FAILED
}
